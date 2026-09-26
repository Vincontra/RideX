package com.rideshare.matchingservice.service;


import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {
    private final MatchingService matchingService;

      //Listens to ride.requested kafka topic.
     // Triggered every time when Ride Service published a new ride request
    //  Ride Service -> Kafka (ride.requested) -> This Consumer -> MatchingService


    @KafkaListener(topics = "ride.requested",groupId = "matching-service-group")
    public void consumeRideRequestedEvent(RideRequestedEvent event){
        try {
            matchingService.matchDriverForRide(event);
        }
        catch (Exception e){
           log.error("Errir processing ride request:{}-{}",event.getRideId(),e.getMessage());
        }
    }
}
