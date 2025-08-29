package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import javafx.concurrent.Task;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlayTitleMusic extends Task<Void> {
    private static final float FREQUENCY = 44100 * 4;

    // 274.1687 Hz = 3.6473 ms per interrupt cycle
    private static final float DURATION_ADJUST = 3.6473f;

    // These are specific to Song #1, but that's the only music there is.
    private static final int NOTE_2_PAUSE = 0x10;
    private static final int NOTE_3_PAUSE = 0x04;

    private record MusicPhase(
            int duration,
            Optional<Integer> freq2,
            Optional<Integer> freq3
    ) {}

    private final Chunk codeChunk;
    private final byte[] buf;
    private final AudioFormat af;
    private final boolean addHarmonic;

    private List<MusicPhase> phases;

    private SourceDataLine sdl;

    public PlayTitleMusic(Chunk codeChunk, boolean addHarmonic) {
        this.codeChunk = codeChunk;
        this.addHarmonic = addHarmonic;
        if (addHarmonic) {
            buf = new byte[2];
            af = new AudioFormat(FREQUENCY,8,2,true,false);
        } else {
            buf = new byte[1];
            af = new AudioFormat(FREQUENCY,8,1,true,false);
        }
    }

    @Override
    protected Void call() throws LineUnavailableException {
        decodeTitleMusic();
        if (isCancelled()) return null;

        setup();
        playTitleMusic();
        teardown();
        return null;
    }

    private void decodeTitleMusic() {
        phases = new ArrayList<>();
        int pointer = 0x5edc;
        int duration;
        Optional<Integer> freq2 = Optional.empty();
        Optional<Integer> freq3 = Optional.empty();

        while (true) {
            final int durationByte = codeChunk.getUnsignedByte(pointer);
            final int frequencyByte = codeChunk.getUnsignedByte(pointer + 1);
            if (durationByte >= 0xfa) break;

            final int targetStruct = durationByte >> 5;
            final int durationIndex = 0x575e + (durationByte & 0x1f);
            duration = 0x6 * codeChunk.getUnsignedByte(durationIndex);

            final boolean overlap = (frequencyByte & 0x80) > 0;
            final int freq = frequencyByte & 0x7f;
            final int frequency;
            if (freq != 0x7f) {
                final int shiftDistance = freq / 0x0c;
                final int frequencyIndex = freq % 0x0c;
                final int frequencyValue = codeChunk.getWord(0x58fd + (2 * frequencyIndex));
                frequency = 0x1234dd / (frequencyValue >> shiftDistance);
                if (targetStruct == 2) freq2 = Optional.of(frequency);
                if (targetStruct == 3) freq3 = Optional.of(frequency);
            }

            if (!overlap) {
                phases.add(new MusicPhase(duration, freq2, freq3));
                freq2 = Optional.empty();
                freq3 = Optional.empty();
            }

            pointer += 2;
        }
    }

    private void playTitleMusic() throws LineUnavailableException {
        for (MusicPhase phase : phases) {
            if (isCancelled()) return;

            if (phase.duration() == 0) continue;
            int t0 = 0;
            int t1;
            if (phase.freq2().isPresent()) {
                final int cycles = phase.duration() - NOTE_2_PAUSE;
                t1 = Math.round(DURATION_ADJUST * cycles);
                generateTone(phase.freq2().get(), t1 - t0, 50);
                t0 = t1;
            }
            if (phase.freq3().isPresent()) {
                final int cycles = phase.duration() - NOTE_3_PAUSE;
                t1 = Math.round(DURATION_ADJUST * cycles);
                generateTone(phase.freq3().get(), t1 - t0, 50);
                t0 = t1;
            }
            t1 = Math.round(DURATION_ADJUST * phase.duration());
            generateTone(0, t1 - t0, 50);
        }
    }

    private void setup() throws LineUnavailableException {
        sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.start();
    }

    private void teardown() {
        sdl.drain();
        sdl.stop();
        sdl.close();
    }

    /** Generates a tone, and if addHarmonic is set, its harmonic one octave up.
     @param hz Base frequency (neglecting harmonic) of the tone in cycles per second
     @param ms The number of milliseconds to play the tone.
     @param volume Volume, form 0 (mute) to 100 (max). */
    private void generateTone(int hz, int ms, int volume) {
        if (ms == 0) return;
        for (int i = 0; i < ms * FREQUENCY / 1000; i++) {
            final double angle = i / (FREQUENCY / hz) * 2.0 * Math.PI;
            buf[0] = (byte)(Math.sin(angle) * volume);
            if (addHarmonic) {
                buf[1] = (byte)(Math.sin(2 * angle) * volume * 0.2);
                sdl.write(buf, 0, 2);
            } else {
                sdl.write(buf, 0, 1);
            }
        }
    }
}
