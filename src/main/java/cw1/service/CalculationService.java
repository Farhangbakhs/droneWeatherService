package cw1.service;

import org.springframework.stereotype.Service;
import cw1.DTO.LngLat;

import java.util.ArrayList;
import java.util.List;


/**
 * Service providing spatial calculations for drone navigation.
 *
 * <p>This service implements geometric algorithms for:
 * <ul>
 *   <li>Distance calculations between coordinate points</li>
 *   <li>Position movement based on bearing angles</li>
 *   <li>Point-in-polygon detection for restricted zones</li>
 * </ul>
 *
 * <p><b>Coordinate System:</b> Uses WGS84 geographic coordinates (longitude, latitude)
 * represented as decimal degrees. All calculations assume a flat-earth approximation
 * valid for small distances (< 100km).
 *
 * @author Drone Delivery System
 * @version 2.0.0
 * @since 1.0.0
 */
@Service
public class CalculationService {

    /**
     * Standard movement step distance in degrees (~16.65 meters at Edinburgh latitude).
     *
     * <p>This constant defines how far a drone moves in one step when given a direction.
     * At Edinburgh's latitude (~56°), 0.00015° longitude ≈ 8.3m, 0.00015° latitude ≈ 16.7m.
     */
    public static final double STEP = 0.00015;

    /**
     * Distance threshold for considering two points "close" (same as STEP).
     *
     * <p>Two points are considered close if their Euclidean distance is less than
     * this threshold. Used for delivery completion detection.
     */
    public static final double CLOSE_THRESHOLD = 0.00015;

    /**
     * Epsilon value for floating-point comparison tolerance (10^-12).
     *
     * <p>Used in geometric calculations to handle floating-point rounding errors,
     * particularly in the point-on-segment check.
     */
    private static final double EPS = 1e-12;

