import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class VanGoLogoPanel extends JPanel {
    private final boolean horizontal;

    public VanGoLogoPanel(boolean horizontal) {
        this.horizontal = horizontal;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (horizontal) {
            // Van icon block
            int iconSize = Math.min(h - 8, 40);
            int iconX = 2;
            int iconY = (h - iconSize) / 2;
            
            g2.setColor(VanGoTheme.NAVY);
            g2.fill(new RoundRectangle2D.Float(iconX, iconY, iconSize, iconSize, 8, 8));
            g2.setColor(VanGoTheme.ORANGE);
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Float(iconX + 3, iconY + 3, iconSize - 6, iconSize - 6, 6, 6));

            // Small wheels
            int wheelSize = iconSize / 5;
            g2.setColor(Color.WHITE);
            g2.fillOval(iconX + iconSize / 5, iconY + iconSize - wheelSize, wheelSize, wheelSize);
            g2.fillOval(iconX + iconSize - iconSize / 5 - wheelSize,
                        iconY + iconSize - wheelSize, wheelSize, wheelSize);

            // Wordmark
            int textX = iconX + iconSize + 8;
            int textBaseline = iconY + (iconSize + g2.getFontMetrics().getAscent()) / 2 - 2;
            g2.setFont(VanGoTheme.font(Math.max(16, iconSize - 8), Font.BOLD));
            g2.setColor(VanGoTheme.NAVY);
            String van = "VAN";
            g2.drawString(van, textX, textBaseline);
            int vanW = g2.getFontMetrics().stringWidth(van);
            g2.setColor(VanGoTheme.ORANGE);
            g2.drawString("GO", textX + vanW, textBaseline);
        } else {
            int size = Math.min(w, h) - 4;
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            g2.setColor(VanGoTheme.NAVY);
            g2.fill(new RoundRectangle2D.Float(x, y, size, size, 10, 10));
            g2.setColor(VanGoTheme.ORANGE);
            g2.setFont(VanGoTheme.font(size / 2, Font.BOLD));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("V", x + (size - fm.stringWidth("V")) / 2,
                          y + (size + fm.getAscent() - fm.getDescent()) / 2);
        }

        g2.dispose();
    }
}