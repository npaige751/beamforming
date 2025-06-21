package acousticeyes.simulation.ui;

import acousticeyes.simulation.Speaker;

import java.util.List;

public interface SpeakerListener {
    public void speakersUpdated(List<Speaker> speakers);
}
