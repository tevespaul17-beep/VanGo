import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class AnimatedSeatButton extends JComponent {
    private final int seatNumber;
    private final boolean isJumpSeat;
    private final boolean isOccupied;
    private boolean isSelected;

    private float currentScale = 1.0f;
    private float targetScale = 1.0f;
    private float selectionProgress = 0.0f;
    private float targetSelectionProgress = 0.0f;

    private final Timer animTimer;
    private final Runnable onClickAction;

    public AnimatedSeatButton(int seatNumber, boolean isJumpSeat, boolean isOccupied, Runnable onClickAction) {
        this.seatNumber = seatNumber;
        this.isJumpSeat = isJumpSeat;
        this.isOccupied = isOccupied;
        this.onClickAction = onClickAction;
        setOpaque(false);
        setCursor(isOccupied ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        animTimer = new Timer(16, e -> updateAnimation());
        animTimer.start();

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (!isOccupied) targetScale = 1.06f; }
            @Override public void mouseExited(MouseEvent e)  { if (!isOccupied) targetScale = 1.0f; }
            @Override public void mousePressed(MouseEvent e) { if (!isOccupied) targetScale = 0.94f; }
            @Override public void mouseReleased(MouseEvent e) {
                if (!isOccupied) {
                    targetScale = 1.06f;
                    isSelected = !isSelected;
                    targetSelectionProgress = isSelected ? 1.0f : 0.0f;
                    if (onClickAction != null) onClickAction.run();
                }
            }
        });
    }

    private void updateAnimation() {
        boolean repaint = false;
        if (Math.abs(targetScale - currentScale) > 0.001f) {
            currentScale += (targetScale - currentScale) * 0.25f;
            repaint = true;
        }
        if (Math.abs(targetSelectionProgress - selectionProgress) > 0.001f) {
            selectionProgress += (targetSelectionProgress - selectionProgress) * 0.25f;
            repaint = true;
        }
        if (repaint) repaint();
    }

    public boolean isSelected() { return isSelected; }
    public int getSeatNumber() { return seatNumber; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        g2.translate(w / 2.0, h / 2.0);
        g2.scale(currentScale, currentScale);
        g2.translate(-w / 2.0, -h / 2.0);

        Color bgBase;
        if (isOccupied) {
            bgBase = VanGoTheme.DANGER;
        } else if (isJumpSeat) {
            bgBase = VanGoTheme.SEAT_JUMP;
        } else {
            bgBase = VanGoTheme.SEAT_AVAILABLE;
        }

        Color bg = lerp(bgBase, VanGoTheme.ORANGE, selectionProgress);
        Shape seatShape = createSeatShape(w, h);
        g2.setColor(bg);
        g2.fill(seatShape);
        g2.setColor(VanGoTheme.NAVY_DARK);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(seatShape);

        Color text = isOccupied || selectionProgress > 0.2f ? Color.WHITE : Color.BLACK;
        g2.setColor(text);
        int fontSize = Math.max(14, Math.min(32, Math.round(Math.min(w, h) * 0.44f)));
        g2.setFont(VanGoTheme.font(fontSize, Font.BOLD));
        String label = String.valueOf(seatNumber);
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(label)) / 2;
        int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(label, tx, ty);

        g2.dispose();
    }

    private Shape createSeatShape(int width, int height) {
        float arc = Math.min(30, Math.min(width, height) * 0.24f);
        if (!isJumpSeat) {
            return new RoundRectangle2D.Float(1, 1, width - 2, height - 2, arc, arc);
        }

        float leftSeatWidth = width * 0.34f;
        Path2D.Float shape = new Path2D.Float();
        shape.moveTo(leftSeatWidth, 1);
        shape.quadTo(1, 1, 1, height * 0.28f);
        shape.curveTo(-1, height * 0.40f, -1, height * 0.60f, 1, height * 0.72f);
        shape.quadTo(1, height - 1, leftSeatWidth, height - 1);
        shape.lineTo(width - arc, height - 1);
        shape.quadTo(width - 1, height - 1, width - 1, height - arc);
        shape.lineTo(width - 1, arc);
        shape.quadTo(width - 1, 1, width - arc, 1);
        shape.closePath();
        return shape;
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

}