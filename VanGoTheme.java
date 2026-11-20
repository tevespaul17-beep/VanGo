import java.awt.Color;
import java.awt.Font;

public final class VanGoTheme {
    // Brand
    public static final Color NAVY        = new Color(0x1E293B);
    public static final Color NAVY_DARK   = new Color(0x0F172A);
    public static final Color ORANGE      = new Color(0xFF6B00);
    public static final Color ORANGE_DARK = new Color(0xD95A00);

    // Surfaces
    public static final Color BG          = new Color(0xF1F5F9);
    public static final Color SURFACE     = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(0xF8FAFC);
    public static final Color BORDER      = new Color(0xCBD5E1);

    // Text
    public static final Color TEXT        = new Color(0x0F172A);
    public static final Color TEXT_SECOND = new Color(0x475569);
    public static final Color TEXT_TERT   = new Color(0x94A3B8);

    // Semantic
    public static final Color GO          = new Color(0x16A34A);
    public static final Color GO_BG       = new Color(0xDCFCE7);
    public static final Color DANGER      = new Color(0xDC2626);
    public static final Color DANGER_BG   = new Color(0xFEE2E2);
    public static final Color WARN        = new Color(0xF59E0B);
    public static final Color WARN_BG     = new Color(0xFEF3C7);

    // Seats
    public static final Color SEAT_AVAILABLE = new Color(0xD1FAE5);
    public static final Color SEAT_JUMP      = new Color(0xFEF3C7);

    private static final String FAMILY = "Segoe UI";

    public static Font font(int size, int style) { return new Font(FAMILY, style, size); }
    public static Font body()    { return font(14, Font.PLAIN); }
    public static Font label()   { return font(12, Font.BOLD); }
    public static Font h2()      { return font(22, Font.BOLD); }
    public static Font h3()      { return font(17, Font.BOLD); }

    private VanGoTheme() {}
}