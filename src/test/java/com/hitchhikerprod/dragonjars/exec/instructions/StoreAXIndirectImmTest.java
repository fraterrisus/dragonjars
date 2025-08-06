package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreAXIndirectImmTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x18, // StoreAXIndirectImm
            (byte) 0x5b, //   heap index
            (byte) 0x03, //   addr offset
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data), 0, 0);
        i.setWidth(false);
        i.setDS(0x01);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setHeap(0x5b, 0x07);
        i.setHeap(0x5c, 0x00);
        i.setWidth(true);
        i.start();

        assertEquals(0xbbaa, i.readWord(0x01, 0x0a));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data), 0, 0);
        i.setWidth(false);
        i.setDS(0x01);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setHeap(0x5b, 0x07);
        i.setHeap(0x5c, 0x00);
        i.start();

        assertEquals(0x00aa, i.readWord(0x01, 0x0a));
        assertEquals(2, i.instructionsExecuted());
    }
}