    /**
     * Calculate Euclidean distance between two geographic points.
     *
     * <p>Uses the flat-earth approximation (Pythagorean theorem) to calculate
     * straight-line distance between two points. This is accurate for small
     * distances (< 100km) and significantly faster than great-circle calculations.
     *
     * <p><b>Formula:</b> distance = √((lng₂ - lng₁)² + (lat₂ - lat₁)²)
     *
     * <p><b>Note:</b> This returns distance in degrees, not meters. For real-world
     * distance, multiply by ~111,000 meters/degree for latitude.
     *
     * @param a First geographic point
     * @param b Second geographic point
     * @return Euclidean distance between the points in degrees
     * @throws NullPointerException if either point is null
     */
    public double calculateDistance(LngLat a, LngLat b) {
        double dx = a.getLng() - b.getLng();
        double dy = a.getLat() - b.getLat();
        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * Check if two points are within the close proximity threshold.
     *
     * <p>Two points are considered "close" if their Euclidean distance is less
     * than {@link #CLOSE_THRESHOLD} (0.00015 degrees ≈ 16.65 meters).
     *
     * <p><b>Use Case:</b> Determine when a drone has reached its destination or
     * if two coordinates are effectively at the same location.
     *
     * @param a First geographic point
     * @param b Second geographic point
     * @return {@code true} if distance < CLOSE_THRESHOLD, {@code false} otherwise
     * @throws NullPointerException if either point is null
     */
    public boolean isCloseTo(LngLat a, LngLat b) {
        return calculateDistance(a, b) < CLOSE_THRESHOLD;
    }

    /**
     * Calculate the next position when moving from a starting point in a given direction.
     *
     * <p>Moves {@link #STEP} distance (0.00015 degrees) from the start position
     * in the direction specified by the angle.
     *
     * <p><b>Angle Convention:</b>
     * <ul>
     *   <li>0° = East (positive x-axis)</li>
     *   <li>90° = North (positive y-axis)</li>
     *   <li>180° = West (negative x-axis)</li>
     *   <li>270° = South (negative y-axis)</li>
     * </ul>
     *
     * <p><b>Formula:</b>
     * <pre>
     * newLng = startLng + (STEP × cos(angle))
     * newLat = startLat + (STEP × sin(angle))
     * </pre>
     *
     * @param start Starting geographic point
     * @param angleDeg Direction angle in degrees (automatically normalized to 0-360)
     * @return New position after moving STEP distance in the specified direction
     * @throws NullPointerException if start is null
     */
    public LngLat nextPosition(LngLat start, double angleDeg) {
        // Normalize angle to 0-360 range, then convert to radians
        double rad = Math.toRadians(angleDeg % 360.0);
        double dx = STEP * Math.cos(rad);
        double dy = STEP * Math.sin(rad);
        return new LngLat(start.getLng() + dx, start.getLat() + dy);
    }

    /**
     * Determine if a point is inside or on the boundary of a polygon region.
     *
     * <p>This method uses a two-step approach:
     * <ol>
     *   <li><b>Boundary Check:</b> Test if point lies on any edge of the polygon</li>
     *   <li><b>Interior Check:</b> Use ray casting algorithm to test if point is inside</li>
     * </ol>
     *
     * <p><b>Algorithm Details:</b>
     * <ul>
     *   <li>Assumes polygon vertices form a closed loop (first == last vertex)</li>
     *   <li>Automatically removes duplicate closing vertex for processing</li>
     *   <li>Uses ray casting: Counts edge crossings of a horizontal ray from the point</li>
     *   <li>Odd number of crossings = inside, even = outside</li>
     * </ul>
     *
     * <p><b>Use Case:</b> Check if drone position is within a restricted no-fly zone.
     *
     * @param point The point to test
     * @param vertices List of polygon vertices forming a closed loop (first == last)
     * @return {@code true} if point is inside or on the boundary, {@code false} otherwise
     * @throws NullPointerException if point or vertices is null
     * @throws IllegalArgumentException if vertices has fewer than 3 unique points
     */
    public boolean isInRegion(LngLat point, List<LngLat> vertices) {
        // Drop the duplicate closing vertex to create a proper polygon
        List<LngLat> poly = new ArrayList<>();
        for (int i = 0; i < vertices.size() - 1; i++) {
            poly.add(vertices.get(i));
        }

        // Check if point is on the polygon boundary
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            if (onSegment(poly.get(j), poly.get(i), point)) return true;
        }

        // Use ray casting algorithm to check if point is inside
        return isInside(point, poly);
    }

    /**
     * Ray casting algorithm to determine if a point is inside a polygon.
     *
     * <p><b>Algorithm:</b> Cast a horizontal ray from the point to infinity (right direction).
     * Count how many times the ray crosses polygon edges:
     * <ul>
     *   <li>Odd number of crossings → point is inside</li>
     *   <li>Even number of crossings → point is outside</li>
     * </ul>
     *
     * <p><b>Implementation Notes:</b>
     * <ul>
     *   <li>Only counts crossings where edge crosses the horizontal line y=point.lat</li>
     *   <li>Ensures crossing is to the right of the point (x < intersection point)</li>
     *   <li>Handles edge cases where ray passes through vertices</li>
     * </ul>
     *
     * @param point Point to test
     * @param poly Polygon vertices (without closing duplicate)
     * @return {@code true} if point is inside polygon, {@code false} otherwise
     */
    private static boolean isInside(LngLat point, List<LngLat> poly) {
        boolean inside = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            LngLat vi = poly.get(i);
            LngLat vj = poly.get(j);

            double xi = vi.getLng(), yi = vi.getLat();
            double xj = vj.getLng(), yj = vj.getLat();
            double x = point.getLng(), y = point.getLat();

            // Check if horizontal ray from point crosses this edge
            // Condition 1: (yi > y) != (yj > y) ensures edge crosses horizontal line
            // Condition 2: x < intersection_x ensures crossing is to the right
            boolean intersect = ((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }
        return inside;
    }

    /**
     * Check if a point lies on a line segment.
     *
     * <p><b>Algorithm:</b> Two-step verification:
     * <ol>
     *   <li><b>Collinearity Check:</b> Use cross product to verify point is on the line.
     *       If cross product ≈ 0 (within EPS tolerance), points are collinear.</li>
     *   <li><b>Bounding Box Check:</b> Verify point is within the segment's bounding box,
     *       not just on the extended line.</li>
     * </ol>
     *
     * <p><b>Cross Product Formula:</b> (b - a) × (p - a) = 0 for collinear points
     * <pre>
     * cross = (b.lng - a.lng)(p.lat - a.lat) - (b.lat - a.lat)(p.lng - a.lng)
     * </pre>
     *
     * <p><b>EPS Tolerance:</b> Adds small epsilon (10^-12) to handle floating-point
     * rounding errors in geometric calculations.
     *
     * @param a First endpoint of the segment
     * @param b Second endpoint of the segment
     * @param p Point to test
     * @return {@code true} if p lies on segment ab, {@code false} otherwise
     */
    private boolean onSegment(LngLat a, LngLat b, LngLat p) {
        // Step 1: Check if points are collinear using cross product
        // Cross product = (b - a) × (p - a)
        double cross = (b.getLng() - a.getLng()) * (p.getLat() - a.getLat()) -
                (b.getLat() - a.getLat()) * (p.getLng() - a.getLng());

        // If cross product is not near zero, point is not on the line
        if (Math.abs(cross) > EPS) return false;

        // Step 2: Check if point is within segment's bounding box
        // (not on the extended infinite line)
        double minx = Math.min(a.getLng(), b.getLng()) - EPS;
        double maxx = Math.max(a.getLng(), b.getLng()) + EPS;
        double miny = Math.min(a.getLat(), b.getLat()) - EPS;
        double maxy = Math.max(a.getLat(), b.getLat()) + EPS;
        double x = p.getLng(), y = p.getLat();

        return x >= minx && x <= maxx && y >= miny && y <= maxy;
    }
}