package acousticeyes.simulation.ui;

import acousticeyes.util.Utils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.function.Consumer;

/* Wrapper for UI components and logic pertaining to adjusting the value of some parameter.
 *

 */
public class Parameter extends JPanel implements ChangeListener {
    private double value;
    private double min, max;
    private boolean logarithmic; // whether to use logarithmic or linear interpolation between min and max values
    private boolean integer; // whether to round values to integers
    public String name;

    private JLabel label; // displays parameter name and current value
    private JSlider slider; // slider for adjusting parameter value
    private Consumer<Double> changeCallback; // called whenever the slider is moved

    public static final int SLIDER_WD = 250;
    public static final Font FONT = new Font("Ubuntu Mono", Font.PLAIN, 16);

    public Parameter(String name, double defaultValue, double minValue, double maxValue, boolean logarithmic, boolean integer, int width, int height, Consumer<Double> callback) {
        value = defaultValue;
        min = minValue;
        max = maxValue;
        this.logarithmic = logarithmic;
        this.integer = integer;
        this.name = name;
        label = new JLabel(name + ": " + value);
        label.setFont(FONT);
        label.setBounds(0,0,width - SLIDER_WD, height);
        slider = new JSlider(0, SLIDER_WD, valueToPos(defaultValue));
        slider.setBounds(width - SLIDER_WD, 0, SLIDER_WD, height);
        slider.addChangeListener(this);
        setLayout(null);
        add(label);
        add(slider);
        changeCallback = callback;
    }

    public double get() {
        return value;
    }

    public void setValue(double value) {
        value = Math.min(max, Math.max(min, value));
        label.setText(name + ": " + value);
        slider.removeChangeListener(this);
        slider.setValue(valueToPos(value));
        slider.addChangeListener(this);
    }

    // convert a parameter value to a slider position
    private int valueToPos(double value) {
        if (logarithmic) {
            return (int) Utils.lerp(Math.log(min), Math.log(max), 0, SLIDER_WD, Math.log(value));
        } else {
            return (int) Utils.lerp(min, max, 0, SLIDER_WD, value);
        }
    }

    // convert slider position to a parameter value
    private double posToValue(int pos) {
        double v = 0;
        if (logarithmic) {
            v = Math.exp(Utils.lerp(0, SLIDER_WD, Math.log(min), Math.log(max), pos));
        } else {
            v = Utils.lerp(0, SLIDER_WD, min, max, pos);
        }
        if (integer) {
            v = (int) v;
        }
        return v;
    }

    @Override
    public void stateChanged(ChangeEvent changeEvent) {
        value = posToValue(slider.getValue());
        label.setText(name + ": " + value);
        changeCallback.accept(value);
    }
}
