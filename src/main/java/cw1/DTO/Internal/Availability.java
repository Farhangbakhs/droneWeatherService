// cw1/DTO/Internal/Availability.java
package cw1.DTO.Internal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor
public class Availability {
    private DayOfWeek dayOfWeek;             // "MONDAY"
    private LocalTime from;                  // "00:00:00"
    private LocalTime until;                 // "23:59:59"
}
