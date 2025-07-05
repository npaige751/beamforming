package acousticeyes.filter;

public class FIRFilter {

    private double[] coeffs;
    private double[] recentSamples;
    private int sidx = 0;

    public FIRFilter(double[] coeffs) {
        this.coeffs = coeffs;
        this.recentSamples = new double[coeffs.length];
    }

    public double filter(double x) {
        double res = 0;
        int si = sidx + 1;
        for (int i=0; i < coeffs.length; i++) {
            if (si == recentSamples.length) si = 0;
            res += coeffs[i] * recentSamples[si];
            si++;
        }
        return res;
    }

    public static double[] rand(int n) {
        double[] res = new double[n];
        for (int i=0; i < n; i++) {
            res[i] = Math.random();
        }
        return res;
    }

    public static void main(String[] args) throws InterruptedException {
        long NS = 50_000_000;
        int ORDER = 100;
        int THREADS = 16;
        long time = System.currentTimeMillis();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                FIRFilter f = new FIRFilter(rand(ORDER));
                double m = 0.001;
                for (int i=0; i < NS; i++) {
                    f.filter(i*m);
                }

            }
        };
        Thread[] threads = new Thread[THREADS];
        for (int i=0; i < THREADS; i++) {
            threads[i] = new Thread(r);
            threads[i].start();
        }
        for (int i=0; i < THREADS; i++) {
            threads[i].join();
        }
        double nmul = NS * ORDER * THREADS;
        time = System.currentTimeMillis() - time;
        System.out.println("Took " + time + " ms; " + (nmul / time / 1000) + "M multiplications/sec");
    }
}
