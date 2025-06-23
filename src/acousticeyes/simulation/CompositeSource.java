package acousticeyes.simulation;

import java.util.Arrays;
import java.util.List;

/* A sum of other SoundSources */
public class CompositeSource implements SoundSource {
    private List<SoundSource> sources;

    public CompositeSource(SoundSource... sources) {
        this.sources = Arrays.stream(sources).toList();
    }

    @Override
    public double get(double t) {
        double sum = 0;
        for (SoundSource s : sources) {
            sum += s.get(t);
        }
        return sum / sources.size();
    }
}
