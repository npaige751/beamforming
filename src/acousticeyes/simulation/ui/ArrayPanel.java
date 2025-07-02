package acousticeyes.simulation.ui;

import acousticeyes.simulation.PhasedArray;
import acousticeyes.simulation.Microphone;
import acousticeyes.util.Vec3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

/* UI for adjusting parameters of the phased array and displaying a graphical representation
 * of the arrangement of microphones.
 */
public class ArrayPanel extends JPanel implements ItemListener {
    private static final int PARAM_HT = 40;
    private static final int PAD = 5;

    public enum Mode { GRID, RADIAL }

    private Parameter xsize, ysize, rmin, rmax, rexp, spiral, posNoise, micNoise, rows, cols, rings, spokes;
    private Mode mode = Mode.RADIAL;
    private JComboBox<Mode> modeSelector;
    private ArrayLayoutLabel layout;
    private List<ArrayListener> arrayListeners = new ArrayList<>();

    private PhasedArray phasedArray;

    public ArrayPanel(int width, int height) {
        setSize(width, height);
        setLayout(null);
        xsize = new Parameter("X Size", 1.0, 0.01, 10, true, false, width, PARAM_HT, (x) -> updateGrid());
        ysize = new Parameter("Y Size", 1.0, 0.01, 10, true, false, width, PARAM_HT, (x) -> updateGrid());
        rmin = new Parameter("R Min", 0.05, 0.01, 10, true, false, width, PARAM_HT, (x) -> updateGrid());
        rmax = new Parameter("R Max", 0.3, 0.01, 10, true, false, width, PARAM_HT, (x) -> updateGrid());
        rexp = new Parameter("R Exp", 1.0, 0.1, 10, true, false, width, PARAM_HT, (x) -> updateGrid());
        spiral = new Parameter("Spiral", 0, 0, 1, false, false, width, PARAM_HT, (x) -> updateGrid());
        posNoise = new Parameter("Position Noise", 0.001, 0.0001, 1, true, false, width, PARAM_HT, (x) -> updateGrid());
        micNoise = new Parameter("Mic Noise (dB)", -60, -60, 10, false, false, width, PARAM_HT, (x) -> updateGrid());
        rows = new Parameter("Rows", 4, 1, 16, false, true, width, PARAM_HT, (x) -> updateGrid());
        cols = new Parameter("Cols", 4, 1, 16, false, true, width, PARAM_HT, (x) -> updateGrid());
        rings = new Parameter("Rings", 6, 1, 16, false, true, width, PARAM_HT, (x) -> updateGrid());
        spokes = new Parameter("Spokes", 16, 1, 32, false, true, width, PARAM_HT, (x) -> updateGrid());

        modeSelector = new JComboBox<>(Mode.values());
        modeSelector.setBounds(0,0,width,PARAM_HT);
        modeSelector.setSelectedItem(Mode.RADIAL);
        modeSelector.addItemListener(this);

        layout = new ArrayLayoutLabel(2.0, 2.0);

        add(modeSelector);
        add(rings);
        add(spokes);
        add(rmin);
        add(rmax);
        add(rexp);
        add(spiral);
        add(posNoise);
        add(micNoise);
        add(layout);
        // some of these have the same coordinates. not all are visible simultaneously, depending on the grid/radial mode
        rows.setBounds(PAD, PARAM_HT, width, PARAM_HT);
        cols.setBounds(PAD, PARAM_HT * 2, width, PARAM_HT);
        xsize.setBounds(PAD, PARAM_HT * 3, width, PARAM_HT);
        ysize.setBounds(PAD, PARAM_HT * 4, width, PARAM_HT);
        rmin.setBounds(PAD, PARAM_HT * 3, width, PARAM_HT);
        rmax.setBounds(PAD, PARAM_HT * 4, width, PARAM_HT);
        rexp.setBounds(PAD, PARAM_HT * 5, width, PARAM_HT);
        spiral.setBounds(PAD, PARAM_HT * 6, width, PARAM_HT);
        rings.setBounds(PAD, PARAM_HT, width, PARAM_HT);
        spokes.setBounds(PAD, PARAM_HT * 2, width, PARAM_HT);
        posNoise.setBounds(PAD, PARAM_HT * 7, width, PARAM_HT);
        micNoise.setBounds(PAD, PARAM_HT * 8, width, PARAM_HT);
        layout.setBounds(PAD, PARAM_HT * 9, width, width);

        phasedArray = constructArray();
        addArrayListener(layout);
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
        setMode((Mode) modeSelector.getSelectedItem());
    }

    // some parameters only apply in grid or radial mode. switch which ones are visible as appropriate
    private void setMode(Mode mode) {
        if (mode == this.mode) return;
        this.mode = mode;
        if (mode == Mode.RADIAL) {
            remove(xsize);
            remove(ysize);
            remove(rows);
            remove(cols);
            add(rmin);
            add(rmax);
            add(rexp);
            add(spiral);
            add(rings);
            add(spokes);
        } else if (mode == Mode.GRID) {
            add(xsize);
            add(ysize);
            add(rows);
            add(cols);
            remove(rmin);
            remove(rmax);
            remove(rexp);
            remove(spiral);
            remove(rings);
            remove(spokes);
        }
        updateGrid();
    }

    // build a PhasedArray based on the current values of the parameters
    private PhasedArray constructArray() {
        switch(mode) {
            case GRID:
                return PhasedArray.grid(
                        (int) rows.get(),
                        (int) cols.get(),
                        new Vec3(0,0,0),
                        new Vec3(xsize.get()/rows.get(), 0, 0),
                        new Vec3(0, ysize.get()/cols.get(), 0),
                        posNoise.get()
                );
            case RADIAL:
                return PhasedArray.radial(
                        (int) rings.get(),
                        (int) spokes.get(),
                        rmin.get(),
                        rmax.get(),
                        rexp.get(),
                        spiral.get(),
                        posNoise.get()
                );
            default:
                throw new IllegalStateException("Unsupported mode: " + mode);
        }
    }

    public void addArrayListener(ArrayListener listener) {
        arrayListeners.add(listener);
        listener.phasedArrayUpdated(phasedArray);
    }

    public void updateGrid() {
        phasedArray = constructArray();
        for (ArrayListener listener : arrayListeners) {
            listener.phasedArrayUpdated(phasedArray);
        }
        repaint();
    }

    // visual representation of the array layout
    private static class ArrayLayoutLabel extends JLabel implements ArrayListener {
        private double xs, ys;
        private PhasedArray arr;

        ArrayLayoutLabel(double xs, double ys) {
            this.xs = xs;
            this.ys = ys;
        }

        @Override
        public void paintComponent(Graphics g) {
            g.setColor(Color.WHITE);
            g.fillRect(0,0,getWidth(),getHeight());
            g.setColor(Color.BLUE);
            int r = 4;
            if (arr == null) return;
            for (Microphone m : arr.mics) {
                int x = (int) (((m.pos.x / xs) + 0.5) * getWidth());
                int y = (int) (((m.pos.y / ys) + 0.5) * getWidth());
                g.fillRect(x - r, y - r, 2*r, 2*r);
            }
        }

        @Override
        public void phasedArrayUpdated(PhasedArray array) {
            arr = array;
            repaint();
        }
    }
}
