package acousticeyes.network;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MicrophoneDataDispatcher {

    public static final int SAMPLES_PER_SECOND = 48000;
    public static final int FRAMES_PER_SECOND = 32;
    public static final int MS_PER_FRAME = 1000 / FRAMES_PER_SECOND;
    public static final int SAMPLES_PER_FRAME = SAMPLES_PER_SECOND * MS_PER_FRAME / 1000;
    public static final int PACKETS_PER_FRAME = SAMPLES_PER_FRAME / UdpServer.SAMPLES_PER_MIC;

    private boolean initialized = false;
    private MicFrameData frame;

    public MicrophoneDataDispatcher() {
        frame = new MicFrameData(0);
        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.scheduleAtFixedRate(this::processFrame, 0, MS_PER_FRAME, TimeUnit.MILLISECONDS);
    }

    private void processFrame() {
        synchronized (this) {
            initialized = false;
        }
        long startTime = System.currentTimeMillis();
        while (!frame.isComplete() && System.currentTimeMillis() < startTime + MS_PER_FRAME) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        if (frame.isComplete()) {
            // send frame for processing
        }

    }

    public void accept(MicDataPacket mdp) {
        synchronized (this) {
            if (!initialized) {
                initialized = true;
                frame = new MicFrameData(mdp.sequenceNumber);
            }
            frame.copyPacket(mdp);
        }
        System.out.println("Received packet " + mdp.sequenceNumber);
    }
}