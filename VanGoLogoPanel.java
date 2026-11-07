import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class VanGoLogoPanel extends JPanel {
    private final boolean showText;

    public VanGoLogoPanel() { this(true); }

    public VanGoLogoPanel(boolean showText) {
        this.showText = showText;
        setOpaque(false);
        setPreferredSize(showText ? new Dimension(240, 72) : new Dimension(56, 56));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (showText) {
            double scale = Math.min(w / 260.0, h / 80.0);
            g2.scale(scale, scale);

            // Speed streaks
            g2.setColor(VanGoTheme.ORANGE);
            g2.fill(new RoundRectangle2D.Double(78, 14, 26, 4, 2, 2));
            g2.fill(new RoundRectangle2D.Double(70, 21, 34, 4, 2, 2));
            g2.fill(new RoundRectangle2D.Double(74, 28, 30, 4, 2, 2));

            // Van body
            Path2D vanBody = new Path2D.Double();
            vanBody.moveTo(110, 8);
            vanBody.lineTo(142, 8);
            vanBody.lineTo(158, 22);
            vanBody.lineTo(164, 28);
            vanBody.lineTo(164, 38);
            vanBody.lineTo(110, 38);
            vanBody.closePath();
            g2.setColor(VanGoTheme.NAVY);
            g2.fill(vanBody);

            // Glass
            Path2D glass = new Path2D.Double();
            glass.moveTo(116, 12);
            glass.lineTo(140, 12);
            glass.lineTo(152, 24);
            glass.lineTo(116, 24);
            glass.closePath();
            g2.setColor(VanGoTheme.ICE);
            g2.fill(glass);
            g2.setColor(new Color(255, 255, 255, 140));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(glass);

            // Wheels
            drawWheel(g2, 120, 38, 12, 4);
            drawWheel(g2, 154, 38, 12, 4);

            // Wordmark — "Van" navy + "GO" orange, with proper kerning
            Font wordFont = new Font(VanGoTheme.FONT_FAMILY, Font.BOLD | Font.ITALIC, 40);
            g2.setFont(wordFont);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(VanGoTheme.NAVY);
            g2.drawString("Van", 58, 72);
            int vanW = fm.stringWidth("Van");
            g2.setColor(VanGoTheme.ORANGE);
            g2.drawString("GO", 58 + vanW + 1, 72);

            // Underline accent
            g2.setColor(VanGoTheme.ORANGE);
            g2.fill(new RoundRectangle2D.Double(48, 78, 168, 4, 2, 2));
        } else {
            double scale = Math.min(w / 90.0, h / 60.0);
            g2.scale(scale, scale);

            g2.setColor(VanGoTheme.ORANGE);
            g2.fill(new RoundRectangle2D.Double(6, 14, 16, 5, 2, 2));
            g2.fill(new RoundRectangle2D.Double(1, 23, 22, 5, 2, 2));
            g2.fill(new RoundRectangle2D.Double(4, 32, 18, 5, 2, 2));

            Path2D vanBody = new Path2D.Double();
            vanBody.moveTo(32, 8);
            vanBody.lineTo(68, 8);
            vanBody.lineTo(82, 22);
            vanBody.lineTo(88, 28);
            vanBody.lineTo(88, 38);
            vanBody.lineTo(32, 38);
            vanBody.closePath();
            g2.setColor(VanGoTheme.NAVY);
            g2.fill(vanBody);

            Path2D glass = new Path2D.Double();
            glass.moveTo(40, 12);
            glass.lineTo(64, 12);
            glass.lineTo(76, 23);
            glass.lineTo(40, 23);
            glass.closePath();
            g2.setColor(VanGoTheme.ICE);
            g2.fill(glass);

            drawWheel(g2, 46, 38, 13, 5);
            drawWheel(g2, 76, 38, 13, 5);
        }
        g2.dispose();
    }

    private void drawWheel(Graphics2D g2, int cx, int cy, int size, int hubSize) {
        g2.setColor(VanGoTheme.NAVY);
        g2.fillOval(cx - size / 2, cy - size / 2, size, size);
        g2.setColor(VanGoTheme.ORANGE);
        g2.fillOval(cx - hubSize / 2, cy - hubSize / 2, hubSize, hubSize);
    }
}