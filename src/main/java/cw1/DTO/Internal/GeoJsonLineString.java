package cw1.DTO.Internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Minimal GeoJSON LineString:
 * {
 *   "type": "LineString",
 *   "coordinates": [ [lng, lat], [lng, lat], ... ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoJsonLineString {
    private String type;
    private List<List<Double>> coordinates;
}
