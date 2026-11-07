import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class ClarityButton extends JButton {
    private Color defaultBg;
    private Color hoverBg;
    private Color pressedBg;
    private final int arc = 10;
    private boolean hover = false;
    private boolean press = false;

    public ClarityButton(String text, Color bg, Color fg, Color border, int fontSize) {
        super(text);
        this.defaultBg = bg;
        this.hoverBg = deriveHover(bg);
        this.pressedBg = hoverBg.darker();
        setFont(VanGoTheme.font(fontSize, Font.BOLD));
        setBackground(bg);
        setForeground(fg);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; press = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { press = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e){ press = false; repaint(); }
        });
    }

    private static Color deriveHover(Color c) {
        if (c.equals(VanGoTheme.ORANGE)) return VanGoTheme.ORANGE_DARK;
        if (c.equals(VanGoTheme.NAVY)) return VanGoTheme.NAVY_DARK;
        if (c.equals(Color.WHITE)) return VanGoTheme.SURFACE_ALT;
        if (c.equals(VanGoTheme.GO)) return VanGoTheme.GO.darker();
        if (c.equals(VanGoTheme.DANGER)) return VanGoTheme.DANGER.darker();
        if (c.equals(VanGoTheme.WARN)) return VanGoTheme.WARN.darker();
        return c.darker();
    }

    public void setBaseBackground(Color bg) {
        this.defaultBg = bg;
        this.hoverBg = deriveHover(bg);
        this.pressedBg = hoverBg.darker();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        Color bg = !isEnabled() ? VanGoTheme.SURFACE_ALT
                : press ? pressedBg
                : hover ? hoverBg
                : defaultBg;

        // Soft shadow (skipped for white ghost buttons to keep them light)
        if (!defaultBg.equals(Color.WHITE) && isEnabled()) {
            g2.setColor(new Color(15, 40, 65, 18));
            g2.fill(new RoundRectangle2D.Float(0, 1, w, h - 1, arc, arc));
        }

        // Fill
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h - 1, arc, arc));

        // Inset border
        g2.setColor(darker(bg, 12));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 2, arc, arc));

        // Focus ring
        if (isFocusOwner()) {
            g2.setColor(VanGoTheme.ORANGE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 5, arc, arc));
        }

        g2.dispose();
        super.paintComponent(g);
    }

    private static Color darker(Color c, int amt) {
        return new Color(
                Math.max(0, c.getRed() - amt),
                Math.max(0, c.getGreen() - amt),
                Math.max(0, c.getBlue() - amt)
        );
    }
}