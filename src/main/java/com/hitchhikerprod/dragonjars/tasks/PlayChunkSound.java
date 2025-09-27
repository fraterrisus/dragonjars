package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import javafx.beans.property.IntegerProperty;
import javafx.concurrent.Task;

import javax.sound.sampled.SourceDataLine;

public class PlayChunkSound extends Task<Void> {
    private final SourceDataLine sdl;
    private final IntegerProperty volume;
    private final Chunk soundChunk;

    public PlayChunkSound(SourceDataLine sdl, IntegerProperty volume, Chunk soundChunk) {
        this.sdl = sdl;
        this.volume = volume;
        this.soundChunk = soundChunk;
    }

    @Override
    protected Void call() throws Exception {
        final byte[] buf = new byte[1];

        final int numPhases = soundChunk.getWord(0);
        final int delay = soundChunk.getWord(2);

        int numWrites = Math.round(delay / 6f); // was 7.5

        // Any higher than this starts to clip
        double volumeFactor = 1.8 * volume.get() / 100.0;

        for (int b = 4; b < numPhases; b++) {
            buf[0] = (byte)(Math.round(soundChunk.getByte(b) * volumeFactor));
            for (int i = 0; i < numWrites; i++) {
                sdl.write(buf, 0, 1);
            }
        }

        sdl.drain();
        return null;
    }
}
