package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Heap;
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

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY, PROGRAM, data, Chunk.EMPTY)).init();
        i.getSegmentForChunk(0x02, Frob.IN_USE);
        i.setWidth(false);
        i.setAH(0xbb);
        i.setAL(0xaa);
        Heap.get(0x5b).write(0x03); // address (lo)
        Heap.get(0x5c).write(0x00); // address (hi)
        Heap.get(0x5d).write(0x02); // segment#
        i.setBL(0x03); // address offset
        i.setWidth(true);
        i.start(1, 1);

        assertEquals(0xbbaa, i.memory().read(0x02, 0x06, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00,
                (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY, PROGRAM, data, Chunk.EMPTY)).init();
        i.getSegmentForChunk(0x02, Frob.IN_USE);
        i.setWidth(false);
        i.setAH(0xbb);
        i.setAL(0xaa);
        Heap.get(0x5b).write(0x03);
        Heap.get(0x5c).write(0x00);
        Heap.get(0x5d).write(0x02);
        i.setBL(0x03); // address offset
        i.start(1, 1);

        assertEquals(0x00aa, i.memory().read(0x02, 0x06, 2));
        assertEquals(2, i.instructionsExecuted());
    }
}