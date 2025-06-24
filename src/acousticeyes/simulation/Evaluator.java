package acousticeyes.simulation;

import acousticeyes.util.Utils;
import acousticeyes.util.Vec3;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/* Evaluates and graphs array performance as its parameters are varied. */
public class Evaluator {

    public enum IndependentVariable {
        RINGS("Rings"),
        SPOKES("Spokes"),
        MINR("Min Radius"),
        MAXR("Max Radius"),
        SPIRAL("Spiral"),
        EXP("Exponentialness"),
        FREQ("Frequency");

        final String name;
        IndependentVariable(String name) {
            this.name = name;
        }
    }

    public enum DependentVariable {
        BEAMWIDTH("3-dB Beamwidth"),
        MAX_SL("Max Sidelobe Intensity"),
        AVG_SL("Avg Sidelobe Intensity"),
        AVG30_SL("Avg Sidelobe Within 30 deg"),
        AVG60_SL("Avg Sidelobe Within 60 deg");

        final String name;
        DependentVariable(String name) {
            this.name = name;
        }
    }

    private final IndependentVariable xVar, cVar; // variables to represent by x-axis position and separate colored lines, respectively
    private final DependentVariable yVar;
    private final boolean useExp; // linear vs. exponential spacing for array

    // default array/simulation parameters for non-varying variables
    private static final double DEFAULT_FREQ = 3000;
    private static final double DEFAULT_MINR = 0.05;
    private static final double DEFAULT_MAXR = 0.3;
    private static final int DEFAULT_RINGS = 6;
    private static final int DEFAULT_SPOKES = 16;
    private static final double DEFAULT_SPIRAL = 1;
    private static final double DEFAULT_EXP = 1;

    public Evaluator(IndependentVariable x, IndependentVariable c, DependentVariable y, boolean useExp) {
        this.xVar = x;
        this.cVar = c;
        this.yVar = y;
        this.useExp = useExp;
    }

    private Simulator getSimulator(double x, double c) {
        double freq = DEFAULT_FREQ;
        if (xVar == IndependentVariable.FREQ) freq = x;
        if (cVar == IndependentVariable.FREQ) freq = c;
        return new Simulator(new Speaker(new Vec3(0,0,10), new SinusoidSource(freq, 1, 0)));
    }

    private PhasedArray getArray(double x, double c) {
        double minr = DEFAULT_MINR;
        double maxr = DEFAULT_MAXR;
        int rings = DEFAULT_RINGS;
        int spokes = DEFAULT_SPOKES;
        double spiral = DEFAULT_SPIRAL;
        double exp = DEFAULT_EXP;
        switch (xVar) {
            case RINGS -> rings = (int) x;
            case SPOKES -> spokes = (int) x;
            case MINR -> minr = x;
            case MAXR -> maxr = x;
            case SPIRAL -> spiral = x;
            case EXP -> exp = x;
        }
        switch (cVar) {
            case RINGS -> rings = (int) c;
            case SPOKES -> spokes = (int) c;
            case MINR -> minr = c;
            case MAXR -> maxr = c;
            case SPIRAL -> spiral = c;
            case EXP -> exp = c;
        }
        return PhasedArray.radial(rings, spokes, minr, maxr, exp, useExp, spiral, 0);
    }

    private double evaluate(double x, double c) {
        Simulator sim = getSimulator(x, c);
        PhasedArray arr = getArray(x, c);
        arr.simulate(sim, 5000);
        if (yVar == DependentVariable.BEAMWIDTH) {
            return getBeamwidth(arr);
        } else {
            SidelobeInfo sidelobe = evaluateSidelobes(arr, 100);
            return switch (yVar) {
                case MAX_SL -> sidelobe.max;
                case AVG_SL -> sidelobe.avg;
                case AVG30_SL -> sidelobe.avg30;
                case AVG60_SL -> sidelobe.avg60;
                default -> 0;
            };
        }
    }

    private static double getBeamwidth(PhasedArray arr) {
        double centerResponse = Utils.rms(arr.delayAndSum(arr.farFieldBeamformingDelays(0, 0), 2000 / Simulator.SPS, 1000));
        double theta_step = 0.001;
        double theta = 0;
        while (theta < Math.PI) {
            theta += theta_step;
            double resp = Utils.rms(arr.delayAndSum(arr.farFieldBeamformingDelays(theta, 0), 2000 / Simulator.SPS, 1000));
            if (resp < 0.5 * centerResponse) {
                break;
            }
        }
        return Utils.degrees(theta);
    }

