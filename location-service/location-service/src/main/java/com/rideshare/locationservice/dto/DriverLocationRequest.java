package com.rideshare.locationservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriverLocationRequest {
      // this class taking/receiving data from drivers
      private String driverId;
      private double latitude;
      private double longitude;

}
