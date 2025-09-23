package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreAXTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x14, // StoreAX
            (byte) 0x05, //   index (lo)
            (byte) 0x00, //   index (hi)
            (byte) 0x1e  // Exit
    ));

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.IN_USE);
        i.setDS(dataSegment);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setWidth(true);
        i.start(0, 0);

        assertEquals(0xbbaa, i.memory().read(dataSegment, 0x05, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.IN_USE);
        i.setDS(dataSegment);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setWidth(false);
        i.start(0, 0);

        assertEquals(0x00aa, i.memory().read(dataSegment, 0x05, 2));
        assertEquals(2, i.instructionsExecuted());
    }
}