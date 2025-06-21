package acousticeyes.simulation;

public class SquareWaveSource implements SoundSource {

    private double freq, ampl;

    public SquareWaveSource(double f, double a) {
        freq = f;
        ampl = a;
    }

    @Override
    public double get(double t) {
        return (freq * t) % 1 > 0.5 ? ampl : -ampl;
    }
}
