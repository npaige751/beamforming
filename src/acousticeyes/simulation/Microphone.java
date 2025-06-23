package acousticeyes.simulation;

import acousticeyes.util.Utils;
import acousticeyes.util.Vec3;

public class Microphone {
    public Vec3 pos; // theoretical position - beamforming assumes the mic is here
    public Vec3 noisyPos; // actual position - used for simulating microphone input
    public double[] recording; // stores simulated pressure samples from most recent simulation run

    public Microphone(Vec3 p) {
        pos = p;
        noisyPos = p;
    }

    // noise describes the standard deviation of Gaussian noise to be added to the position
    public Microphone(Vec3 p, Vec3 noise) {
        pos = p;
        noisyPos = p.add(new Vec3(sampleGaussian(noise.x), sampleGaussian(noise.y), sampleGaussian(noise.z)));
    }

    // fast/simple approximation to sampling a Gaussian distribution by summing 12 uniformly [0,1] variables
    private double sampleGaussian(double sigma) {
        double s = 0;
        for (int i=0; i < 12; i++) {
            s += Math.random();
        }
        s -= 6;
        return s * sigma;
    }

    // interpolates samples to find approximate pressure level at time t (in seconds)
    public double sampleRecording(double t) {
        return Utils.lerpSample(recording, t * Simulator.SPS);
    }

    // adds white noise to recording
    public void addNoise(double db) {
        if (recording == null) return;
        double ampl = Math.pow(10, db/10);
        for (int i=0; i < recording.length; i++) {
            recording[i] += Math.random() * ampl;
        }
    }
}
