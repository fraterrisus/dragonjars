package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ImageDecoder;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MonsterAnimationTask extends Task<Void> {
    private final Interpreter interpreter;
    private final Chunk priChunk, secChunk;
    private final List<Subimage> subimages = new ArrayList<>();
    private final int[] background;
    private final int[] foreground;

    private record Subimage(int counter, int pointer, int proceed) {
        public Subimage decrement() {
            return new Subimage(counter - 1, pointer, proceed);
        }
    }

    public MonsterAnimationTask(Interpreter interpreter, Chunk primarychunk, Chunk secondaryChunk) {
        this.interpreter = interpreter;
        this.priChunk = primarychunk;
        this.secChunk = secondaryChunk;
        this.background = new int[0x3e80];
        this.foreground = new int[0x3e80];
        System.arraycopy(interpreter.videoMemory(), 0, background, 0, 0x3e80);
        for (int i = 0; i < 0x3e80; i++) foreground[i] = 0x66;
    }

    @Override
    protected Void call() throws Exception {
        decodePrimary();
        applyChromaKey();

        subimages.clear();
        for (int i = 0; i < 4; i++) {
            final int basePointer = secChunk.getWord(2*i);
            if (basePointer == 0) subimages.add(null);
            else subimages.add(new Subimage(0, basePointer, 0));
        }

        while (true) {
            sleepHelper(250);
            if (isCancelled()) break;
            decodeSecondary();
            if (isCancelled()) break;
            applyChromaKey();
            if (isCancelled()) break;
        }
        return null;
    }

    private void sleepHelper(int sleepTimeMs) {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }

    public void decodePrimary() {
        int x0 = priChunk.getUnsignedByte(0);
        int y0 = priChunk.getUnsignedByte(1);
        int x1 = priChunk.getUnsignedByte(2);
        int y1 = priChunk.getUnsignedByte(3);

        int pointer = 4;
        int bx = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                bx = bx ^ priChunk.getUnsignedByte(pointer);
                pointer++;
                final int byteAnd = ImageDecoder.getAEA2(bx);
                final int byteOr = ImageDecoder.getAFA2(bx);

                final int adr = 5 + x + (y * 0x50);
                int oldValue = foreground[adr];
                int newValue = (oldValue & byteAnd) | byteOr;
                foreground[adr] = (byte)(newValue & 0xff);
            }
        }
    }

    private void decodeSecondary() {
        for (int i = 3; i >= 0; i--) {
            final Subimage s = subimages.get(i);
            if (Objects.isNull(s)) continue;
            if (s.counter() > 0) subimages.set(i, s.decrement());
            else {
                final int ptr = secChunk.getWord(s.pointer() + 1);

                helper_4c23(secChunk, ptr);

                if (secChunk.getUnsignedByte(s.pointer() + 3) == 0xff) {
                    final int basePointer = secChunk.getWord(2 * i);
                    if (basePointer == 0) subimages.set(i, null);
                    else subimages.set(i, new Subimage(0, basePointer, 0));
                } else {
                    final Subimage s1 = new Subimage(
                            secChunk.getUnsignedByte(s.pointer()),
                            s.pointer() + 3,
                            s.proceed()
                    );
                    subimages.set(i, s1);
                }
            }
        }
    }

    private void helper_4c23(Chunk chunk, int myPointer) {
        if (chunk.getUnsignedByte(myPointer) == 0xff) return;

        for (int y = 0x00; y < 0x88; y++) {
            for (int x = 0x05; x < 0x4b; x++) {
                int byte3 = chunk.getUnsignedByte(myPointer++); // 0x4c4c

                while (byte3 == 0) {
                    x = chunk.getUnsignedByte(myPointer++); // dx, 0x4c3c
                    if (x == 0xff) return;
                    x = x + 5;
                    y = chunk.getUnsignedByte(myPointer++); // bx, 0x4c46
                    byte3 = chunk.getUnsignedByte(myPointer++); // ax, 0x4c4c
                }

                final int adr = x + (y * 0x50);
                int oldValue = foreground[adr] & 0xff;
                int newValue = oldValue ^ byte3;
                foreground[adr] = (byte)newValue;
            }
        }
    }

    private void applyChromaKey() {
        final int[] buffer = new int[0x3e80];
        for (int i = 0; i < buffer.length; i++) {
            final int val = foreground[i];
            final int byteAnd = ImageDecoder.getAEA2(val);
            final int byteOr = ImageDecoder.getAFA2(val);

            // final int adr = 5 + x + (y * 0x50);
            int oldValue = background[i];
            int newValue = (oldValue & byteAnd) | byteOr;
            buffer[i] = (byte) (newValue & 0xff);
        }

        if (isCancelled()) return;
        Platform.runLater(() -> interpreter.copyToVideoMemory(buffer));
    }
}
