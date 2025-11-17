package cw1.DTO;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class LngLatJsonTests {

    @Autowired JacksonTester<LngLat> json;

    @Test
    void serialize() throws Exception {
        var ll = new LngLat(-3.192473, 55.946233);
        var content = json.write(ll);
        assertThat(content).extractingJsonPathNumberValue("@.lng").isEqualTo(-3.192473);
        assertThat(content).extractingJsonPathNumberValue("@.lat").isEqualTo(55.946233);
    }

    @Test
    void deserializeIgnoresUnknownFields() throws Exception {
        var content = """
          {"lng": -3.192473, "lat": 55.946233, "extra": 123}
        """;
        var parsed = json.parse(content).getObject();
        assertThat(parsed.getLng()).isEqualTo(-3.192473);
        assertThat(parsed.getLat()).isEqualTo(55.946233);
    }
}