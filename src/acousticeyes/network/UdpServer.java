package acousticeyes.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpServer {
    public static final int PORT = 42069;

    public static final byte MIC_DATA_MAGIC_NUMBER = 0x12;
    public static final byte VIDEO_DATA_MAGIC_NUMBER = 0x34;

    public static final int NUM_MICROPHONES = 96;
    private static final SampleFormat SAMPLE_FORMAT = new SampleFormat(2, true, true);
    public static final int SAMPLES_PER_MIC = 7;
    private static final int HEADER_SIZE = 5;
    private static final int PACKET_BUFFER_SIZE = HEADER_SIZE + NUM_MICROPHONES * SAMPLES_PER_MIC * SAMPLE_FORMAT.bytes;

    private DatagramSocket socket;
    private Thread receiveThread;
    private MicrophoneDataDispatcher dispatcher;

    public UdpServer(MicrophoneDataDispatcher dispatcher) throws SocketException {
        socket = new DatagramSocket(PORT);
        receiveThread = new Thread(this::processPackets, "PacketProcessor");
        this.dispatcher = dispatcher;
    }

    public void start() {
        receiveThread.start();
    }

    private void processPackets() {
        byte[] packetBuffer = new byte[PACKET_BUFFER_SIZE];
        while (true) {
            DatagramPacket packet = new DatagramPacket(packetBuffer, PACKET_BUFFER_SIZE);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            byte[] data = packet.getData();
            if (data[0] == MIC_DATA_MAGIC_NUMBER) {
                if (dispatcher != null) {
                    dispatcher.accept(decodePacket(data)); // do we need to split this off to a different thread to avoid packet loss?
                }
            } else if (data[0] == VIDEO_DATA_MAGIC_NUMBER) {
            } else {
                // invalid packet
            }
        }
    }

    /* Packet format:
     * - 1 byte magic number (packet type)
     * - 4 byte big endian integer sequence number
     * - NUM_MICROPHONES * SAMPLES_PER_MIC * SAMPLE_FORMAT.bytes bytes of microphone data:
     *   [ samples for mic 0] [ samples for mic 1] ...
     */
    private MicDataPacket decodePacket(byte[] data) {
        int offset = 5;
        int seq = (data[1] << 24) | ((data[2] << 16) & 0xff0000) | ((data[3] << 8) & 0xff00) | (data[4] & 0xff);
        double[][] samples = new double[NUM_MICROPHONES][];
        for (int m = 0; m < NUM_MICROPHONES; m++) {
            samples[m] = SAMPLE_FORMAT.decodeSamples(data, offset, SAMPLES_PER_MIC);
            offset = SAMPLE_FORMAT.advance(offset, SAMPLES_PER_MIC);
        }
        return new MicDataPacket(seq, samples);
    }

}
