package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreAXLongPtrTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x00, // Padding
            (byte) 0x17, // StoreAXIndirect
            (byte) 0x5b, //   heap index
            (byte) 0x5a  // Exit
    ));

    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(EMPTY, PROGRAM, data), 1, 1);
        i.setWidth(false);
        i.setDS(0xff);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setHeap(0x5b, 0x03); // address (lo)
        i.setHeap(0x5c, 0x00); // address (hi)
        i.setHeap(0x5d, 0x02); // chunk#
        i.setBL(0x03); // address offset
        i.setWidth(true);
        i.start();

        assertEquals(0xbbaa, i.readWord(0x02, 0x06));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(EMPTY, PROGRAM, data), 1, 1);
        i.setWidth(false);
        i.setDS(0xff);
        i.setAH(0xbb);
        i.setAL(0xaa);
        i.setHeap(0x5b, 0x03); // address (lo)
        i.setHeap(0x5c, 0x00); // address (hi)
        i.setHeap(0x5d, 0x02); // chunk#
        i.setBL(0x03); // address offset
        i.start();

        assertEquals(0x00aa, i.readWord(0x02, 0x06));
        assertEquals(2, i.instructionsExecuted());
    }
}