    private static class SidelobeInfo {
        double max, rmax, avg, avg30, avg60, size;
    }

    private static SidelobeInfo evaluateSidelobes(PhasedArray arr, int steps) {
        double centerResponse = Utils.rms(arr.delayAndSum(arr.farFieldBeamformingDelays(0, 0), 2000 / Simulator.SPS, 100));
        double[][] heatmap = arr.sweepBeam(0, Utils.radians(90), steps, 0, Utils.radians(90), steps, 2000/Simulator.SPS, 100);
        // find first minimum along theta; this defines where sidelobes can begin. Assumes approximate radial symmetry
        int th = 1;
        while (th < steps) {
            if (heatmap[th][0] > heatmap[th-1][0]) break;
            th++;
        }
        SidelobeInfo info = new SidelobeInfo();
        info.size = th * 90.0 / steps;
        int count = 0;
        int count30 = 0;
        int count60 = 0;
        for (int i=0; i < steps; i++) {
            for (int j=0; j < steps; j++) {
                double r = Math.sqrt(i*i + j*j);
                if (r > th) {
                    if (heatmap[i][j] > info.max) {
                        info.max = heatmap[i][j];
                        info.rmax = r;
                    }
                    info.avg += heatmap[i][j];
                    if (r*90.0/steps < 30) {
                        info.avg30 += heatmap[i][j];
                        count30++;
                    }
                    if (r*90.0/steps < 60) {
                        info.avg60 += heatmap[i][j];
                        count60++;
                    }
                    count++;
                }
            }
        }
        info.avg = Utils.db(info.avg / count);
        info.avg30 = Utils.db(info.avg30 / count30);
        info.avg60 = Utils.db(info.avg60 / count60);
        info.max = Utils.db(info.max / centerResponse);
        info.rmax = info.rmax * 90 / steps;
        return info;
    }

    private class GraphOptions {
        double xmin, xmax, cmin, cmax, ymin, ymax;
        int xsteps, csteps;
        int xsize, ysize;

        private static final int TOP_MARGIN = 65;
        private static final int LEFT_MARGIN = 120;
        private static final int RIGHT_MARGIN = 10;
        private static final int BOT_MARGIN = 80;
        private static final int TIC_LENGTH = 12;

        private static final Color BACKGROUND_COLOR = Color.BLACK;
        private static final Color AXIS_COLOR = Color.WHITE;
        private static final Color GRID_COLOR = new Color(50, 50, 50);
        private static final Color TEXT_COLOR = Color.WHITE;
        private static final Font FONT = new Font("Courier", Font.PLAIN, 12);

        GraphOptions(double xmin, double xmax, double cmin, double cmax, double ymin, double ymax, int xsteps, int csteps, int xsize, int ysize) {
            this.xmin = xmin;
            this.xmax = xmax;
            this.cmin = cmin;
            this.cmax = cmax;
            this.ymin = ymin;
            this.ymax = ymax;
            this.xsteps = xsteps;
            this.csteps = csteps;
            this.xsize = xsize;
            this.ysize = ysize;
        }

