package acousticeyes.network;

/* Represents the decoded samples from one packet. */
public class MicDataPacket {

    public int sequenceNumber;
    public double[][] samples; // indexed by microphone number, sample

    public MicDataPacket(int seq, double[][] samples) {
        this.sequenceNumber = seq;
        this.samples = samples;
    }
}
