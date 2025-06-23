package acousticeyes.util;

// a 3-dimensional vector
public class Vec3 {
    public double x,y,z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 add(Vec3 other) {
        return new Vec3(x + other.x, y + other.y, z + other.z);
    }

    public Vec3 sub(Vec3 other) {
        return new Vec3(x - other.x, y - other.y, z - other.z);
    }

    public Vec3 mul(double d) {
        return new Vec3(x*d, y*d, z*d);
    }

    public double dot(Vec3 other) {
        return x*other.x + y*other.y + z*other.z;
    }

    // projection of this onto other
    public Vec3 projOnto(Vec3 other) {
        return other.mul(this.dot(other) / other.dot(other));
    }

    public double mag() {
        return Math.sqrt(x*x + y*y + z*z);
    }

    public double distance(Vec3 other) {
        return this.sub(other).mag();
    }

    public Vec3 rotX(double th) {
        double c = Math.cos(th);
        double s = Math.sin(th);
        return new Vec3(x, c*y + s*z, -s*y + c*z);
    }

    public Vec3 rotY(double th) {
        double c = Math.cos(th);
        double s = Math.sin(th);
        return new Vec3(c*x + s*z, y, -s*x + c*z);
    }

    public Vec3 rotZ(double th) {
        double c = Math.cos(th);
        double s = Math.sin(th);
        return new Vec3(c*x + s*y, -s*x + c*y, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
