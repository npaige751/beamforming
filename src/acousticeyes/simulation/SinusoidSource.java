package acousticeyes.simulation;

public class SinusoidSource implements SoundSource {

    private double f, a, phi;

    public SinusoidSource(double freq, double amp, double phase) {
        f = freq;
        a = amp;
        phi = phase;
    }

    @Override
    public double get(double t) {
        return a * Math.sin(2*Math.PI*f*t + phi);
    }
}
