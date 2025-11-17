package cw1.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import cw1.DTO.LngLat;
import cw1.service.CalculationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceController.class)
class ServiceControllerMvcTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CalculationService calculationService;

    @Test
    void uid_returnsStudentId() throws Exception {
        mvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2324780"));
    }

    @Test
    void distanceTo_validBody_returns200AndValue() throws Exception {
        given(calculationService.calculateDistance(any(), any())).willReturn(1.2345);

        var body = """
        {
          "position1": {"lng": -3.192473, "lat": 55.946233},
          "position2": {"lng": -3.192473, "lat": 55.942617}
        }
        """;

        mvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().json("1.2345"));
    }

    @Test
    void distanceTo_missingField_returns400() throws Exception {
        var body = """
        {
          "position1": {"lng": -3.192473, "lat": 55.946233}
        }
        """;
        mvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void distanceTo_outOfRangeLat_returns400() throws Exception {
        var body = """
        {
          "position1": {"lng": -3.192473, "lat": 55.946233},
          "position2": {"lng": 10.0, "lat": 123.456}
        }
        """;
        mvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isCloseTo_valid_returns200AndBoolean() throws Exception {
        given(calculationService.isCloseTo(any(), any())).willReturn(true);

        var body = """
        {
          "position1": {"lng": -3.192473, "lat": 55.946233},
          "position2": {"lng": -3.192400, "lat": 55.946200}
        }
        """;

        mvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void isCloseTo_invalidJson_returns400() throws Exception {
        var body = """
        { "foo": 1 }
        """;
        mvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextPosition_valid_returns200AndLngLatJson() throws Exception {
        given(calculationService.nextPosition(any(LngLat.class), any(Double.class)))
                .willReturn(new LngLat(-2.99985, 55.0));

        var body = """
        {
          "start": {"lng": -3.0, "lat": 55.0},
          "angle": 0
        }
        """;

        mvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").value(-2.99985))
                .andExpect(jsonPath("$.lat").value(55.0));
    }

    @Test
    void nextPosition_missingAngle_returns400() throws Exception {
        var body = """
        {
          "start": {"lng": -3.0, "lat": 55.0}
        }
        """;
        mvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nextPosition_startOutOfRange_returns400() throws Exception {
        var body = """
        {
          "start": {"lng": -999.0, "lat": 55.0},
          "angle": 90
        }
        """;
        mvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isInRegion_validClosedPolygon_returns200True() throws Exception {
        given(calculationService.isInRegion(any(), any())).willReturn(true);

        var body = """
        {
          "position": { "lng": -3.189, "lat": 55.944 },
          "region": {
            "name": "central",
            "vertices": [
              {"lng": -3.192473, "lat": 55.946233},
              {"lng": -3.192473, "lat": 55.942617},
              {"lng": -3.184319, "lat": 55.942617},
              {"lng": -3.184319, "lat": 55.946233},
              {"lng": -3.192473, "lat": 55.946233}
            ]
          }
        }
        """;

        mvc.perform(post("/api/v1/isInRegion")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void isInRegion_missingPosition_returns400() throws Exception {
        var body = """
        {
          "region": {
            "name": "central",
            "vertices": [
              {"lng": 0, "lat": 0},
              {"lng": 1, "lat": 0},
              {"lng": 1, "lat": 1},
              {"lng": 0, "lat": 0}
            ]
          }
        }
        """;
        mvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isInRegion_outOfRangeVertex_returns400() throws Exception {
        var body = """
        {
          "position": {"lng": 0, "lat": 0},
          "region": {
            "name": "bad",
            "vertices": [
              {"lng": 0, "lat": 0},
              {"lng": 181, "lat": 0},
              {"lng": 0, "lat": 1},
              {"lng": 0, "lat": 0}
            ]
          }
        }
        """;
        mvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isInRegion_tooFewVertices_returns400() throws Exception {
        var body = """
        {
          "position": {"lng": 0, "lat": 0},
          "region": {
            "name": "tiny",
            "vertices": [
              {"lng": 0, "lat": 0},
              {"lng": 1, "lat": 0},
              {"lng": 0, "lat": 0}
            ]
          }
        }
        """;
        mvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isInRegion_openPolygon_returns400() throws Exception {
        var body = """
        {
          "position": {"lng": 0.5, "lat": 0.5},
          "region": {
            "name": "open",
            "vertices": [
              {"lng": 0, "lat": 0},
              {"lng": 1, "lat": 0},
              {"lng": 1, "lat": 1},
              {"lng": 0, "lat": 1}
            ]
          }
        }
        """;
        mvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void isInRegion_malformedJson_returns400() throws Exception {
        var body = """
        { "position": {"lng": 0, "lat": 0}, "region": "x" }
        """;
        mvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}