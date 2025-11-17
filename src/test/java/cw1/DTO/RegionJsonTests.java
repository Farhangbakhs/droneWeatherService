package cw1.DTO;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class RegionJsonTests {

    @Autowired JacksonTester<RegionRequest> json;

    @Test
    void deserializeIgnoresUnknownFields_andBinds() throws Exception {
        var content = """
        {
          "position": {"lng": -3.19, "lat": 55.94, "extra": 123},
          "region": {
            "name": "central",
            "vertices": [
              {"lng": -3.192473, "lat": 55.946233, "noop": true},
              {"lng": -3.192473, "lat": 55.942617},
              {"lng": -3.184319, "lat": 55.942617},
              {"lng": -3.184319, "lat": 55.946233},
              {"lng": -3.192473, "lat": 55.946233}
            ],
            "unused": "x"
          },
          "ignoredRoot": true
        }
        """;
        var parsed = json.parse(content).getObject();
        assertThat(parsed.getPosition().getLng()).isEqualTo(-3.19);
        assertThat(parsed.getRegion().getName()).isEqualTo("central");
        assertThat(parsed.getRegion().getVertices()).hasSize(5);
    }
}