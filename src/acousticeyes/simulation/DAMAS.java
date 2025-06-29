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

    public void updateAndRecomputeArrayIfNeeded(PhasedArray arr, double freq, int n, double fov) {
        if (this.arr.equals(arr) && this.freq == freq && this.n == n && this.fov == fov) return;
        this.arr = arr;
        this.freq = freq;
        this.n = n;
        this.fov = fov;
        this.N = n*n;
        computeArrayResponseMatrix();
    }

    public void computeArrayResponseMatrix() {
        A = new double[N][N];
        Vec3[][] dirs = new Vec3[n][n];
        double timeToAngularFreq = freq * 2 * Math.PI / Simulator.SPEED_OF_SOUND;
        // precompute pointing directions
        for (int i=0; i < n; i++) {
            double theta = fov * i / (n - 1.0) - (fov / 2);
            for (int j=0; j < n; j++) {
                double phi = fov * j / (n - 1.0) - (fov / 2);
                dirs[i][j] = new Vec3(0, 0, 1).rotX(phi).rotY(theta);
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Vec3 v = dirs[i][j];
                double[][] resp = new double[n][n];
                for (int a = 0; a < n; a++) {
                    for (int b = 0; b < n; b++) {
                        Vec3 aim = dirs[a][b];
                        double real = 0;
                        double imag = 0;
                        for (int mi = 0; mi < arr.mics.size(); mi++) {
                            Vec3 dv = arr.mics.get(mi).pos.sub(arr.getCenter());
                            double phaseDiff = (dv.dot(aim) - dv.dot(v)) * timeToAngularFreq;
                            real += Math.cos(phaseDiff);
                            imag += Math.sin(phaseDiff);
                        }
                        resp[a][b] = Math.sqrt(real * real + imag * imag);
                    }
                }

                for (int a = 0; a < n; a++) {
                    for (int b = 0; b < n; b++) {
                        A[i * n + j][a * n + b] = resp[a][b] / resp[i][j]; // normalize so that the array response is 1 for incoming sound aligned with the beam aim direction
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
        double totAdj = 0;
        int iter = 0;
        while (iter++ < iters) {
            totAdj = 0;
            int istart = iter % 2 == 0 ? 0 : N-1;
            int iend   = iter % 2 == 0 ? N : -1;
            int istep  = iter % 2 == 0 ? 1 : -1;

            for (int i = istart; i != iend; i += istep) {
                double prevx = X[i];
                X[i] = Y[i];
                for (int j = 0; j < N; j++) {
                    if (i == j) continue;
                    X[i] -= A[i][j] * X[j];
                }
                X[i] /= A[i][i];
                if (X[i] < 0) X[i] = 0;
                totAdj += Math.abs(X[i] - prevx);
            }
            if (totAdj < 1e-10) break;
        }
        System.out.println("Iterations " + iter + ": last adj = " + totAdj);
        return unflatten(X, n);
    }

    // given a DAMAS solution for source intensities, reconstruct the corresponding heatmap.
    // in theory, this should be very similar to the input heatmap
    public double[][] reconstruct(double[][] X) {
        double[][] hm = new double[n][n];
        for (int i=0; i < n; i++) {
            for (int j=0; j < n; j++) {
                for (int x = 0; x < n; x++) {
                    for (int y = 0; y < n; y++) {
                        hm[x][y] += X[i][j] * A[i*n + j][x * n + y];
                    }
                }
            }
        }
        return hm;
    }
}
