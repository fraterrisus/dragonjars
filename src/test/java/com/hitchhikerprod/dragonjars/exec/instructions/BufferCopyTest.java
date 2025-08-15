package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BufferCopyTest {
    public static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x1d, // BufferCopy
            (byte) 0x1e  // Exit
    ));

    @Test
    public void fromBufferToChunk() {
        final Chunk data = new Chunk(new byte[0x700]);

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data));
        for (int idx = 0; idx < 0x700; idx++) {
            i.writeBufferD1B0(idx, (int)(Math.random() * 0xff));
        }
        i.setDS(0x01);
        i.setAH(0x00);
        i.setAL(0x00);
        i.setBL(0x00);
        i.start(0, 0);

        final Chunk newData = i.getSegment(0x01);

        for (int idx = 0; idx < 0x700; idx++) {
            assertEquals(i.readBufferD1B0(idx), newData.getUnsignedByte(idx),
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

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data));
        i.setDS(0x01);
        i.setAH(0x00);
        i.setAL(0x00);
        i.setBL(0xff);
        i.start(0, 0);

        for (int idx = 0; idx < 0x700; idx++) {
            assertEquals(data.getUnsignedByte(idx), i.readBufferD1B0(idx),
                    "Byte " + idx + " failed to match");
        }
        assertEquals(2, i.instructionsExecuted());
    }
}