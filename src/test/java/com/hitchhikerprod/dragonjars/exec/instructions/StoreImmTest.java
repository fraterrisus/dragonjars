package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreImmTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x1c, // StoreImm
                (byte) 0x06, //   addr (lo)
                (byte) 0x00, //   addr (hi)
                (byte) 0xaa, //   value (lo)
                (byte) 0xbb, //   value (hi)
                (byte) 0x5a  // Exit
        ));

        final Chunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(program, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.CLEAN);
        i.setDS(dataSegment);
        i.setWidth(true);
        i.start(0, 0);

        assertEquals(0xbbaa, i.memory().read(dataSegment, 0x06, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x1c, // StoreImm
                (byte) 0x06, //   addr (lo)
                (byte) 0x00, //   addr (hi)
                (byte) 0xaa, //   value (lo)
                (byte) 0x5a  // Exit
        ));

        final Chunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(program, data, Chunk.EMPTY)).init();
        final int dataSegment = i.getSegmentForChunk(0x01, Frob.CLEAN);
        i.setDS(dataSegment);
        i.setWidth(false);
        i.start(0, 0);

        assertEquals(0x00aa, i.memory().read(dataSegment, 0x06, 2));
        assertEquals(2, i.instructionsExecuted());
    }
}