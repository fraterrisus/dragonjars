package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import javafx.concurrent.Task;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

public class PlayChunkSound extends Task<Void> {
    private static final float FREQUENCY = 44100 * 4;

    private final AudioFormat af = new AudioFormat(FREQUENCY,8,1,true,false);
    private final Chunk soundChunk;

    private SourceDataLine sdl;

    public PlayChunkSound(Chunk soundChunk) {
        this.soundChunk = soundChunk;
    }

    @Override
    protected Void call() throws Exception {
        sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();

        final byte[] buf = new byte[1];

        final int numPhases = soundChunk.getWord(0);
        final int delay = soundChunk.getWord(2);

        int numWrites = Math.round(delay / 7.5f);

        for (int b = 4; b < numPhases; b++) {
            buf[0] = soundChunk.getByte(b);
            for (int i = 0; i < numWrites; i++) {
                sdl.write(buf, 0, 1);
            }
        }

        sdl.drain();
        sdl.stop();
        sdl.close();
        return null;
    }
}
