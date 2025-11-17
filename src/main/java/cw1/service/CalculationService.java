package cw1.service;

import org.springframework.stereotype.Service;
import cw1.DTO.LngLat;

import java.util.ArrayList;
import java.util.List;


/**
 * Spatial calculations for drone navigation.
 */
@Service
public class CalculationService {

    public static final double STEP = 0.00015;
    public static final double CLOSE_THRESHOLD = 0.00015;
    private static final double EPS = 1e-12;

    public double calculateDistance(LngLat a, LngLat b) {
        double dx = a.getLng() - b.getLng();
        double dy = a.getLat() - b.getLat();
        return Math.sqrt(dx*dx + dy*dy);
    }

    public boolean isCloseTo(LngLat a, LngLat b) {
        return calculateDistance(a, b) < CLOSE_THRESHOLD;
    }

    public LngLat nextPosition(LngLat start, double angleDeg) {
        double rad = Math.toRadians(angleDeg % 360.0);
        double dx = STEP * Math.cos(rad);
        double dy = STEP * Math.sin(rad);
        return new LngLat(start.getLng() + dx, start.getLat() + dy);
    }

    public boolean isInRegion(LngLat point, List<LngLat> vertices) {
        // drop the duplicate closing vertex
        List<LngLat> poly = new ArrayList<>();
        for (int i = 0; i < vertices.size() - 1; i++) {
            poly.add(vertices.get(i));
        }

        // check if point is on boundary
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            if (onSegment(poly.get(j), poly.get(i), point)) return true;
        }

        // ray casting for interior
        boolean inside = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            LngLat vi = poly.get(i);
            LngLat vj = poly.get(j);

            double xi = vi.getLng(), yi = vi.getLat();
            double xj = vj.getLng(), yj = vj.getLat();
            double x = point.getLng(), y = point.getLat();

            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    private boolean onSegment(LngLat a, LngLat b, LngLat p) {
        double cross = (b.getLng() - a.getLng()) * (p.getLat() - a.getLat()) -
                (b.getLat() - a.getLat()) * (p.getLng() - a.getLng());
        if (Math.abs(cross) > EPS) return false;

        double minx = Math.min(a.getLng(), b.getLng()) - EPS;
        double maxx = Math.max(a.getLng(), b.getLng()) + EPS;
        double miny = Math.min(a.getLat(), b.getLat()) - EPS;
        double maxy = Math.max(a.getLat(), b.getLat()) + EPS;
        double x = p.getLng(), y = p.getLat();
        return x >= minx && x <= maxx && y >= miny && y <= maxy;
    }
}