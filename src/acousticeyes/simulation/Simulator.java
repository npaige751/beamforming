package acousticeyes.simulation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class Simulator {
    public List<Speaker> speakers;
    public static final double SPS = 48000;
    public static final double SPEED_OF_SOUND = 340.0; // m/s

    public Simulator(Speaker... speakers) {
        this.speakers = Arrays.stream(speakers).toList();
    }

    public Simulator(List<Speaker> speakers) {
        this.speakers = speakers;
    }

    public void simulate(Microphone m, int ns) {
        double[] samples = new double[ns];
        for (Speaker sp : speakers) {
            double dist = sp.pos.distance(m.noisyPos);
            double delay = dist / SPEED_OF_SOUND;
            double attenuation = 1.0 / (dist * dist); // 1-meter reference level
            int delaySamples = (int) (delay * SPS);
            double fractionalSampleDelay = ((delay * SPS) - delaySamples) / SPS;
            for (int s = 0; s < ns - delaySamples; s++) {
                double t = s / SPS;
                samples[s + delaySamples] += sp.source.get(t - fractionalSampleDelay) * attenuation;
            }
        }
        m.recording = samples;
    }

    private static final Color[] colorMap = new Color[] {
            new Color(40, 0, 80),
            new Color(0, 100, 160),
            new Color(20, 220, 120),
            new Color(180, 220, 30),
            new Color(200, 120, 20),
            new Color(240, 20, 0)
    };

    private static int tonemap(double x, double scale, double offset) {
        double lx = Math.log1p(x) * scale + offset;
        if (lx >= colorMap.length-1) return colorMap[colorMap.length-1].getRGB();
        int ci = (int) lx;
        double d = lx - ci;
        int r = (int) (d * colorMap[ci+1].getRed() + (1-d) * colorMap[ci].getRed());
        int g = (int) (d * colorMap[ci+1].getGreen() + (1-d) * colorMap[ci].getGreen());
        int b = (int) (d * colorMap[ci+1].getBlue() + (1-d) * colorMap[ci].getBlue());
        return new Color(r,g,b).getRGB();
    }

    public double[][] scan2d(PhasedArray arr, int xs, int ys, double thetaStart, double thetaEnd, double phiStart, double phiEnd) {
        arr.simulate(this, 5000);
        return arr.sweepBeam(thetaStart, thetaEnd, xs, phiStart, phiEnd, ys, 2000 / SPS, 100);
    }

    public BufferedImage render(double[][] heatmap) {
        int xs = heatmap.length;
        int ys = heatmap[0].length;
        BufferedImage img = new BufferedImage(xs, ys, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < xs; x++) {
            for (int y = 0; y < ys; y++) {
                img.setRGB(x, ys - y - 1, tonemap(heatmap[x][y], 0.5, 0));
            }
        }
        return img;
    }
}