        BufferedImage graph() {
            int gwidth = xsize - LEFT_MARGIN - RIGHT_MARGIN;
            int gheight = ysize - TOP_MARGIN - BOT_MARGIN;
            int gcx = LEFT_MARGIN + gwidth / 2;
            int gcy = TOP_MARGIN + gheight / 2;
            int gleft = LEFT_MARGIN;
            int gright = xsize - RIGHT_MARGIN;
            int gbot = ysize - BOT_MARGIN;
            int gtop = TOP_MARGIN;

            BufferedImage img = new BufferedImage(xsize, ysize, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();

            g.setFont(FONT);
            FontMetrics metrics = g.getFontMetrics(FONT);

            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, xsize, ysize);

            // draw tics and tic values
            int xstepsPerTic = (xsteps / 10) + 1;
            for (int xt = 0; xt <= xsteps; xt += xstepsPerTic) {
                int x = (int) (gleft + gwidth * xt / (xsteps - 1.0));
                g.setColor(GRID_COLOR);
                g.drawLine(x, gtop, x, gbot);
                g.setColor(AXIS_COLOR);
                g.drawLine(x, gbot - TIC_LENGTH/2, x, gbot + TIC_LENGTH/2);

                double xval = xmin + (xmax - xmin) * xt / (xsteps - 1.0);
                String ticLabel = ticLabelString(xval);
                double labelWidth = metrics.getStringBounds(ticLabel, g).getWidth();
                g.setColor(TEXT_COLOR);
                g.drawString(ticLabel, (int) (x - labelWidth/2), gbot + TIC_LENGTH + 20);
            }

            int ytics = 10;
            for (int yt = 0; yt < ytics; yt++) {
                int y = (int) (gbot - (gbot-gtop) * yt / (ytics-1.0));
                g.setColor(GRID_COLOR);
                g.drawLine(gleft, y, gright, y);
                g.setColor(AXIS_COLOR);
                g.drawLine(gleft - TIC_LENGTH/2, y, gleft + TIC_LENGTH/2, y);

                double yval = ymin + (ymax - ymin) * yt / (ytics - 1.0);
                String ticLabel = ticLabelString(yval);
                double labelWidth = metrics.getStringBounds(ticLabel, g).getWidth();
                g.setColor(TEXT_COLOR);
                g.drawString(ticLabel, (int) (gleft - TIC_LENGTH/2 - 10 - labelWidth), y + 5);
            }

            g.setColor(AXIS_COLOR);
            g.drawLine(gleft, gtop, gleft, gbot);
            g.drawLine(gleft, gbot, gright, gbot);

            // x axis label
            g.setColor(TEXT_COLOR);
            double xlabelWidth = metrics.getStringBounds(xVar.name, g).getWidth();
            g.drawString(xVar.name, (int) (gcx - xlabelWidth/2), ysize - 10);

            // y axis label
            double yLabelWidth = metrics.getStringBounds(yVar.name, g).getWidth();
            AffineTransform transform = AffineTransform.getRotateInstance(Utils.radians(-90));
            transform.preConcatenate(AffineTransform.getTranslateInstance(20, gcy + yLabelWidth/2));
            g.setTransform(transform);
            g.drawString(yVar.name, 0, 0);
            g.setTransform(AffineTransform.getRotateInstance(0));

            // c axis legend
            g.setColor(TEXT_COLOR);
            double cLabelWidth = metrics.getStringBounds(cVar.name, g).getWidth();
            int cxstart = (int) (gright - Math.max(100, cLabelWidth));
            g.drawString(cVar.name, cxstart, 20);
            g.setColor(getColor(0.0));
            g.drawLine(cxstart, 35, cxstart + 15, 35);
            g.setColor(getColor(1.0));
            g.drawLine(cxstart, 50, cxstart + 15, 50);

            g.setColor(TEXT_COLOR);
            g.drawString(ticLabelString(cmin), cxstart + 25, 40);
            g.drawString(ticLabelString(cmax), cxstart + 25, 55);

            // title
            String title = "Plot of " + yVar.name + " vs. " + xVar.name + " and " + cVar.name;
            double titleWidth = metrics.getStringBounds(title, g).getWidth();
            g.drawString(title, (int) (xsize/2 - titleWidth/2), TOP_MARGIN/2);

            // plots
            for (int cs = 0; cs < csteps; cs++) {
                System.out.println("Plotting " + (cs+1) + " of " + csteps + "...");
                double cfrac = cs / (csteps - 1.0);
                g.setColor(getColor(cfrac));
                int prevxp = 0;
                int prevyp = 0;
                double c = cmin + (cmax - cmin) * cfrac;
                for (int i = 0; i < xsteps; i++) {
                    double x = xmin + (xmax - xmin) * i / (xsteps - 1.0);
                    double y = evaluate(x, c);
                    System.out.println(y);
                    int xp = (int) Utils.lerp(xmin, xmax, gleft, gright, x);
                    int yp = (int) Utils.lerp(ymin, ymax, gbot, gtop, y);
                    if (i != 0) {
                        g.drawLine(prevxp, prevyp, xp, yp);
                    }
                    prevxp = xp;
                    prevyp = yp;
                }
            }
            return img;
        }

        private static Color getColor(double cfrac) {
            return new Color(Color.HSBtoRGB((float) cfrac * 0.5f, 0.8f, 0.8f));
        }
    }

    private static String ticLabelString(double x) {
        String s = x + "";
        if (s.contains(".")) {
            if (Math.abs(x - (int) x) < 1e-5) return "" + (int) x;
            return s.substring(0, Math.min(s.indexOf('.') + 3, s.length()));
        }
        return s;
    }

    public static void main(String[] args) throws IOException {
        Evaluator e = new Evaluator(IndependentVariable.FREQ, IndependentVariable.SPIRAL, DependentVariable.AVG30_SL, false);
        ImageIO.write(e.new GraphOptions(1000, 10000, 0, 1, -20, 0, 10, 8, 1000, 800).graph(),
                "png",
                new File("test.png"));
    }
}
