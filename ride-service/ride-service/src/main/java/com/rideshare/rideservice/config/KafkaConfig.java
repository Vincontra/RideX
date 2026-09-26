package com.rideshare.rideservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

public class KafkaConfig {

    //In my project, I use Kafka for asynchronous communication between the Ride Service and Matching Service.
    //When a user requests a ride, the Ride Service publishes a message to the ride.requested Kafka topic.
    // The Matching Service consumes that message and finds a suitable driver.
    // Once a driver is found, it publishes the result to the ride.matched topic, which the Ride Service consumes.
    // This allows both services to communicate without directly depending on each other's immediate response.


    // why kafka
    //Used Kafka so that the ride request and driver-matching process can happen asynchronously,
    //and the two services remain loosely coupled

    // Asynchronously matlab rideservice ko matchserive ke response ki
    // w8 krne ki jrurat nhi
    // They can both work independtly
    // Nhi to we can directly use here rest Api between Ride Service and Match Service


    //Topic where Ride Service published ride request
    // Matching Service subcribers to this topic

    @Bean
    public NewTopic rideRequestedTopic(){
        return TopicBuilder.name("ride.requested")
                .partitions(3)
                .replicas(1)
                .build();
    }

    //Topic where Matching Service publishes match results
    // Ride Service subscribers to this topic

    @Bean
    public NewTopic rideMatchedTopic(){
        return TopicBuilder.name("ride.matched")
                .partitions(3)
                .replicas(1)
                .build();
    }


}


//I created separate Kafka topics for ride requests and ride matches.
// Each topic has 3 partitions, which allows messages to be processed in parallel by multiple consumer instances.
// So as the number of ride requests increases, I can add more instances of the Matching Service and distribute the processing across partitions.
// This helps the system scale horizontally

// Why relicas?
//Replication is mainly for fault tolerance.
//My local setup uses one replica because I'm running a single Kafka broker.
// In a production cluster, multiple replicas can keep copies of the data across brokers so the system can continue working if one broker fails.
