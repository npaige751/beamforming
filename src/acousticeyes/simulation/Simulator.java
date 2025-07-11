package acousticeyes.simulation;

import acousticeyes.beamforming.Microphone;
import acousticeyes.beamforming.PhasedArray;
import acousticeyes.util.ColorMap;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/* Represents a collection of speakers and their associated sound sources.
 * Handles simulation of sound propagation from speakers to microphones.
 * Provides rendering of beamforming heatmap results.
 */
public class Simulator {
    public List<Speaker> speakers;
    public static final double SPS = 48000; // sampling rate, Hz
    public static final double SPEED_OF_SOUND = 340.0; // m/s

    public Simulator(Speaker... speakers) {
        this.speakers = Arrays.stream(speakers).toList();
    }

    public Simulator(List<Speaker> speakers) {
        this.speakers = speakers;
    }

    // Populates 'recording' field in m with pressure samples simulated based on
    // the speakers and their sound sources. Delays and attenuation due to distance
    // are accounted for. The medium is assumed to be lossless, stationary and uniform,
    // and the environment empty (i.e, no reflections).
    // "noisy" actual positions of microphones are used to calculate delays, while the
    // beamforming will use the theoretical positions.
    public void simulate(Microphone m, int ns) {
        double[] samples = new double[ns];
        for (Speaker sp : speakers) {
            double dist = sp.pos.distance(m.noisyPos);
            double delay = dist / SPEED_OF_SOUND;
            double attenuation = 1.0 / (dist * dist); // 1-meter reference level
            int delaySamples = (int) (delay * SPS);
            double fractionalSampleDelay = ((delay * SPS) - delaySamples) / SPS;
            for (int s = 0; s < ns - delaySamples; s++) {
                double t = s / SPS;
                samples[s + delaySamples] += sp.source.get(t - fractionalSampleDelay) * attenuation;
            }
        }
        m.recording = samples;
    }

    // simulate, then do beamforming
    public double[][] scan2d(PhasedArray arr, int xs, int ys, double thetaStart, double thetaEnd, double phiStart, double phiEnd) {
        arr.simulate(this, 5000);
        double[][] spectra = arr.computeSpectra(2000 / SPS, 100);
        return arr.sweepBeamFreqDomain(spectra, thetaStart, thetaEnd, xs, phiStart, phiEnd, ys);
    }
}
