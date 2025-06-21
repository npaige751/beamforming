package acousticeyes.simulation;

import acousticeyes.util.Utils;

public class NoiseSource implements SoundSource{

    private double[] samples;

    public NoiseSource(int ns, double amp) {
        samples = new double[ns];
        for (int i=0; i < ns; i++) {
            samples[i] = Math.random() * amp;
        }
    }

    @Override
    public double get(double t) {
        return Utils.lerpSample(samples, t * Simulator.SPS);
    }
}
