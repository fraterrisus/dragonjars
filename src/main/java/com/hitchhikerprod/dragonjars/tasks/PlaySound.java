package com.hitchhikerprod.dragonjars.tasks;

import javafx.concurrent.Task;
import javafx.util.Pair;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

// TODO: change this into a Service with a long-running SourceDataLine
public class PlaySound extends Task<Void> {
    private static final float FREQUENCY = 44100;

    private final byte[] buf = new byte[1];
    private final AudioFormat af = new AudioFormat(FREQUENCY,8,1,true,false);
    private final int effectId;

    private SourceDataLine sdl;

    public PlaySound(int effectId) {
        this.effectId = effectId;
    }

    @Override
    protected Void call() throws Exception {
        sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();

        final Pair<Integer, Integer> timing = switch (effectId) {
            case 1 -> new Pair<>(0x0c, 0x28); // (0xf0, 0x200);
            case 2 -> new Pair<>(0x03, 0x30); // (0x28, 0x400);
            case 3 -> new Pair<>(0x09, 0x60); // (0xc8, 0x800);
            default -> null;
        };
        if (timing == null) return null;

        final int volume = 25; // out of 100
        boolean toggle = true;
        int i = timing.getValue();
        int j = 0;
        while (i > 0) {
            if (j == 0) {
                j = timing.getKey();
                toggle = !toggle;
            }
            buf[0] = (byte)(toggle ? volume : 0);
            sdl.write(buf, 0, 1);
            j -= 1;
            i -= 1;
        }

        sdl.drain();
        sdl.stop();
        sdl.close();
        return null;
    }
}
