package acousticeyes.simulation;

import acousticeyes.util.Utils;
import acousticeyes.util.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* Represents an array of microphones and implements delay-and-sum beamforming */
public class PhasedArray {
    public List<Microphone> mics = new ArrayList<>();
    private Vec3 center;
    private int n;

    public PhasedArray(List<Vec3> locations, double posNoise) {
        n = locations.size();
        center = new Vec3(0,0,0);
        for (int i=0; i < n; i++) {
            mics.add(new Microphone(locations.get(i), new Vec3(posNoise, posNoise, posNoise)));
            center = center.add(locations.get(i));
        }
        center = center.mul(1.0/n);
    }

    public static PhasedArray grid(int n1, int n2, Vec3 c, Vec3 d1, Vec3 d2, double posNoise) {
        List<Vec3> pos = new ArrayList<>();
        for (int i=0; i < n1; i++) {
            for (int j=0; j < n2; j++) {
                pos.add(c.add(d1.mul(i - (n1-1)/2.0)).add(d2.mul(j - (n2-1)/2.0)));
            }
        }
        return new PhasedArray(pos, posNoise);
    }

    /* Creates a radial/spiral array.
     *   useExp - if true, use exponential spacing (based on 'exp') in radius; otherwise, linear spacing, 'exp' has no effect
     *   exp - The radius function is a shifted version of e^(x * exp) where x ranges from 0 to 1 across the range of radii.
     *         Higher values give more extreme skew towards smaller rings.
     *   spiral - ranges from 0 (straight spokes) to 1 (maximum spiralization). Larger values would create mirror versions of less spiraly arrangements.
     *   posNoise - standard deviation for gaussian noise modeling error in actual vs. theoretical microphone positions (same stddev on all axes, for simplicity)
     */
    public static PhasedArray radial(int rings, int spokes, double minR, double maxR, double exp, boolean useExp, double spiral, double posNoise) {
        List<Vec3> pos = new ArrayList<>();
        double maxexp = Math.exp(exp) - 1;
        spiral *= (rings-1)*0.5;
        for (int r = 0; r < rings; r++) {
            double radFrac = r / (rings - 1.0);
            double rad;
            if (useExp) {
                radFrac = Math.exp(radFrac * exp) - 1;
                rad = minR + (maxR - minR) * (radFrac / maxexp);
            } else {
                rad = minR + (maxR - minR) * radFrac;
            }
            for (int s = 0; s < spokes; s++) {
                double theta = 2 * Math.PI * (s + spiral*radFrac) / spokes;
                pos.add(new Vec3(rad * Math.cos(theta), rad * Math.sin(theta), 0.0));
            }
        }
        return new PhasedArray(pos, posNoise);
    }

    // apply simulation to all microphones in the array. this populates their recordings.
    public void simulate(Simulator sim, int ns) {
        for (Microphone m : mics) {
            sim.simulate(m, ns);
        }
    }

    // calculates the relative delays between microphones for sound arriving from (theta, phi)
    // assumes array is looking down Z axis; theta is azimuth, phi is altitude angle, centered on 0
    public double[] farFieldBeamformingDelays(double theta, double phi) {
        double[] delays = new double[n];
        Vec3 aim = new Vec3(0,0,1).rotX(phi).rotY(theta);
        for (int i=0; i < delays.length; i++) {
            // center is an arbitrary reference point set to 0 relative delay; any point in the array plane could be used.
            // the delay time is the length of the projection of the vector from the center to the microphone onto the
            // incoming sound direction (beam aim vector) times the speed of sound.
            Vec3 dv = mics.get(i).pos.sub(center);
            delays[i] = dv.dot(aim) / Simulator.SPEED_OF_SOUND;
        }
        return normalizeDelays(delays);
    }

    // shift so minimum delay is 0 and all delays are positive
    public double[] normalizeDelays(double[] delays) {
        double[] res = new double[delays.length];
        double min = Utils.min(delays);
        for (int i=0; i < delays.length; i++) {
            res[i] = delays[i] - min;
        }
        return res;
    }

