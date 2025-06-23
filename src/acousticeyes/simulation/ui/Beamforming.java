package acousticeyes.simulation.ui;

import javax.swing.*;

/* Top-level UI container for beamforming simulation */
public class Beamforming extends JFrame {

    public ArrayPanel arrayPanel;
    public HeatmapPanel heatmapPanel;
    public SourcesPanel sourcesPanel;

    public Beamforming() {
        super("Beamforming");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        heatmapPanel = new HeatmapPanel(1800, 1400, 100, 100);
        heatmapPanel.setBounds(10,10,1800,1400);

        arrayPanel = new ArrayPanel(600, 900);
        arrayPanel.setBounds(1820,10,600,800);
        arrayPanel.addArrayListener(heatmapPanel);

        sourcesPanel = new SourcesPanel(600, 500);
        sourcesPanel.setBounds(1820, 900, 600, 500);
        sourcesPanel.addSpeakerListener(heatmapPanel);

        add(arrayPanel);
        add(heatmapPanel);
        add(sourcesPanel);

        setSize(2400,1400);
        setVisible(true);
    }

    public static void main(String[] args) {
        new Beamforming();
    }
}
