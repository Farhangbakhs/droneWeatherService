package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response when weather conditions are unsafe for delivery
 * Contains risk information and alternative delivery windows
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherBlockedResponse {
    private String riskLevel;                                    // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> warnings;                               // List of specific weather issues
    private List<OptimalWindowsResponse.DeliveryWindow> windows; // Alternative safe time windows
}
