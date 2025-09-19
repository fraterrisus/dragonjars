package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.VideoBuffer;
import com.hitchhikerprod.dragonjars.exec.VideoHelper;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MonsterAnimationTask extends Task<Void> {
    // In theory this uses the same 0x02 counter as the torch
    private static final int ANIMATION_DELAY_MS = 120;

    private final Interpreter interpreter;
    private final Chunk priChunk, secChunk;
    private final List<Subimage> subimages = new ArrayList<>();
    private final VideoBuffer background;
    private final VideoBuffer foreground;
    private final PixelRectangle mask;

    private record Subimage(int counter, int pointer, int proceed) {
        public Subimage decrement() {
            return new Subimage(counter - 1, pointer, proceed);
        }
    }

    public MonsterAnimationTask(Interpreter interpreter, Chunk primarychunk, Chunk secondaryChunk, VideoBuffer background) {
        this.interpreter = interpreter;
        this.priChunk = primarychunk;
        this.secChunk = secondaryChunk;
        this.background = background;
        this.foreground = new VideoBuffer(VideoBuffer.CHROMA_KEY);
        // this is threadsafe
        this.mask = interpreter.draw().getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel();
    }

    private boolean weShouldStop() {
        return isCancelled() || !interpreter.isMonsterAnimationEnabled();
    }

    @Override
    protected Void call() throws Exception {
        decodePrimary();
        sendToScreen();

        subimages.clear();
        for (int i = 0; i < 4; i++) {
            final int basePointer = secChunk.getWord(2*i);
            if (basePointer == 0) subimages.add(null);
            else subimages.add(new Subimage(0, basePointer, 0));
        }

        while (true) {
            sleepHelper(ANIMATION_DELAY_MS);
            if (weShouldStop()) break;

            // This isn't threadsafe but I mostly don't care
            final boolean travelMode = interpreter.heap(Heap.COMBAT_MODE).read() == 0;
            if (interpreter.isPaused() && travelMode) continue;

            decodeSecondary();
            if (weShouldStop()) break;

            sendToScreen();
            if (weShouldStop()) break;
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

    private void decodePrimary() {
        int x0 = priChunk.getUnsignedByte(0);
        int y0 = priChunk.getUnsignedByte(1);
        int x1 = priChunk.getUnsignedByte(2);
        int y1 = priChunk.getUnsignedByte(3);

        int pointer = 4;
        int bx = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                bx = bx ^ priChunk.getUnsignedByte(pointer++);
                final byte hi = (byte)((bx >> 4) & 0xf);
                final byte lo = (byte)(bx & 0xf);
                if (hi != 6) foreground.set(mask.x0() + 2*x, mask.y0() + y, hi);
                if (lo != 6) foreground.set(mask.x0() + (2*x)+1, mask.y0() + y, lo);
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

                applyDiff(secChunk, ptr);

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

    private void applyDiff(Chunk chunk, int myPointer) {
        if (chunk.getUnsignedByte(myPointer) == 0xff) return;

        for (int y = 0x00; y < 0x88; y++) {
            for (int x = 0x00; x < 0x46; x++) {
                int byte3 = chunk.getUnsignedByte(myPointer++); // 0x4c4c

                while (byte3 == 0) {
                    x = chunk.getUnsignedByte(myPointer++); // dx, 0x4c3c
                    if (x == 0xff) return;
                    y = chunk.getUnsignedByte(myPointer++); // bx, 0x4c46
                    byte3 = chunk.getUnsignedByte(myPointer++); // ax, 0x4c4c
                }

                int vbx = mask.x0() + (2*x);
                final int vby = mask.y0() + y;
                final byte hi_in = foreground.get(vbx, vby);
                final byte hi_md = (byte)((byte3 >> 4) & 0xf);
                foreground.set(vbx, vby, (byte)(hi_in ^ hi_md));
                vbx++;
                final byte lo_in = foreground.get(vbx, vby);
                final byte lo_md = (byte)(byte3 & 0xf);
                foreground.set(vbx, vby, (byte)(lo_in ^ lo_md));
            }
        }
    }

    private void sendToScreen() {
        final VideoBuffer output = new VideoBuffer(background);
        foreground.writeTo(output, mask);
        if (weShouldStop()) return;
        interpreter.bitBlast(output, mask);
    }
}
