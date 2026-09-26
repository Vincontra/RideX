package com.rideshare.rideservice.service;

import com.rideshare.rideservice.dto.RideRequest;
import com.rideshare.rideservice.dto.RideResponse;
import com.rideshare.rideservice.event.RideRequestedEvent;
import com.rideshare.rideservice.model.Ride;
import com.rideshare.rideservice.model.RideStatus;
import com.rideshare.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {
    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestedEvent> kafkaTemplate;
    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";

    public RideResponse requestRide(RideRequest request) {
        //Step 1: save ride to database
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupAddress(request.getPickupAddress());
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimatedFare(calculateEstimateFare(request));
        Ride savedRide = rideRepository.save(ride);

        // Step 2:Ye event ko publish krenge to Kafka
        // Matching service will consume this and find nearest driver
        RideRequestedEvent event = new RideRequestedEvent(
                savedRide.getId(),
                savedRide.getRiderId(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupLongitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress()
        );
        kafkaTemplate.send(RIDE_REQUESTED_TOPIC,savedRide.getId(),event);
        System.out.println("RideReqEvent published to kafka for ride: "+savedRide.getId());
       // log.info("RideRequestedEvent published to Kafka for ride: {}", savedRide.getId());

        //Update status to Matching
        savedRide.setStatus(RideStatus.MATCHING);
        rideRepository.save(savedRide);

        return mapToResponse(savedRide); // return is diff to map hi kr lete

    }

    public void updateRideWithDriver(String rideId, String driverId){
        // this is not in controller
        // lekin after status becomes matching when the rider get
        // the driver status ko badlna padega to Accepted
        // to isliye this method

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);
    }

    public RideResponse startRide(String rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(()->new RuntimeException("Ride not found"));

        if (ride.getStatus()!=RideStatus.ACCEPTED){
            // agar accpet hi nhi to start to ho nhi sakti
            throw new RuntimeException("Ride can not be started. Current Status: "+ride.getStatus());
        }

        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);
        return mapToResponse(ride);

    }

    public RideResponse completeRide(String rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(()->new RuntimeException("Ride not found"));
        if (ride.getStatus()!=RideStatus.RIDE_STARTED){
            // agar chalu hi nhi to kri to complete kaise
            throw new RuntimeException("Ride can not be completed. Current Status: "+ride.getStatus());
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setActualFare(ride.getEstimatedFare());
        rideRepository.save(ride);
        return mapToResponse(ride);
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(()->new RuntimeException("Ride not found"));
//        if (ride.getStatus()!=RideStatus.ACCEPTED){
//            // agar chalu hi nhi to kri to cancel  karenge
//            throw new RuntimeException("Ride can not be cancelled. Current Status: "+ride.getStatus());
//        }
        // cancel to kbhi bhi kr sakte evven though it is not accepted

        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        return mapToResponse(ride);
    }
    public RideResponse getRideById(String rideId) {
        Ride ride=rideRepository.findById(rideId).orElseThrow(()->new RuntimeException("Ride not found"));
        return mapToResponse(ride);
    }

    public List<RideResponse> getRidesByRider(String riderId) {
        List<RideResponse> responses = new ArrayList<>();
        List<Ride>rides=rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId);
        for (Ride ride:rides) {
            responses.add(mapToResponse(ride));
        }
        return responses;
    }
    private RideResponse mapToResponse(Ride savedRide) {
        RideResponse response = new RideResponse();
        response.setId(savedRide.getId());
        response.setRiderId(savedRide.getRiderId());
        response.setDriverId(savedRide.getDriverId());
        response.setPickupLatitude(savedRide.getPickupLatitude());
        response.setPickupLongitude(savedRide.getPickupLongitude());
        response.setPickupAddress(savedRide.getPickupAddress());
        response.setDropLatitude(savedRide.getDropLatitude());
        response.setDropLongitude(savedRide.getDropLongitude());
        response.setDropAddress(savedRide.getDropAddress());
        response.setStatus(savedRide.getStatus());
        response.setEstimatedFare(savedRide.getEstimatedFare());
        response.setActualFare(savedRide.getActualFare());
        response.setCreatedAt(savedRide.getCreatedAt());
        response.setStartedAt(savedRide.getStartedAt());
        response.setCompletedAt(savedRide.getCompletedAt());
        return response;
    }
    private double calculateEstimateFare(RideRequest request) {
        // Simplified Haversine distance calculation
        double lat1 = Math.toRadians(request.getPickupLatitude());
        double lat2 = Math.toRadians(request.getDropLatitude());

        double lon1 = Math.toRadians(request.getPickupLongitude());
        double lon2 = Math.toRadians(request.getDropLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a =Math.pow(Math.sin(dLat / 2), 2)
                +Math.cos(lat1) * Math.cos(lat2)
                *Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));
        double distanceKm = 6371 * c;

        //Base fare: 50Rs + 12Rs. perKm
        double fare = 50 + (distanceKm * 12);
        return Math.round(fare * 100.0) / 100.0;

    }


}
