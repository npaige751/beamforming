package acousticeyes.simulation;

import acousticeyes.util.Vec3;

public class DAMAS {

    private int n;
    private int N;
    private double fov;
    private PhasedArray arr;
    private double freq;

    private double[][] A;

    public DAMAS(PhasedArray arr, double freq, int n, double fov) {
        this.n = n;
        this.N = n*n;
        this.arr = arr;
        this.freq = freq;
        this.fov = fov;
    }

    public void computeArrayResponseMatrix() {
        A = new double[N][N];
        for (int i = 0; i < n; i++) {
            double theta = fov * i / (n - 1.0) - (fov / 2);
            for (int j = 0; j < n; j++) {
                double phi = fov * j / (n - 1.0) - (fov / 2);
                Vec3 p = new Vec3(0, 0, 1).rotX(phi).rotY(theta).mul(10);
                Simulator sim = new Simulator(new Speaker(p, new SinusoidSource(freq, 1, 0)));
                double[][] resp = arr.sweepBeam(-fov / 2, fov / 2, n, -fov / 2, fov / 2, n, 2000 / Simulator.SPS, 100);
                for (int a = 0; a < n; a++) {
                    for (int b = 0; b < n; b++) {
                        A[i * n + j][a * n + b] = resp[a][b];
                    }
                }
            }
        }
    }

    private static double[] flatten(double[][] x) {
        double[] res = new double[x.length * x[0].length];
        for (int i=0; i < x.length; i++) {
            for (int j=0; j < x[0].length; j++) {
                res[i * x[0].length + j] = x[i][j];
            }
        }
        return res;
    }

    private static double[][] unflatten(double[] x, int n) {
        double[][] res = new double[n][n];
        for (int i=0; i < n; i++) {
            for (int j=0; j < n; j++) {
                res[i][j] = x[i*n + j];
            }
        }
        return res;
    }

    public double[][] deconvolve(double[][] heatmap, int iters) {
        double[] Y = flatten(heatmap);
        double[] X = new double[N];
        for (int iter = 0; iter < iters; iter++) {
            for (int i = 0; i < N; i++) {
                X[i] = Y[i];
                for (int j = 0; j < N; j++) {
                    if (i == j) continue;
                    X[i] -= A[i][j] * X[j];
                }
                if (X[i] < 0) X[i] = 0;
            }
        }
        return unflatten(X, n);
    }
}
