package acousticeyes.util;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ColorMap {

    public Color[] colors;

    public ColorMap(Color[] colors) {
        this.colors = colors;
    }

    public static final ColorMap DEFAULT = new ColorMap(new Color[] {
            new Color(40, 0, 80),
            new Color(0, 100, 160),
            new Color(20, 220, 120),
            new Color(180, 220, 30),
            new Color(200, 120, 20),
            new Color(240, 20, 0)
    });

    // use a logarithmic mapping for heatmap values to accommodate a higher dynamic range
    public int map(double x, double scale, double offset) {
        double lx = Math.log1p(x) * scale + offset;
        if (lx >= colors.length-1) return colors[colors.length-1].getRGB();
        int ci = (int) lx;
        double d = lx - ci;
        int r = (int) (d * colors[ci+1].getRed() + (1-d) * colors[ci].getRed());
        int g = (int) (d * colors[ci+1].getGreen() + (1-d) * colors[ci].getGreen());
        int b = (int) (d * colors[ci+1].getBlue() + (1-d) * colors[ci].getBlue());
        return new Color(r,g,b).getRGB();
    }

    public BufferedImage render(double[][] heatmap, double colorScale) {
        int xs = heatmap.length;
        int ys = heatmap[0].length;
        BufferedImage img = new BufferedImage(xs, ys, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                img.setRGB(x, ys - y - 1, ColorMap.DEFAULT.map(heatmap[x][y], colorScale, 0));
            }
        }
        return img;
    }

}
