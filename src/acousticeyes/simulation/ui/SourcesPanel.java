package acousticeyes.simulation.ui;

import acousticeyes.simulation.SinusoidSource;
import acousticeyes.simulation.Speaker;
import acousticeyes.util.Utils;
import acousticeyes.util.Vec3;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/* UI for managing a list of speaker positions and sound sources */
public class SourcesPanel extends JPanel implements ListSelectionListener {

    private Parameter theta, phi, freq, ampl, dist;

    private JList<SourceDescriptor> list;
    private JButton add, remove;
    private JLabel title;

    private int lastSelected = 0;
    private ArrayList<SourceDescriptor> descriptors = new ArrayList<>();
    private ArrayList<SpeakerListener> speakerListeners = new ArrayList<>();

    private static final int PARAM_HT = 40;

    private static class SourceDescriptor {
        double theta, phi, freq, ampl, dist;

        SourceDescriptor(double theta, double phi, double freq, double ampl, double dist) {
            this.theta = theta;
            this.phi = phi;
            this.freq = freq;
            this.ampl = ampl;
            this.dist = dist;
        }

        Speaker getSpeaker() {
            Vec3 p = new Vec3(0,0,1).rotX(phi).rotY(theta).mul(dist);
            return new Speaker(p, new SinusoidSource(freq, ampl, 0));
        }

        public String toString() {
            return freq + "Hz, A = " + ampl + ", theta = " + theta + ", phi = " + phi + ", dist = " + dist;
        }
    }

    public SourcesPanel(int width, int height) {
        setLayout(null);

        descriptors.add(new SourceDescriptor(0, 0, 1000, 100, 10));

        title = new JLabel("SOURCES");
        title.setBounds(0,0,width, PARAM_HT);

        add = new JButton("Add");
        add.setBounds(0, PARAM_HT, 150, 30);
        add.addActionListener((ActionEvent e) -> {
            descriptors.add(new SourceDescriptor(0, 0, 1000, 10, 10));
            list.setListData(new Vector<>(descriptors));
            list.setSelectedIndex(descriptors.size() - 1);
        });

        remove = new JButton("Remove");
        remove.setBounds(160, PARAM_HT, 150, 30);
        remove.addActionListener((ActionEvent e) -> {
            int index = list.getSelectedIndex();
            descriptors.remove(index);
            list.setListData(new Vector<>(descriptors));
            lastSelected = -1;
            fireListeners();
        });

        int list_ht = height - 30 - PARAM_HT * 6 - 20;
        list = new JList<>(new Vector<>(descriptors));
        list.setBounds(0, PARAM_HT + 30, width, list_ht);
        list.addListSelectionListener(this);

        theta = new Parameter("Theta", 0, -90, 90,false, false, width, PARAM_HT, (x) -> updateSelectedDescriptor());
        phi = new Parameter("Phi", 0, -90, 90, false, false, width, PARAM_HT, (x) -> updateSelectedDescriptor());
        dist = new Parameter("Distance", 10, 0.1, 100, true, false, width, PARAM_HT, (x) -> updateSelectedDescriptor());
        freq = new Parameter("Frequency", 1000, 100, 10000, true, true, width, PARAM_HT, (x) -> updateSelectedDescriptor());
        ampl = new Parameter("Amplitude", 100, 1, 1000, true, false, width, PARAM_HT, (x) -> updateSelectedDescriptor());

        add(title);
        add(add);
        add(remove);
        add(list);
        add(theta);
        add(phi);
        add(dist);
        add(freq);
        add(ampl);

        int starty = PARAM_HT + 30 + list_ht;
        theta.setBounds(0, starty, width, PARAM_HT);
        phi.setBounds(0, starty + PARAM_HT, width, PARAM_HT);
        dist.setBounds(0, starty + PARAM_HT * 2, width, PARAM_HT);
        freq.setBounds(0, starty + PARAM_HT * 3, width, PARAM_HT);
        ampl.setBounds(0, starty + PARAM_HT * 4, width, PARAM_HT);
    }

    private boolean justUpdated = false;
    private void updateSelectedDescriptor() {
        System.out.println("Updating descriptor " + lastSelected);
        int index = lastSelected;
        if (index < 0 || index >= descriptors.size()) return;
        descriptors.set(index, new SourceDescriptor(
                Utils.radians(theta.get()), Utils.radians(phi.get()), freq.get(), ampl.get(), dist.get()));
        justUpdated = true;
        list.setListData(new Vector<>(descriptors));
        fireListeners();
    }

    private void fireListeners() {
        List<Speaker> speakers = descriptors.stream().map(SourceDescriptor::getSpeaker).toList();
        for (SpeakerListener sp : speakerListeners) {
            sp.speakersUpdated(speakers);
        }
    }

    public void addSpeakerListener(SpeakerListener listener) {
        speakerListeners.add(listener);
        listener.speakersUpdated(descriptors.stream().map(SourceDescriptor::getSpeaker).toList());
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        if (justUpdated) {
            justUpdated = false;
            return;
        }
        int index = list.getSelectedIndex();
        if (index == -1) return;
        SourceDescriptor desc = descriptors.get(index);
        theta.setValue(desc.theta);
        phi.setValue(desc.phi);
        dist.setValue(desc.dist);
        freq.setValue(desc.freq);
        ampl.setValue(desc.ampl);
        lastSelected = index;
    }

}