    // shifts the microphone recordings according to the specified delay times and sums them.
    // startTime - can be used to skip to a point where all of the microphones have started receiving sound
    // samples - the number of samples over which to perform the summation
    public double[] delayAndSum(double[] delays, double startTime, int samples) {
        double[] sum = new double[samples];
        for (int i=0; i < samples; i++) {
            for (int mi = 0; mi < mics.size(); mi++) {
                sum[i] += mics.get(mi).sampleRecording(startTime + delays[mi] + i/Simulator.SPS);
            }
            sum[i] /= mics.size();
        }
        return sum;
    }

    public double[][] computeSpectra(double startTime, int samples) {
        double[][] spectra = new double[n][samples];
        int startSample = (int) (startTime * Simulator.SPS);
        for (int i=0; i < n; i++) {
            spectra[i] = Utils.dft(Arrays.copyOfRange(mics.get(i).recording, startSample, startSample + samples));
        }
        return spectra;
    }

    // frequency domain DAS beamforming: delays are implemented as phase shifts for each frequency bin independently
    public double[] delayAndSumFreqDomain(double[][] spectra, double[] delays, int samples) {
        double[] sum = new double[samples];
        double freqStep = Simulator.SPS / samples;
        for (int mi = 0; mi < mics.size(); mi++) {
            double phaseDelayBase = delays[mi] * 2 * Math.PI * freqStep;
            double psr = Math.cos(phaseDelayBase);
            double psi = Math.sin(phaseDelayBase);
            double pr = psr;
            double pi = psi;
            for (int i=2; i < samples; i += 2) {
                double freq = i/2 * freqStep;
                if (freq > 10000) break;

                double sr = spectra[mi][i];
                double si = spectra[mi][i+1];
                double rotr = sr * pr - si * pi;
                double roti = sr * pi + si * pr;
                sum[i] += rotr;
                sum[i+1] += roti;

                // increment phasor by phaseDelayBase using angle summation identities
                // this avoids evaluating trig functions in the inner loop (2x faster)
                double newpr = pr * psr - pi * psi;
                pi = pi * psr + pr * psi;
                pr = newpr;
            }
        }
        double[] mag = new double[samples/2];
        for (int i=0; i < mag.length; i++) {
            mag[i] = Math.sqrt(sum[2*i] * sum[2*i] + sum[2*i+1] * sum[2*i+1]);
        }
        return mag;
    }

    // Run DAS beamforming on a grid of points. Returns a 2D array with the RMS amplitude of the
    // delayed-and-summed waveforms at each beam direction.
    public double[][] sweepBeam(double thetaStart, double thetaEnd, int thetaSteps, double phiStart, double phiEnd, int phiSteps, double startTime, int samples) {
        double[][] res = new double[thetaSteps][phiSteps];
        for (int i=0; i < thetaSteps; i++) {
            double theta = thetaStart + ((thetaEnd - thetaStart)*i)/(thetaSteps-1);
            for (int j=0; j < phiSteps; j++) {
                double phi = phiStart + ((phiEnd - phiStart)*j)/(phiSteps-1);
                res[i][j] = Utils.rms(delayAndSum(farFieldBeamformingDelays(theta, phi), startTime, samples));
            }
        }
        return res;
    }

    public double[][] sweepBeamFreqDomain(double thetaStart, double thetaEnd, int thetaSteps, double phiStart, double phiEnd, int phiSteps, double startTime, int samples) {
        double[][] spectra = computeSpectra(startTime, samples);
        double[][] res = new double[thetaSteps][phiSteps];
        for (int i=0; i < thetaSteps; i++) {
            double theta = thetaStart + ((thetaEnd - thetaStart)*i)/(thetaSteps-1);
            for (int j=0; j < phiSteps; j++) {
                double phi = phiStart + ((phiEnd - phiStart)*j)/(phiSteps-1);
                res[i][j] = Utils.sum(delayAndSumFreqDomain(spectra, farFieldBeamformingDelays(theta, phi), samples));
            }
        }
        return res;
    }

}
