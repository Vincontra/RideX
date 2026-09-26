package com.rideshare.matchingservice.event;

// Jo event produced kara tha to kafka by ride service to topic ride.requested
// that will be consumed here from Kafka
// Kafka is middleman buffer hai producer consumer jaisa
//RideService ne produce kiya --> Kafka --> Matching ne Consume

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEvent {

    private String rideId;
    private String riderId;

    //PICKUP
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;

    //DROP
    private double dropLatitude;
    private double dropLongitude;
    private String dropAddress;

}
