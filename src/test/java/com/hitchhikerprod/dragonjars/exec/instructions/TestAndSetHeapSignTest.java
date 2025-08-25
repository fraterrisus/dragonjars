package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestAndSetHeapSignTest {
    @Test
    public void set() {
        final Chunk program = new Chunk(List.of(
                (byte)0x48, // TestHeapSign
                (byte)0x06, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.heap(Heap.SELECTED_PC).write(0xff);
        i.start(0, 0);

        assertFalse(i.getZeroFlag());
        assertEquals(0x80, i.heap(Heap.SELECTED_PC).read() & 0x80);
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void clear() {
        final Chunk program = new Chunk(List.of(
                (byte)0x48, // TestHeapSign
                (byte)0x06, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.heap(Heap.SELECTED_PC).write(0x7f);
        i.start(0, 0);

        assertTrue(i.getZeroFlag());
        // bit gets set if not already set
        assertEquals(0x80, i.heap(Heap.SELECTED_PC).read() & 0x80);
        assertEquals(2, i.instructionsExecuted());
    }
}