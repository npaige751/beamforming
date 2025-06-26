package acousticeyes.simulation;

import java.util.Arrays;

/* Represents a scheme for using different parts of a phased array for differet frequency bands. */
public class Subarrays {

    private double[] frequencyBands; // band i goes from frequencyBands[i] to frequencyBands[i+1]
    private double[][] weights; // indexed by band and then by microphone number

    public Subarrays(double[] bands, double[][] w) {
        frequencyBands = bands;
        weights = w;
    }

    public double getWeight(int mic, double freq) {
        if (freq < frequencyBands[0]) return 0;
        if (freq >= frequencyBands[frequencyBands.length-1]) return 0;
        int band = 1;
        while (band < frequencyBands.length && frequencyBands[band] < freq) {
            band++;
        }
        return weights[band-1][mic];
    }

    public double maxFrequency() {
        return frequencyBands[frequencyBands.length-1];
    }

    public static class Band {
        public int minRing, maxRing;
        public double maxFreq;

        public Band(double maxFreq, int minRing, int maxRing) {
            this.maxFreq = maxFreq;
            this.maxRing = maxRing;
            this.minRing = minRing;
        }
    }

    public static Subarrays trivial(double minFreq, double maxFreq, int n) {
        double[] w = new double[n];
        Arrays.fill(w, 1.0);
        return new Subarrays(new double[] {minFreq, maxFreq}, new double[][]{w});
    }

    public static Subarrays forRadial(double minFreq, int rings, int spokes, Band... bands) {
        double[] freqs = new double[bands.length + 1];
        double[][] weights = new double[bands.length][rings * spokes];
        freqs[0] = minFreq;
        for (int b = 0; b < bands.length; b++) {
            weights[b] = weightsForRings(bands[b].minRing, bands[b].maxRing, rings * spokes, spokes);
            freqs[b+1] = bands[b].maxFreq;
            if (freqs[b+1] <= freqs[b]) throw new IllegalArgumentException("Bands must be specified in increasing frequency order");
        }
        return new Subarrays(freqs, weights);
    }

    private static double[] weightsForRings(int minRing, int maxRing, int n, int spokes) {
        double[] w = new double[n];
        for (int i = minRing * spokes; i < maxRing * spokes; i++) {
            w[i] = 1;
        }
        return w;
    }

}
