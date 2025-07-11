package acousticeyes.network;

import acousticeyes.beamforming.BeamformingManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/* Assembles individual microphone data packets into MicFrameData containers which represent
 * all of the microphone data for a whole frame, and sends complete frames (ones with no
 * missing packets) off to BeamformingManager for processing.
 *
 * This schedules a periodic task, once per frame, which starts collecting and assembling
 * packets. Packets could arrive late, or not at all, so this task will time out after one
 * frame duration so that if there are missing packets, it does not block future frame's
 * processing waiting for data that will never show up.
 */
public class MicrophoneDataDispatcher {

    public static final int SAMPLES_PER_SECOND = 48000;
    public static final int FRAMES_PER_SECOND = 32;
    public static final int MS_PER_FRAME = 1000 / FRAMES_PER_SECOND;
    public static final int SAMPLES_PER_FRAME = 420; //SAMPLES_PER_SECOND * MS_PER_FRAME / 1000;
    public static final int PACKETS_PER_FRAME = 60; // SAMPLES_PER_FRAME / UdpServer.SAMPLES_PER_MIC;

    private boolean initialized = false; // whether any packets for the current frame have been received
    private MicFrameData frame;
    private BeamformingManager bfManager;

    public MicrophoneDataDispatcher(BeamformingManager bm) {
        frame = new MicFrameData(0);
        bfManager = bm;
        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.scheduleAtFixedRate(this::processFrame, 0, MS_PER_FRAME, TimeUnit.MILLISECONDS);
    }

    private void processFrame() {
        synchronized (this) {
            initialized = false;
        }
        long startTime = System.currentTimeMillis();
        // wait up to a frame duration for the packets for this frame to come in
        while ((!initialized || !frame.isComplete()) && System.currentTimeMillis() < startTime + MS_PER_FRAME) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
        // check initialized here as well - if no packets were received, frame could still refer to a previous completed frame
        if (initialized && frame.isComplete()) {
            bfManager.updateLatestFrame(frame);
        }

    }

    // called from packet processor thread
    public void accept(MicDataPacket mdp) {
        synchronized (this) {
            if (!initialized) {
                initialized = true;
                frame = new MicFrameData(mdp.sequenceNumber);
            }
            frame.copyPacket(mdp); // this is a no-op if mdp is not within frame's range
        }
    }
}