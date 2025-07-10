package acousticeyes.network;

/* Represents on frame's worth of microphone data. Packet data is copied into this structure
 * as packets arrive.
 */
public class MicFrameData {

    public double[][] samples;
    public int startSeq; // sequence number of first packet
    private long[] filledPackets; // bitmap indicating which packets have arrived

    // mask for last partial entry in filledPackets
    private static final long MASK = MicrophoneDataDispatcher.PACKETS_PER_FRAME % 64 == 0 ? -1 :
            (1L << MicrophoneDataDispatcher.PACKETS_PER_FRAME % 64) - 1;

    public MicFrameData(int startSeq) {
        samples = new double[UdpServer.NUM_MICROPHONES][MicrophoneDataDispatcher.SAMPLES_PER_FRAME];
        this.startSeq = startSeq;
        filledPackets = new long[(MicrophoneDataDispatcher.PACKETS_PER_FRAME + 63) / 64];
    }

    public boolean isInRange(MicDataPacket mdp) {
        return mdp.sequenceNumber >= startSeq &&
                mdp.sequenceNumber < startSeq + MicrophoneDataDispatcher.PACKETS_PER_FRAME;
    }

    public void copyPacket(MicDataPacket mdp) {
        if (!isInRange(mdp)) return;
        if (isFilled(mdp.sequenceNumber)) {
            System.err.println("Already filled sequence number " + mdp.sequenceNumber);
        }
        int start = (mdp.sequenceNumber - startSeq) * UdpServer.SAMPLES_PER_MIC;
        for (int m = 0; m < samples.length; m++) {
            for (int s = 0; s < UdpServer.SAMPLES_PER_MIC; s++) {
                samples[m][start + s] = mdp.samples[m][s];
            }
        }
        setFilled(mdp.sequenceNumber);
    }

    // determines whether all packets for this frame have arrived
    public boolean isComplete() {
        for (int i = 0; i < filledPackets.length - 1; i++) {
            if (filledPackets[i] != -1) return false;
        }
        return filledPackets[filledPackets.length - 1] == MASK;
    }

    private boolean isFilled(int seq) {
        int s = seq - startSeq;
        return (filledPackets[s / 64] & (1L << (s % 64))) != 0;
    }

    private void setFilled(int seq) {
        int s = seq - startSeq;
        filledPackets[s / 64] |= (1L << (s % 64));
    }
}
