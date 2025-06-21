package acousticeyes.simulation;

import acousticeyes.util.Utils;
import acousticeyes.util.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static PhasedArray radial(int rings, int spokes, double minR, double maxR, double exp, boolean useExp, double spiral, double posNoise) {
        List<Vec3> pos = new ArrayList<>();
        double maxexp = Math.exp(exp) - 1;
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

    public void simulate(Simulator sim, int ns) {
        for (Microphone m : mics) {
            sim.simulate(m, ns);
        }
    }

    // assumes array is looking down Z axis; theta is azimuth, phi is altitude angle, centered on 0
    public double[] farFieldBeamformingDelays(double theta, double phi) {
        double[] delays = new double[n];
        Vec3 aim = new Vec3(0,0,1).rotX(phi).rotY(theta);
        for (int i=0; i < delays.length; i++) {
            Vec3 dv = mics.get(i).pos.sub(center);
            if (dv.mag() < 1e-6) {
                delays[i] = 0;
            } else {
                delays[i] = dv.dot(aim) / Simulator.SPEED_OF_SOUND;
            }
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

    public double[] delayAndSum(double[] delays, double startTime, int samples) {
        double[] sum = new double[samples];
        for (int i=0; i < samples; i++) {
            for (int mi = 0; mi < mics.size(); mi++) {
                sum[i] += mics.get(mi).sampleRecording(startTime + delays[mi] + i/Simulator.SPS);
            }
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

    public double[] delayAndSumFreqDomain(double[][] spectra, double[] delays, double startTime, int samples) {
        double[] sum = new double[samples];
        for (int i=0; i < samples; i+=2) {
            for (int mi = 0; mi < mics.size(); mi++) {
                sum[i] += mics.get(mi).sampleRecording(startTime + delays[mi] + i/Simulator.SPS);
            }
        }
        return sum;

    }

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
                res[i][j] = Utils.rms(delayAndSumFreqDomain(spectra, farFieldBeamformingDelays(theta, phi), startTime, samples));
            }
        }
        return res;
    }

}
