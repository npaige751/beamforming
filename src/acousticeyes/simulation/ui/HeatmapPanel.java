package acousticeyes.simulation.ui;

import acousticeyes.simulation.PhasedArray;
import acousticeyes.simulation.Simulator;
import acousticeyes.simulation.Speaker;
import acousticeyes.simulation.DAMAS;
import acousticeyes.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/* Displays rendered beamforming heatmaps */
public class HeatmapPanel extends JPanel implements ArrayListener, SpeakerListener {

    private Simulator simulator;
    private PhasedArray phasedArray;
    private DAMAS damas;
    private BufferedImage renderedImage;
    private int xs, ys;

    private MainPanel mainPanel;
    private JPanel paramsPanel;
    private JCheckBox useDamas;

    private Parameter fovTheta, fovPhi, resolution, colorScale;
    // possible others: color scaling; resolution; thresholds for source detection, etc.

    private Thread simulationThread;
    private LinkedBlockingQueue<SimRequest> queue = new LinkedBlockingQueue<>();

    // paramsPanel dimensions
    private static final int PP_WD = 500;
    private static final int PP_HT = 200;

    private static final int PARAM_HT = 40;

    public HeatmapPanel(int width, int height, int xs, int ys) {
        setSize(width, height);
        setLayout(null);
        this.xs = xs;
        this.ys = ys;
        simulator = new Simulator(new ArrayList<>());

        mainPanel = new MainPanel();
        mainPanel.setBounds(0, 0, width, height - PP_HT);

        fovTheta = new Parameter("FOV theta", 90, 20, 180, false, true, PP_WD, PARAM_HT, (x) -> scheduleRun());
        fovPhi = new Parameter("FOV phi", 90, 20, 180, false, true, PP_WD, PARAM_HT, (x) -> scheduleRun());
        resolution = new Parameter("Resolution", 100, 5, 200, true, true, PP_WD, PARAM_HT, (x) -> scheduleRun());
        colorScale = new Parameter("Color scale", 4, 0, 10, false, false, PP_WD, PARAM_HT, (x) -> scheduleRun());
        useDamas = new JCheckBox("Use DAMAS", false);
        useDamas.addActionListener((x) -> scheduleRun());

        paramsPanel = new JPanel();
        paramsPanel.setLayout(null);
        paramsPanel.add(fovTheta);
        paramsPanel.add(fovPhi);
        paramsPanel.add(resolution);
        paramsPanel.add(colorScale);
        paramsPanel.add(useDamas);
        fovTheta.setBounds(0,0, PP_WD, PARAM_HT);
        fovPhi.setBounds(0,PARAM_HT, PP_WD, PARAM_HT);
        colorScale.setBounds(0, PARAM_HT*2, PP_WD, PARAM_HT);
        resolution.setBounds(0, PARAM_HT * 3, PP_WD, PARAM_HT);
        useDamas.setBounds(0, PARAM_HT * 4, PP_WD, PARAM_HT);
        paramsPanel.setBounds(0, height - PP_HT, PP_WD, PP_HT);

        add(paramsPanel);
        add(mainPanel);

        /* Simulation runs could potentially be lengthy, so it's best not to do this computation on
           the UI thread. With a basic queue, dozens of unnecessary simulation runs could be scheduled
           from a single slider adjustment, which could take many seconds to complete. Instead, only
           actually service the last request in the queue.
         */
        simulationThread = new Thread(() -> {
            while (true) {
                try {
                    SimRequest req = queue.take();
                    while (!queue.isEmpty()) { // discard intermediate requests, only take most recent
                        req = queue.poll();
                    }
                    runSimulation(req);
                } catch (InterruptedException ignored) {
                }
            }
        });
        simulationThread.start();
    }

    @Override
    public void phasedArrayUpdated(PhasedArray array) {
        phasedArray = array;
        scheduleRun();
    }

    @Override
    public void speakersUpdated(java.util.List<Speaker> speakers) {
        simulator = new Simulator(speakers);
        scheduleRun();
    }

    // encapsulates all state needed to run a simulation
    private static class SimRequest {
        Simulator simulator;
        PhasedArray phasedArray;
        double fovTheta, fovPhi, colorScale;
        int xs, ys;

        SimRequest(Simulator sim, PhasedArray arr, int xs, int ys, double ft, double fp, double cs) {
            simulator = sim;
            phasedArray = arr;
            fovTheta = ft;
            fovPhi = fp;
            colorScale = cs;
            this.xs = xs;
            this.ys = ys;
        }
    }

    public void scheduleRun() {
        SimRequest req = new SimRequest(simulator, phasedArray, (int) resolution.get(), (int) resolution.get(), fovTheta.get(), fovPhi.get(), colorScale.get());
        queue.add(req);
    }

    // potentially slow - don't call on the Swing event dispatch thread (this would freeze the UI while
    // the simulation is running)
    private void runSimulation(SimRequest req) {
        boolean damasEnabled = useDamas.isSelected();
        if (damasEnabled) {
            req.xs = Math.min(req.xs, 25);
            req.ys = req.xs;
        }
        double fovt = Utils.radians(req.fovTheta);
        double fovp = Utils.radians(req.fovPhi);
        long time = System.currentTimeMillis();
        double[][] hm = req.simulator.scan2d(req.phasedArray, req.xs, req.ys, -fovt/2, fovt/2, -fovp/2, fovp/2);
        BufferedImage img;
        if (damasEnabled) {
            damas = new DAMAS(req.phasedArray, 6000, req.xs, fovt);
            long atime = System.currentTimeMillis();
            damas.computeArrayResponseMatrix();
            System.out.println(" DAMAS A matrix: " + (System.currentTimeMillis() - atime) + " ms");
            atime = System.currentTimeMillis();
            double[][] damasResult = damas.deconvolve(hm, 50);
            System.out.println(" DAMAS deconvolution: " + (System.currentTimeMillis() - atime) + " ms");
            img = req.simulator.render(damasResult, req.colorScale);
        } else {
            img = req.simulator.render(hm, req.colorScale);
        }
        System.out.println("Beamforming took " + (System.currentTimeMillis() - time) + " ms");
        // todo: render actual / proposed source locations on top of this image
        // all Swing UI rendering must happen on the event dispatch thread
        SwingUtilities.invokeLater(() -> {
            renderedImage = img;
            repaint();
        });
    }

    private class MainPanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            if (renderedImage != null) {
                g.drawImage(renderedImage, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

}
