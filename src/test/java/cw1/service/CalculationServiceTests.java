package cw1.service;

import org.junit.jupiter.api.Test;
import cw1.DTO.LngLat;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationServiceTests {

    private final CalculationService svc = new CalculationService();

    @Test
    void distance_zeroWhenSamePoint() {
        var p = new LngLat(0.0, 0.0);
        assertThat(svc.calculateDistance(p, p)).isZero();
    }

    @Test
    void distance_symmetricAndPythagorean() {
        var a = new LngLat(1.0, 2.0);
        var b = new LngLat(4.0, 6.0);
        double d1 = svc.calculateDistance(a, b);
        double d2 = svc.calculateDistance(b, a);
        assertThat(d1).isEqualTo(d2);
        assertThat(d1).isEqualTo(5.0);
    }

    @Test
    void isCloseTo_strictLessThanThreshold() {
        var origin = new LngLat(0.0, 0.0);
        var near = new LngLat(CalculationService.CLOSE_THRESHOLD - 1e-12, 0.0);
        var edge = new LngLat(CalculationService.CLOSE_THRESHOLD, 0.0);

        assertThat(svc.isCloseTo(origin, near)).isTrue();
        assertThat(svc.isCloseTo(origin, edge)).isFalse();
    }

    @Test
    void nextPosition_movesOneStepAtAngle() {
        var start = new LngLat(-3.0, 55.0);

        var east = svc.nextPosition(start, 0);
        var north = svc.nextPosition(start, 90);
        var west = svc.nextPosition(start, 180);
        var south = svc.nextPosition(start, 270);
        var diag = svc.nextPosition(start, 45);

        assertThat(east.getLng()).isCloseTo(start.getLng() + CalculationService.STEP, within1e12());
        assertThat(east.getLat()).isCloseTo(start.getLat(), within1e12());

        assertThat(north.getLat()).isCloseTo(start.getLat() + CalculationService.STEP, within1e12());
        assertThat(north.getLng()).isCloseTo(start.getLng(), within1e12());

        assertThat(west.getLng()).isCloseTo(start.getLng() - CalculationService.STEP, within1e12());
        assertThat(south.getLat()).isCloseTo(start.getLat() - CalculationService.STEP, within1e12());

        double mag = Math.hypot(diag.getLng() - start.getLng(), diag.getLat() - start.getLat());
        assertThat(mag).isCloseTo(CalculationService.STEP, within1e12());
    }

    @Test
    void isInRegion_insideConvexPolygon_returnsTrue() {
        var p = new LngLat(0.5, 0.5);
        var poly = closed(List.of(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0)
        ));
        assertThat(svc.isInRegion(p, poly)).isTrue();
    }

    @Test
    void isInRegion_outsideConvexPolygon_returnsFalse() {
        var p = new LngLat(2.0, 0.5);
        var poly = closed(List.of(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0)
        ));
        assertThat(svc.isInRegion(p, poly)).isFalse();
    }

    @Test
    void isInRegion_onEdge_isInside() {
        var p = new LngLat(0.5, 0.0);
        var poly = closed(List.of(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0)
        ));
        assertThat(svc.isInRegion(p, poly)).isTrue();
    }

    @Test
    void isInRegion_onVertex_isInside() {
        var p = new LngLat(0.0, 0.0);
        var poly = closed(List.of(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0)
        ));
        assertThat(svc.isInRegion(p, poly)).isTrue();
    }

    @Test
    void isInRegion_concavePolygon_dentBehavesCorrectly() {
        var poly = closed(List.of(
                new LngLat(0.0, 0.0),
                new LngLat(2.0, 1.0),
                new LngLat(0.0, 2.0),
                new LngLat(0.5, 1.0)
        ));
        assertThat(svc.isInRegion(new LngLat(0.9, 1.0), poly)).isTrue();
        assertThat(svc.isInRegion(new LngLat(0.25, 1.0), poly)).isFalse();
    }

    @Test
    void isInRegion_closedDuplicateLastVertex_worksIdentically() {
        var base = List.of(
                new LngLat(0.0, 0.0),
                new LngLat(1.0, 0.0),
                new LngLat(1.0, 1.0),
                new LngLat(0.0, 1.0),
                new LngLat(0.0, 0.0)
        );
        assertThat(svc.isInRegion(new LngLat(0.5, 0.5), base)).isTrue();
    }

    private static org.assertj.core.data.Offset<Double> within1e12() {
        return org.assertj.core.data.Offset.offset(1e-12);
    }

    private static List<LngLat> closed(List<LngLat> openDistinct) {
        var list = new java.util.ArrayList<>(openDistinct);
        list.add(openDistinct.get(0));
        return list;
    }
}