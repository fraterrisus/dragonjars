package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreAXIndirectTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x16, // StoreAXIndirect
            (byte) 0x5b, //   heap index
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(List.of(PROGRAM, data), 0, 0);
        i.setWidth(true);
        i.setDS(0x01);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setHeap(0x5b, 0x0001);
        i.setBL(0x04);
        i.start();

        assertEquals(0xbbaa, i.readWord(0x01, 0x05));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(List.of(PROGRAM, data), 0, 0);
        i.setWidth(true);
        i.setDS(0x01);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setHeap(0x5b, 0x0001);
        i.setBL(0x04);
        i.setWidth(false);
        i.start();

        assertEquals(0x00aa, i.readWord(0x01, 0x05));
        assertEquals(2, i.instructionsExecuted());
    }
}