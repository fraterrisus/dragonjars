package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveDataTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x1b, // MoveData
            (byte) 0x04, //   src addr (lo)
            (byte) 0x00, //   src addr (hi)
            (byte) 0x06, //   dst addr (lo)
            (byte) 0x00, //   dst addr (hi)
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xaa, (byte) 0xbb, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.CLEAN);
        i.setDS(dataSegment);
        i.setWidth(true);
        i.start(0, 0);

        assertEquals(0xbbaa, i.memory().read(dataSegment, 0x06, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xaa, (byte) 0xbb, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.CLEAN);
        i.setDS(dataSegment);
        i.setWidth(false);
        i.start(0, 0);

        assertEquals(0x00aa, i.memory().read(dataSegment, 0x06, 2));
        assertEquals(2, i.instructionsExecuted());
    }

}