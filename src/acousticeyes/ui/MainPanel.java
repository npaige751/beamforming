package acousticeyes.ui;

import acousticeyes.util.ColorMap;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MainPanel extends JPanel {

    private BufferedImage image;
    private double scale = 3.0;

    public MainPanel() {
    }

    @Override
    public void paintComponent(Graphics g) {
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
        }
    }

    public void heatmapUpdated(double[][] hm) {
        SwingUtilities.invokeLater(() -> {
            image = ColorMap.DEFAULT.render(hm, scale);
            repaint();
        });
    }
}
