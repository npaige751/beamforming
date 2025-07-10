package acousticeyes.simulation.ui;

import acousticeyes.beamforming.PhasedArray;

public interface ArrayListener {
    void phasedArrayUpdated(PhasedArray array);
}
