package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CopyAutomapBufferTest {
    public static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x1d, // BufferCopy
            (byte) 0x1e  // Exit
    ));

    @Test
    public void fromBufferToChunk() {
        final Chunk data = new Chunk(new byte[0x700]);

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data, Chunk.EMPTY)).init();
        for (int idx = 0; idx < 0x700; idx++) {
            int value = (int)(Math.random() * 0xff);
            i.memory().automapChunk().write(idx, 1, value);
        }
        final int segmentId = i.getSegmentForChunk(0x01, Frob.IN_USE);
        i.setDS(segmentId);
        i.setAH(0x00);
        i.setAL(0x00);
        i.setBL(0x00);
        i.start(0, 0);

        final Chunk newData = i.memory().getSegment(segmentId);

        for (int idx = 0; idx < 0x700; idx++) {
            assertEquals(i.memory().automapChunk().read(idx, 1), newData.getUnsignedByte(idx),
                    "Byte " + idx + " failed to match");
        }
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void fromChunkToBuffer() {
        final byte[] rawBytes = new byte[0x700];
        for (int idx = 0; idx < 0x700; idx++) {
            rawBytes[idx] = (byte) (Math.random() * 0xff);
        }
        final Chunk data = new Chunk(rawBytes);

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data, Chunk.EMPTY)).init();
        i.setDS(i.getSegmentForChunk(0x01, Frob.IN_USE));
        i.setAH(0x00);
        i.setAL(0x00);
        i.setBL(0xff);
        i.start(0, 0);

        for (int idx = 0; idx < 0x700; idx++) {
            assertEquals(data.getUnsignedByte(idx), i.memory().automapChunk().read(idx, 1),
                    "Byte " + idx + " failed to match");
        }
        assertEquals(2, i.instructionsExecuted());
    }
}