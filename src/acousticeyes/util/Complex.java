package acousticeyes.util;

public class Complex {

    public double a, b;

    public Complex(double r, double i) {
        a = r;
        b = i;
    }

    // static 3-address style prevents extraneous allocations
    public static void add(Complex res, Complex a, Complex b) {
        double r = a.a + b.a;
        res.b = a.b + b.b;
        res.a = r;
    }

    public static void sub(Complex res, Complex a, Complex b) {
        double r = a.a - b.a;
        res.b = a.b - b.b;
        res.a = r;
    }

    public static void mul(Complex res, Complex x, Complex y) {
        double r = x.a * y.a - x.b * y.b;
        res.b = x.a * y.b + x.b * y.a;
        res.a = r;
    }

    // return e^(i * theta)
    public static Complex expi(double theta) {
        return new Complex(Math.cos(theta), Math.sin(theta));
    }

    @Override
    public String toString() {
        return a + " + i*" + b;
    }
}
