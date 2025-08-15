package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreZeroHeapTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x11, // StoreZeroHeap
                (byte)0x3a, // heap index
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setHeap(0x3a, 0xff);
        i.setHeap(0x3b, 0xff);

        i.start(0, 0);

        assertEquals(0x00000000, i.getHeapBytes(0x3a, 2));
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x11, // StoreZeroHeap
                (byte)0x3a, // heap index
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setHeap(0x3a, 0xff);
        i.setHeap(0x3b, 0xff);

        i.start(0, 0);

        assertEquals(0x0000ff00, i.getHeapBytes(0x3a, 2));
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}