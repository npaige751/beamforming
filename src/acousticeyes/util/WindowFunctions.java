package acousticeyes.util;

import java.util.Arrays;

public class WindowFunctions {

    public static double[] rectangularWindow(int samples) {
        double[] w = new double[samples];
        Arrays.fill(w, 1.0);
        return w;
    }

    private static double[] raisedCosineWindow(int samples, double alpha0) {
        double[] w = new double[samples];
        for (int i = 0; i < samples; i++) {
            w[i] = alpha0 - (1 - alpha0) * Math.cos(2 * Math.PI * i / (samples - 1.0));
        }
        return w;
    }

    public static double[] hammingWindow(int samples) {
        return raisedCosineWindow(samples, 25/64.0);
    }

    public static double[] hannWindow(int samples) {
        return raisedCosineWindow(samples, 0.5);
    }

    private static double[] cosineSumWindow(int samples, double[] alphas) {
        double[] w = new double[samples];
        for (int i = 0; i < samples; i++) {
            for (int k = 0; k < alphas.length; k++) {
                w[i] += (k % 2 == 0 ? 1 : -1) * alphas[k] * Math.cos(2 * k * Math.PI * i / (samples - 1.0));
            }
        }
        return w;
    }

    public static double[] nutallWindow(int samples) {
        return cosineSumWindow(samples, new double[]{ 0.355768, 0.487396, 0.144232, 0.012604 });
    }

    public static double[] blackmanHarrisWindow(int samples) {
        return cosineSumWindow(samples, new double[]{ 0.35875, 0.48829, 0.14128, 0.01168 });
    }

    public static double[] flatTopWindow(int samples) {
        return cosineSumWindow(samples, new double[]{ 0.215578, 0.416631, 0.27726, 0.083579, 0.0069474 });
    }
}
