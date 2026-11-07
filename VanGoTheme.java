import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.Border;

public final class VanGoTheme {
    private VanGoTheme() {}

    // --- Brand ---
    public static final Color NAVY         = new Color(0x0F2841);
    public static final Color NAVY_DARK    = new Color(0x081A2D);
    public static final Color NAVY_LIGHT   = new Color(0x1E3A5F);
    public static final Color ORANGE       = new Color(0xE25208);
    public static final Color ORANGE_DARK  = new Color(0xC44406);
    public static final Color ICE          = new Color(0xD8E8F5);

    // --- Surfaces ---
    public static final Color BG           = new Color(0xF7F9FC);
    public static final Color SURFACE      = Color.WHITE;
    public static final Color SURFACE_ALT  = new Color(0xF1F5F9);
    public static final Color BORDER       = new Color(0xE2E8F0);
    public static final Color BORDER_STRONG= new Color(0xCBD5E1);

    // --- Text ---
    public static final Color TEXT         = new Color(0x0F172A);
    public static final Color TEXT_SECOND  = new Color(0x475569);
    public static final Color TEXT_TERT    = new Color(0x94A3B8);

    // --- Semantic ---
    public static final Color GO           = new Color(0x059669);
    public static final Color GO_BG        = new Color(0xECFDF5);
    public static final Color WARN         = new Color(0xD97706);
    public static final Color WARN_BG      = new Color(0xFFFBEB);
    public static final Color DANGER       = new Color(0xDC2626);
    public static final Color DANGER_BG    = new Color(0xFEF2F2);
    public static final Color SEAT_AVAILABLE = new Color(0x9BEF7A);
    public static final Color SEAT_JUMP      = new Color(0xF28C00);

    // --- Typography ---
    public static final String FONT_FAMILY = "Segoe UI";

    public static Font font(int size, int style) {
        return new Font(FONT_FAMILY, style, size);
    }
    public static Font h1() { return font(28, Font.BOLD); }
    public static Font h2() { return font(22, Font.BOLD); }
    public static Font h3() { return font(18, Font.BOLD); }
    public static Font body() { return font(14, Font.PLAIN); }
    public static Font label() { return font(12, Font.BOLD); }
    public static Font caption() { return font(11, Font.PLAIN); }
    public static Font button() { return font(14, Font.BOLD); }

    // --- Elevation ---
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );
    }

    public static void applyGlobalDefaults() {
        UIManager.put("Label.font", body());
        UIManager.put("Button.font", button());
        UIManager.put("TextField.font", body());
        UIManager.put("Table.font", body());
        UIManager.put("TableHeader.font", label());
        UIManager.put("TabbedPane.font", button());
        UIManager.put("OptionPane.messageFont", body());
        UIManager.put("OptionPane.buttonFont", button());
    }

    /** Soft drop-shadow used across cards & buttons. */
    public static void paintSoftShadow(Graphics2D g2, int x, int y, int w, int h, int arc) {
        g2.setColor(new Color(15, 40, 65, 8));
        g2.fill(new RoundRectangle2D.Float(x, y + 2, w, h, arc, arc));
        g2.setColor(new Color(15, 40, 65, 5));
        g2.fill(new RoundRectangle2D.Float(x - 1, y + 1, w + 2, h + 2, arc + 2, arc + 2));
    }
}