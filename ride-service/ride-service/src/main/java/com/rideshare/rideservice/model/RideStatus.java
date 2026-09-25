package com.rideshare.rideservice.model;


// FLOW:
//REQUESTED ->
// MATCHING ->
// ACCEPTED ->
// DRIVER_ARRIVING->
// RIDE_STARTED ->
// COMPLETED ->
// CANCELLED (cancel toh kabhi bhi ho sakta)


public enum RideStatus {
    REQUESTED,
    MATCHING,
    ACCEPTED,
    DRIVER_ARRIVING,
    RIDE_STARTED,
    COMPLETED,
    CANCELLED
}