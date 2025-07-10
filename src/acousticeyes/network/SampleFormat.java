package acousticeyes.network;

public class SampleFormat {
    public int bytes;
    public boolean signed;
    public boolean bigEndian;

    public SampleFormat(int bytes, boolean signed, boolean bigEndian) {
        this.bytes = bytes;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public double[] decodeSamples(byte[] data, int offset, int nsamples) {
        double[] samples = new double[nsamples];
        for (int i = 0; i < nsamples; i++) {
            samples[i] = decodeSample(data, offset);
            offset += bytes;
        }
        return samples;
    }

    private double decodeSample(byte[] data, int offset) {
        switch(bytes) {
            case 1:
                if (signed) {
                    return data[offset] / 128.0;
                } else {
                    return data[offset] / 128.0 - 1.0;
                }
            case 2:
                byte msb = data[offset + (bigEndian ? 0 : 1)];
                byte lsb = data[offset + (bigEndian ? 1 : 0)];
                int val = (((int) msb) << 8) | (((int) lsb) & 0xff);
                if (signed) {
                    return val / 32768.0;
                } else {
                    return ((short) val) / 32768.0 - 1.0;
                }
        }
        return 0;
    }

    public int advance(int offset, int nsamples) {
        return offset + nsamples * bytes;
    }
}
