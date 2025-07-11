package acousticeyes.ui;

import acousticeyes.beamforming.BeamformingManager;
import acousticeyes.beamforming.PhasedArray;
import acousticeyes.network.MicrophoneDataDispatcher;
import acousticeyes.network.UdpServer;

import javax.swing.*;
import java.net.SocketException;

public class AcousticEyes extends JFrame {

    private PhasedArray arr = PhasedArray.radial(8, 12, 0.05, 0.3, 1.25, 1, 0);
    private MainPanel mp = new MainPanel();
    private BeamformingManager bm = new BeamformingManager(arr, mp);
    private MicrophoneDataDispatcher mdd = new MicrophoneDataDispatcher(bm);
    private UdpServer server;

    public AcousticEyes() {
        super("AcousticEyes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        mp.setBounds(0,0, 800, 800);
        add(mp);

        setSize(1200, 900);
        setVisible(true);
        try {
            server = new UdpServer(mdd);
            server.start();
        } catch (SocketException e) {
        }
    }

    public static void main(String[] args) {
        new AcousticEyes();
    }
}
