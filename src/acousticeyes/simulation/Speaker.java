package acousticeyes.simulation;

import acousticeyes.util.Vec3;

public class Speaker {
    public Vec3 pos;
    public SoundSource source;

    public Speaker(Vec3 p, SoundSource s) {
        pos = p;
        source = s;
    }
}
