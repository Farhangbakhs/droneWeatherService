package cw1.DTO.External;

import cw1.DTO.Internal.LimitDto;
import cw1.DTO.Internal.Vertex;
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
    "name": "George Square Area",
    "id": 1,
    "limits": {
      "lower": 0,
      "upper": -1
    },
    "vertices": [
      {
        "lng": -3.19057881832123,
        "lat": 55.9440241257753,
        "alt": null
      },
      {
        "lng": -3.18998873233795,
        "lat": 55.9428465054091,
        "alt": null
      },
 */

public class RestrictedArea {
    private String name;
    private Integer id;
    private LimitDto limits;
    private List<Vertex> vertices;
}
