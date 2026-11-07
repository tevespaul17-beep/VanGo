import java.util.ArrayList;
import java.util.List;

/**
 * Seat map definitions for each van type.
 *
 * All coordinates are in a fixed REFERENCE space of 800 x 340 (the same space the
 * original hard-coded seats used). VanBackgroundPanel scales that space onto the
 * van image, so seats stay locked to the picture at any panel size.
 *
 * To re-align seats with a PNG, only edit the numbers in the tables below.
 * Each entry is the CENTRE of the seat: {seatNumber, centreX, centreY, jump(1/0)}.
 * Front of the van is on the RIGHT.
 */
public class VanLayout {

    public static final int REF_W = 800;
    public static final int REF_H = 340;

    // Image paths (single place to change them)
    public static final String IMG_COMMUTER = "images/toyota_Hiace_Commuter_seating_top_view_lineart_v1.png";
    public static final String IMG_DELUXE   = "images/toyota_Hiace_Commuter_Deluxe_seating_top_view_lineart_v1.png";
    public static final String IMG_NV350    = "images/nissan_nv350_seating_top_view_lineart_v1.png";
    public static final String IMG_CARGO    = "images/nissan_Urvan_seating_top_view_lineart_v1.png";

    public enum Kind { SEAT, JUMP, DRIVER }

    /** One clickable spot (a seat or the driver marker) in reference coordinates. */
    public static class Spot {
        public final int seatNumber; // 0 for the driver
        public final int cx, cy;
        public final Kind kind;

        Spot(int seatNumber, int cx, int cy, Kind kind) {
            this.seatNumber = seatNumber;
            this.cx = cx;
            this.cy = cy;
            this.kind = kind;
        }
    }

    // Original top-left coords were (x, y) for a 58x50 seat -> centre = (x+29, y+25)
    private static final int[][] COMMUTER = {
        {1, 669, 165, 0}, {2, 669, 250, 0},
        {3, 544,  80, 0}, {4, 544, 165, 0}, {5, 544, 250, 0},
        {6, 419,  80, 0}, {7, 419, 165, 0}, {8, 419, 250, 1},
        {9, 294,  80, 0}, {10, 294, 165, 0}, {11, 294, 250, 1},
        {12, 169, 80, 0}, {13, 169, 165, 0}, {14, 169, 250, 0}
    };
    private static final int COMMUTER_DRIVER_X = 669, COMMUTER_DRIVER_Y = 80;

    private static final int[][] DELUXE = {
        {1, 654, 165, 0}, {2, 654, 250, 0},
        {3, 514,  80, 0}, {4, 514, 165, 0}, {5, 514, 250, 0},
        {6, 359,  80, 0}, {7, 359, 165, 0}, {8, 359, 255, 0},
        {9, 199,  80, 0}, {10, 199, 165, 0}, {11, 199, 255, 0}
    };
    private static final int DELUXE_DRIVER_X = 654, DELUXE_DRIVER_Y = 80;

    private static final int[][] NV350 = {
        {1, 669, 165, 0}, {2, 669, 250, 0},
        {3, 544,  80, 0}, {4, 544, 165, 0}, {5, 544, 250, 0},
        {6, 419,  80, 0}, {7, 419, 165, 0}, {8, 419, 250, 0},
        {9, 294,  80, 0}, {10, 294, 165, 0}, {11, 294, 250, 0},
        {12, 169, 80, 0}, {13, 169, 165, 0}, {14, 169, 250, 0}
    };

    private static final int[][] CARGO = {
        {1, 669, 165, 0}, {2, 669, 250, 0},
        {3, 544,  80, 0}, {4, 544, 165, 0}, {5, 544, 250, 0}
    };

    /** Build the full spot list (driver + exactly `capacity` seats) for this van. */
    public static List<Spot> forVan(Van van) {
        int capacity = Math.max(1, van.getCapacity());
        String key = (van.getImagePath() + " " + van.getModel()).toLowerCase();

        int[][] table = null;
        int driverX = 669, driverY = 80;

        if (key.contains("deluxe")) {
            table = DELUXE; driverX = DELUXE_DRIVER_X; driverY = DELUXE_DRIVER_Y;
        } else if (key.contains("nv350")) {
            table = NV350; driverX = COMMUTER_DRIVER_X; driverY = COMMUTER_DRIVER_Y;
        } else if (key.contains("cargo") || key.contains("nissan_urvan_seating")) {
            table = CARGO; driverX = COMMUTER_DRIVER_X; driverY = COMMUTER_DRIVER_Y;
        } else if (key.contains("hiace")) {
            table = COMMUTER; driverX = COMMUTER_DRIVER_X; driverY = COMMUTER_DRIVER_Y;
        }

        // Known template whose seat count matches the van's capacity -> use it as drawn.
        if (table != null && table.length == capacity) {
            List<Spot> spots = new ArrayList<>();
            spots.add(new Spot(0, driverX, driverY, Kind.DRIVER));
            for (int[] s : table) {
                spots.add(new Spot(s[0], s[1], s[2], s[3] == 1 ? Kind.JUMP : Kind.SEAT));
            }
            return spots;
        }

        // Anything else (new vans, mismatched capacity): generate a grid for ALL seats.
        return generic(capacity);
    }

    /** Front pair beside the driver, then rows of 3 going toward the rear. */
    private static List<Spot> generic(int capacity) {
        List<Spot> spots = new ArrayList<>();
        final int frontX = 669;
        final int[] rowY = {80, 165, 250};

        spots.add(new Spot(0, frontX, rowY[0], Kind.DRIVER));

        int seat = 1;
        for (int i = 1; i <= 2 && seat <= capacity; i++) {
            spots.add(new Spot(seat++, frontX, rowY[i], Kind.SEAT));
        }

        int remaining = capacity - (seat - 1);
        int cols = (int) Math.ceil(remaining / 3.0);
        if (cols > 0) {
            // Even spacing, never tighter than a seat width so nothing overlaps.
            int spacing = Math.max(68, Math.min(125, (frontX - 40) / cols));
            for (int c = 1; c <= cols; c++) {
                int x = frontX - c * spacing;
                for (int r = 0; r < 3 && seat <= capacity; r++) {
                    spots.add(new Spot(seat++, x, rowY[r], Kind.SEAT));
                }
            }
        }
        return spots;
    }
}