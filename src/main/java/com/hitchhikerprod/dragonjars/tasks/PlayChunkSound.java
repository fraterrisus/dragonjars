package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import javax.sound.sampled.SourceDataLine;

public class PlayChunkSound extends Task<Void> {
    private final SourceDataLine sdl;
    private final SimpleObjectProperty<Integer> volume;
    private final Chunk soundChunk;

    public PlayChunkSound(SourceDataLine sdl, SimpleObjectProperty<Integer> volume, Chunk soundChunk) {
        this.sdl = sdl;
        this.volume = volume;
        this.soundChunk = soundChunk;
    }

    @Override
    protected Void call() throws Exception {
        final byte[] buf = new byte[1];

        final int numPhases = soundChunk.getWord(0);
        final int delay = soundChunk.getWord(2);

        int numWrites = Math.round(delay / 7.5f);

        for (int b = 4; b < numPhases; b++) {
            buf[0] = soundChunk.getByte(b);
            for (int i = 0; i < numWrites; i++) {
                sdl.write(buf, 0, volume.get());
            }
        }

        sdl.drain();
        return null;
    }
}
