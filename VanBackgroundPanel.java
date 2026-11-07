import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;

public class VanBackgroundPanel extends JPanel {
    private static final int CANVAS_WIDTH = 1000;
    private static final int CANVAS_HEIGHT = 455;
    private static final int IMAGE_PADDING = 8;
    private static final Map<String, String> MODEL_IMAGES = new HashMap<>();
    private static final Map<String, BufferedImage> IMAGE_CACHE = new HashMap<>();

    static {
        MODEL_IMAGES.put(VanGoKiosk.MODEL_HIACE_COMMUTER,
                "toyota_Hiace_Commuter_seating_top_view_lineart_v1.png");
        MODEL_IMAGES.put(VanGoKiosk.MODEL_HIACE_DELUXE,
                "toyota_Hiace_Commuter_Deluxe_seating_top_view_lineart_v1.png");
        MODEL_IMAGES.put(VanGoKiosk.MODEL_NISSAN_NV350,
                "nissan_nv350_seating_top_view_lineart_v1.png");
        MODEL_IMAGES.put(VanGoKiosk.MODEL_NISSAN_URVAN,
                "nissan_Urvan_seating_top_view_lineart_v1.png");
    }

    private String vanModel = VanGoKiosk.MODEL_HIACE_COMMUTER;
    private BufferedImage vanImage;

    public VanBackgroundPanel() {
        setLayout(null);
        setOpaque(false);
        Dimension canvasSize = new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
        setPreferredSize(canvasSize);
        setMinimumSize(canvasSize);
        setMaximumSize(canvasSize);
        setVanModel(vanModel);
    }

    public void setVanModel(String model) {
        if (model != null && !model.trim().isEmpty()) {
            vanModel = model.trim().toUpperCase();
        }

        String imageName = MODEL_IMAGES.get(vanModel);
        if (imageName == null) {
            throw new IllegalArgumentException("No seating diagram is configured for van model: " + vanModel);
        }
        vanImage = loadImage(imageName);
        repaint();
    }

    public Rectangle2D getImageBounds() {
        int availableWidth = CANVAS_WIDTH - IMAGE_PADDING * 2;
        int availableHeight = CANVAS_HEIGHT - IMAGE_PADDING * 2;
        double scale = Math.min(
                (double) availableWidth / vanImage.getWidth(),
                (double) availableHeight / vanImage.getHeight());
        double imageWidth = vanImage.getWidth() * scale;
        double imageHeight = vanImage.getHeight() * scale;
        return new Rectangle2D.Double(
                (CANVAS_WIDTH - imageWidth) / 2.0,
                (CANVAS_HEIGHT - imageHeight) / 2.0,
                imageWidth,
                imageHeight);
    }

    private static BufferedImage loadImage(String imageName) {
        BufferedImage cached = IMAGE_CACHE.get(imageName);
        if (cached != null) {
            return cached;
        }

        try {
            BufferedImage source;
            java.net.URL resource = VanBackgroundPanel.class.getResource("/images/" + imageName);
            if (resource != null) {
                source = ImageIO.read(resource);
            } else {
                source = ImageIO.read(new File("images", imageName));
            }
            if (source == null) {
                throw new IOException("Unsupported or unreadable image");
            }

            BufferedImage tinted = new BufferedImage(
                    source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
            int ink = VanGoTheme.NAVY.getRGB() & 0x00FFFFFF;
            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int alpha = source.getRGB(x, y) >>> 24;
                    tinted.setRGB(x, y, (alpha << 24) | ink);
                }
            }
            IMAGE_CACHE.put(imageName, tinted);
            return tinted;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load van seating diagram: " + imageName, e);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        Rectangle2D imageBounds = getImageBounds();
        g2.drawImage(vanImage,
                (int) Math.round(imageBounds.getX()),
                (int) Math.round(imageBounds.getY()),
                (int) Math.round(imageBounds.getWidth()),
                (int) Math.round(imageBounds.getHeight()),
                null);

        g2.dispose();
    }
}
