package acousticeyes.simulation.ui;

import acousticeyes.simulation.PhasedArray;
import acousticeyes.simulation.Simulator;
import acousticeyes.simulation.Speaker;
import acousticeyes.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class HeatmapPanel extends JPanel implements ArrayListener, SpeakerListener {

    private Simulator simulator;
    private PhasedArray phasedArray;
    private BufferedImage renderedImage;
    private int xs, ys;

    private MainPanel mainPanel;
    private JPanel paramsPanel;

    private Parameter fovTheta, fovPhi;
    // possible others: color scaling; resolution; thresholds for source detection, etc.

    private Thread simulationThread;
    private LinkedBlockingQueue<SimRequest> queue = new LinkedBlockingQueue<>();

    private static final int PP_WD = 500;
    private static final int PP_HT = 100;

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

        paramsPanel = new JPanel();
        paramsPanel.setLayout(null);
        paramsPanel.add(fovTheta);
        paramsPanel.add(fovPhi);
        fovTheta.setBounds(0,0, PP_WD, PARAM_HT);
        fovPhi.setBounds(0,PARAM_HT, PP_WD, PARAM_HT);
        paramsPanel.setBounds(0, height - PP_HT, PP_WD, PP_HT);

        add(paramsPanel);
        add(mainPanel);

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

    private static class SimRequest {
        Simulator simulator;
        PhasedArray phasedArray;
        double fovTheta, fovPhi;
        int xs, ys;

        SimRequest(Simulator sim, PhasedArray arr, int xs, int ys, double ft, double fp) {
            simulator = sim;
            phasedArray = arr;
            fovTheta = ft;
            fovPhi = fp;
            this.xs = xs;
            this.ys = ys;
        }
    }

    public void scheduleRun() {
        SimRequest req = new SimRequest(simulator, phasedArray, xs, ys, fovTheta.get(), fovPhi.get());
        queue.add(req);
    }

    // don't call on EDT
    private void runSimulation(SimRequest req) {
        double fovt = Utils.radians(req.fovTheta);
        double fovp = Utils.radians(req.fovPhi);
        double[][] hm = req.simulator.scan2d(req.phasedArray, req.xs, req.ys, -fovt/2, fovt/2, -fovp/2, fovp/2);
        BufferedImage img = req.simulator.render(hm);
        // todo: render actual / proposed source locations on top of this image
        SwingUtilities.invokeLater(() -> {
            renderedImage = img;
            repaint();
        });
    }

    private class MainPanel extends JPanel {

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(Color.RED);
            g.fillRect(10,10,100,100);
            if (renderedImage != null) {
                g.drawImage(renderedImage, 0, 0, getWidth(), getHeight(), null);
            }
        }
    }

}
