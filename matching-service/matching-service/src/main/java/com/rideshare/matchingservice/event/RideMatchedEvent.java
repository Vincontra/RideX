package com.rideshare.matchingservice.event;
//MatchService ne produce kiya --> Kafka --> RideServide ne
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event published to Kafka topic: ride.matched
// Consumed by Ride Service to update ride with assigned driver

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideMatchedEvent {
    private String rideId;
    private String riderId;
    private String driverId;  //driver milgaya

    private double driverLatitude;
    private double driverLongitude;
    private double distanceToPickup;
}
