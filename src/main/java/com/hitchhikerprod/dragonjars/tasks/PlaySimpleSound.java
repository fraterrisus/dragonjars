package com.hitchhikerprod.dragonjars.tasks;

import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.util.Pair;

import javax.sound.sampled.SourceDataLine;

public class PlaySimpleSound extends Task<Void> {
    private final SourceDataLine sdl;
    private final SimpleObjectProperty<Integer> volume;
    private final int effectId;

    public PlaySimpleSound(SourceDataLine sdl, SimpleObjectProperty<Integer> volume, int effectId) {
        this.sdl = sdl;
        this.volume = volume;
        this.effectId = effectId;
    }

    @Override
    protected Void call() throws Exception {
        final Pair<Integer, Integer> timing = switch (effectId) {
            case 1 -> new Pair<>(0x30, 0x0a0); // (0xf0, 0x200);
            case 2 -> new Pair<>(0x0c, 0x0c0); // (0x28, 0x400);
            case 3 -> new Pair<>(0x24, 0x180); // (0xc8, 0x800);
            default -> null;
        };
        if (timing == null) return null;

        final byte[] buf = new byte[1];

        boolean toggle = true;
        int i = timing.getValue();
        int j = 0;
        while (i > 0) {
            if (j == 0) {
                j = timing.getKey();
                toggle = !toggle;
            }
            buf[0] = (byte)(toggle ? volume.get() : 0);
            sdl.write(buf, 0, 1);
            j -= 1;
            i -= 1;
        }

        sdl.drain();
        return null;
    }
}
