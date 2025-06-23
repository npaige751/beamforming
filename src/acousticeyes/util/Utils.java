package acousticeyes.util;

public class Utils {

    // linearly map from [sx1, sx2] -> [dx1, dx2]
    public static double lerp(double sx1, double sx2, double dx1, double dx2, double x) {
        double t = (x - sx1) / (sx2 - sx1);
        return dx1 + (dx2 - dx1) * t;
    }

    // linearly interpolate adjacent samples in xs to approximate value at non-integer sample index t
    public static double lerpSample(double[] xs, double t) {
        if (t < 0) return 0;
        if (t+1 >= xs.length) return 0;
        int s = (int) t;
        double d = t - s;
        return xs[s] * (1-d) + xs[s+1] * d;
    }

    // calculate root mean square amplitude of x
    public static double rms(double[] x) {
        double s = 0;
        for (int i=0; i < x.length; i++) {
            s += x[i] * x[i];
        }
        s /= x.length;
        return Math.sqrt(s);
    }

    public static double radians(double degrees) {
        return degrees * Math.PI / 180.0;
    }

    public static double degrees(double radians) {
        return radians * 180.0 / Math.PI;
    }

    public static double db(double linearPower) {
        return 10 * Math.log10(linearPower);
    }

    public static double min(double[] x) {
        double m = Double.MAX_VALUE;
        for (int i=0; i < x.length; i++) {
            if (x[i] < m) m = x[i];
        }
        return m;
    }

    // compute discrete Fourier transform of x
    // returns an array of twice the length of x, where adjacent entries are the real and imaginary components of the DFT
    public static double[] dft(double[] x) {
        int n = x.length;
        double[] res = new double[n];
        for (int k = 0; k < n/2; k++) {
            double csum = 0;
            double ssum = 0;
            for (int t = 0; t < n; t++) {
                csum += Math.cos(2*Math.PI * t * k / n) * x[t];
                ssum += Math.sin(2*Math.PI * t * k / n) * x[t];
            }
            res[k*2] = csum / n; // sqrt(n) for symmetric variant
            res[k*2 + 1] = ssum / n;
        }
        return res;
    }

}
