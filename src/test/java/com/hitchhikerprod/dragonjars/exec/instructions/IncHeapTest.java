package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncHeapTest {
    public static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x23, // IncHeap
            (byte) 0x74, //   heap index
            (byte) 0x1e  // Exit
    ));

    @BeforeAll
    public static void setup() {
        Heap.reset();
    }

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(true);
        Heap.get(0x74).write(0xff);
        Heap.get(0x75).write(0x01);
        i.start(0, 0);

        assertEquals(0x00, Heap.get(0x74).read());
        assertEquals(0x02, Heap.get(0x75).read());
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(false);
        Heap.get(0x74).write(0xff);
        Heap.get(0x75).write(0x01);
        i.start(0, 0);

        assertEquals(0x00, Heap.get(0x74).read());
        assertEquals(0x01, Heap.get(0x75).read());
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }
}