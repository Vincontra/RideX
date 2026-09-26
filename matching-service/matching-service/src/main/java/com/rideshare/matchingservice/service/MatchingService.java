package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.client.LocationServiceClient;
import com.rideshare.matchingservice.dto.NearByDriverResponse;
import com.rideshare.matchingservice.event.RideMatchedEvent;
import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {
    private final LocationServiceClient locationServiceClient;
    private final KafkaTemplate<String,RideMatchedEvent> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";
    private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;

    /**
     * Main matching alogorith
     * Called when RideRequestedEvent is consumed from Kafka i mean jab user call krega or request for Driver
     * @param event
     *
     * STEPS:
     * 1. Ask Location Service for nearby drivers
     * 2. Score each driver and pick the best one
     */
    public void matchDriverForRide(RideRequestedEvent event){
        //step1
        List<NearByDriverResponse> nearByDrivers = locationServiceClient.getNearByDrivers(
                event.getPickupLatitude(),
                event.getPickupLongitude(),
                DEFAULT_SEARCH_RADIUS_KM
        );

        if(nearByDrivers.isEmpty()){
            log.warn("No drivers found near ride");
            return;
        }

        // step 2 Score each driver and pick the best one
        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearByDrivers);

        if(bestDriver.isEmpty()){
            log.warn("could not find suitable driver for ride");
            return;
        }

        NearByDriverResponse assignedDriver = bestDriver.get();

        //step 3 Publish RideMatchedEvent to Kafka
        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm()
        );
        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
        log.info("RideMatchedEvent published");
    }
    /**
     * Driver Scoring algorithm
     *
     * Distance: 70%
     * Rating: 30%
     *
     * Score = (1 / distance) * distanceWeight + rating * ratingWeight //inversely proportional hai
     *
     * @param drivers
     * @return
     *
     */
    private Optional<NearByDriverResponse> findBestDriver(List<NearByDriverResponse> drivers){
        double distanceWeight = 0.7;
        double ratingWeight = 0.3;
        return drivers.stream().max(Comparator.comparingDouble(driver -> {
                    //Distance score: closer = higher score
                    // Add 0.1 to avoid division by zero
                    double distanceScore = 1.0/(driver.getDistanceInKm()+0.1);
                    // Simulated rating between 4.0 and 5.0
                    // In production we can fetch from Driver Service as uski rating bahot sare factors pr based ho sakti hai
                    double simulatedRating = 4.0 + Math.random();
                    //Final weighted score
                    return (distanceScore * distanceWeight)
                            + (simulatedRating * ratingWeight);
        }));
    }
}
