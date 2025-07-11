package acousticeyes.test.udp;

import acousticeyes.beamforming.PhasedArray;
import acousticeyes.util.Vec3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/* Generate simulated microphone data and send it over UDP */
public class UdpClient {
    public static final int SRC_PORT = 42068;
    public static final int DEST_PORT = 42069;

    public static final double SPEED_OF_SOUND = 340.0;
	public static final int SPS = 48000;
	public static final int SPP = 7;
	public static final int BPS = 2;
    public static final double SAMPLE_TIME = 1.0 / SPS;

    public static final PhasedArray arr = PhasedArray.radial(8, 12, 0.05, 0.3, 1.25, 1, 0);
	public static final double FREQ = 6000;

    public static final int PACKET_SIZE = SPP * BPS * arr.mics.size() + 5;

    private static int packetNum = 0;

	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket(SRC_PORT, InetAddress.getLocalHost());
		socket.connect(InetAddress.getLocalHost(), DEST_PORT);

		while (true) {
			DatagramPacket packet = new DatagramPacket(generatePacket(), PACKET_SIZE);
			socket.send(packet);
		}
	}

	private static Vec3 getSpeakerPos(double t) {
		// move speaker around in a circle
		return new Vec3(1 * Math.cos(2*Math.PI*0.2*t), 1 * Math.sin(2*Math.PI*0.2*t), 8.0);
	}

	private static short getSample(int m, double t) {
		double delay = getSpeakerPos(t).distance(arr.mics.get(m).pos) / SPEED_OF_SOUND;
		double a = (Math.cos(2 * Math.PI * FREQ * (t + delay)) + 1) * 0.5;
		return (short) (a * 32767);
	}

	private static byte[] generatePacket() {
		byte[] b = new byte[PACKET_SIZE];
		b[0] = 0x12;
		b[1] = (byte) ((packetNum >> 24) & 0xff);
		b[2] = (byte) ((packetNum >> 16) & 0xff);
		b[3] = (byte) ((packetNum >> 8) & 0xff);
		b[4] = (byte) (packetNum & 0xff);
		for (int m = 0; m < arr.mics.size(); m++) {
			for (int s = 0; s < SPP; s++) {
				short sample = getSample(m, (packetNum * SPP + s) * SAMPLE_TIME);
				b[5 + 2*(m*SPP + s)] = (byte) (sample >> 8);
				b[5 + 2*(m*SPP + s) + 1] = (byte) sample;
			}
		}
		packetNum++;
		return b;
	}
}