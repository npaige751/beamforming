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

    public static double sum(double[] x) {
        double s = 0;
        for (int i=0; i < x.length; i++) {
            s += x[i];
        }
        return s;
    }

    public static double[] product(double[] x, double[] y) {
        double[] res = new double[x.length];
        for (int i=0; i < res.length; i++) {
            res[i] = x[i] * y[i];
        }
        return res;
    }

    // compute discrete Fourier transform of x
    // returns an array where adjacent entries are the real and imaginary components of the DFT
    public static double[] dft(double[] x, int start, int end, int stride) {
        int n = (end - start) / stride;
        double[] res = new double[n];
        for (int k = 0; k < n/2; k++) {
            double csum = 0;
            double ssum = 0;
            for (int t = 0; t < n; t++) {
                csum += Math.cos(2*Math.PI * t * k / n) * x[start + stride*t];
                ssum += Math.sin(2*Math.PI * t * k / n) * x[start + stride*t];
            }
            res[k*2] = csum / n; // sqrt(n) for symmetric variant
            res[k*2 + 1] = ssum / n;
        }
        return res;
    }

    public static double[] dft(double[] x, double[] window) {
        return dft(product(x, window), 0, x.length, 1);
    }

    public static Complex[] fft(double[] x, int start, double[] window) {
        int len = window.length;
        if ((len & (len - 1)) != 0) throw new IllegalArgumentException("fft input must have power-of-2 size");
        Complex[] cx = new Complex[len];
        for (int i=0; i < len; i++) {
            cx[i] = new Complex(x[start + i] * window[i], 0);
        }
        return fft(cx);
    }

    private static int reverseBits(int x, int bits) {
        int res = 0;
        int mask = 1 << (bits - 1);
        for (int b = 0; b < bits; b++) {
            if ((x & 1) == 1) {
                res |= mask;
            }
            x >>= 1;
            mask >>= 1;
        }
        return res;
    }

    private static int fftCacheBits = -1;
    private static int fftCacheSize = -1;
    private static Complex[] twiddleCache;
    private static int[] bitReversalCache;

    private static void updateTwiddleCache(int bits) {
        if (fftCacheBits == bits) return;
        fftCacheBits = bits;
        fftCacheSize = 1 << (bits-1);
        twiddleCache = new Complex[fftCacheSize];
        bitReversalCache = new int[fftCacheSize*2];
        for (int i = 0; i < fftCacheSize; i++) {
            twiddleCache[i] = Complex.expi(-2*Math.PI*i/(fftCacheSize*2));
        }
        for (int i = 0; i < bitReversalCache.length; i++) {
            bitReversalCache[i] = reverseBits(i, fftCacheBits);
        }
    }

    public static Complex[] fft(Complex[] x) {
        int N = x.length;
        int bits = Integer.numberOfTrailingZeros(N);
        updateTwiddleCache(bits);
        Complex[] out = new Complex[N];
        Complex[] res = new Complex[N];

        // first reorder inputs with a bit-reversal permutation
        for (int i = 0; i < N; i ++) {
            int rev = bitReversalCache[i];//reverseBits(i, bits);
            out[i] = x[rev]; // equivalently, out[rev] = x[i], since bit reversal is an involution
            res[i] = new Complex(0,0);
        }

        int S = 2;
        Complex tmp = new Complex(0,0);
        while (S <= N) {
            int halfS = S >> 1;
            for (int i = 0; i < N; i += S) {
                for (int j = 0; j < halfS; j++) {
                    Complex tw = twiddleCache[j * N / S];
                    int idx = i + j;
                    Complex.mul(tmp, tw, out[idx + halfS]);
                    Complex.add(res[idx], out[idx], tmp);
                    Complex.sub(res[idx + halfS], out[idx], tmp);
                }
            }
            Complex[] temp = res;
            res = out;
            out = temp;
            S *= 2;
        }
        return out;
    }

}
