package cw1.DTO.Internal;



import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

/*
[
  {
    "servicePointId": 1,
    "drones": [
      {
        "id": "1",
        "availability": [
          {
            "dayOfWeek": "MONDAY",
            "from": "00:00:00",
            "until": "23:59:59"
          },
 */

public class DroneAvailable {
    private String id;
    private List<Availability> availability;
}
