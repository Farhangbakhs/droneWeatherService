package cw1.DTO.Internal;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor

/*
[
  {
    "name": "Appleton Tower",
    "id": 1,
    "location": {
      "lng": -3.18635807889864,
      "lat": 55.9446806670849,
      "alt": 50
    }
  },
  {
    "name": "Ocean Terminal",
    "id": 2,
    "location": {
      "lng": -3.17732611501824,
      "lat": 55.9811862793337,
      "alt": 50
    }
  }
]
 */

public class LocationDTO {
    private Double lng;
    private Double lat;
    private Integer alt;
}
