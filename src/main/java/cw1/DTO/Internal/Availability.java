// cw1/DTO/Internal/Availability.java
package cw1.DTO.Internal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class Availability {
    private String dayOfWeek;             // "MONDAY"
    private String from;                  // "00:00:00"
    private String until;                 // "23:59:59"
}
