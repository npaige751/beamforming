package acousticeyes.beamforming;

import acousticeyes.network.MicFrameData;
import acousticeyes.network.MicrophoneDataDispatcher;
import acousticeyes.ui.MainPanel;
import acousticeyes.util.WindowFunctions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/* Manages a pool of worker threads to perform the actual beamforming computations upon
 * receiving a new MicFrameData from MicrophoneDataDispatcher. There is a periodic task
 * that runs once per frame (if it is not already running, i.e., if the previous frame's
 * computation finished in time) that checks to see if new microphone data is available,
 * and begins processing it. If processing takes too long, old mic data is dropped and
 * replaced with new data, so that an ever-growing backlog of work doesn't form.
 */
public class BeamformingManager {

    private static final int NTHREADS = 8;
    private static final int WINDOW_SIZE = 256; // DFT window size for computing spectra
    private static final int OVERLAP = 0; // how much to overlap adjacent DFT windows
    private static final double[] WINDOW = WindowFunctions.blackmanHarrisWindow(WINDOW_SIZE); // cached window function values

    private static final double FOV = Math.PI / 2;
    private static final int STEPS = 64; // should be a multiple of NTHREADS

    private Executor executor = new ThreadPoolExecutor(NTHREADS, NTHREADS, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private PhasedArray array;
    private MicFrameData latestFrame;
    private MicFrameData lastProcessedFrame;
    private MainPanel mainPanel;

    public BeamformingManager(PhasedArray arr, MainPanel mp) {
        this.array = arr;
        this.mainPanel = mp;
        ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.scheduleAtFixedRate(this::processFrame, 0, MicrophoneDataDispatcher.MS_PER_FRAME, TimeUnit.MILLISECONDS);
    }

    // set the new latest frame data, but do not immediately start a computation. this will get picked
    // up in a subsequent run of processFrame (or will be replaced again before then)
    public void updateLatestFrame(MicFrameData mfd) {
        latestFrame = mfd;
    }

    public void processFrame() {
        try {
            MicFrameData frame = latestFrame; // assign to a local variable so it doesn't get changed out from under us halfway through
            if (frame == null) return;
            if (frame == lastProcessedFrame) return;
            lastProcessedFrame = frame;
            for (int m = 0; m < array.mics.size(); m++) {
                array.mics.get(m).recording = frame.samples[m];
            }
            // compute spectra
            double[][] spectra = new double[array.mics.size()][];
            List<FutureTask<Void>> dftTasks = new ArrayList<>();
            for (int t = 0; t < NTHREADS; t++) {
                final int threadNum = t;
                dftTasks.add(new FutureTask<>(() -> {
                    for (int m = threadNum; m < array.mics.size(); m += NTHREADS) {
                        spectra[m] = array.mics.get(m).computeSpectrum(OVERLAP, WINDOW);
                    }
                }, null));
                executor.execute(dftTasks.get(t));
            }
            // wait for tasks to finish
            for (int t = 0; t < NTHREADS; t++) {
                dftTasks.get(t).get();
            }

            // run DAS beamforming - split into NTHREADS horizontal bands to be processed concurrently
            double[][] heatmap = new double[STEPS][STEPS];
            List<FutureTask<Void>> beamformingTasks = new ArrayList<>();
            for (int t = 0; t < NTHREADS; t++) {
                final int threadNum = t;
                double phiStep = FOV / NTHREADS;
                double phiPixelStep = FOV / STEPS;
                beamformingTasks.add(new FutureTask<>(() -> {
                    double phiStart = threadNum * phiStep - (FOV / 2);
                    double[][] heatmapSlice = array.sweepBeamFreqDomain(spectra, -FOV / 2, FOV / 2, STEPS, phiStart, phiStart + phiStep - phiPixelStep, STEPS / NTHREADS);
                    for (int x = 0; x < STEPS; x++) {
                        for (int y = 0; y < STEPS / NTHREADS; y++) {
                            // no synchronization needed here since different threads access disjoint regions of heatmap
                            heatmap[x][y + threadNum * STEPS / NTHREADS] = heatmapSlice[x][y];
                        }
                    }
                }, null));
                executor.execute(beamformingTasks.get(t));
            }
            // wait for tasks to finish
            for (int t = 0; t < NTHREADS; t++) {
                beamformingTasks.get(t).get();
            }
            // display result
            mainPanel.heatmapUpdated(heatmap);

        } catch (InterruptedException | ExecutionException ignored) {
        }
    }
}
