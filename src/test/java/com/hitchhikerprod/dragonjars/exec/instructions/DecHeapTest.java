package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecHeapTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x26, // DecHeap
            (byte) 0xaa, //   heap index
            (byte) 0x1e  // Exit
    ));

    @BeforeAll
    public static void setup() {
        Heap.reset();
    }

    @Test
    public void wideUnderflow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(false);
        Heap.get(0xaa).write(0x00);
        Heap.get(0xab).write(0x01);
        i.setWidth(true);
        i.start(0, 0);

        i.setWidth(false);
        assertEquals(0xff, Heap.get(0xaa).read());
        assertEquals(0x00, Heap.get(0xab).read());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(false);
        Heap.get(0xaa).write(0x16);
        Heap.get(0xab).write(0xff);
        i.start(0, 0);

        assertEquals(0x15, Heap.get(0xaa).read());
        assertEquals(0xff, Heap.get(0xab).read());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrowUnderflow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(false);
        Heap.get(0xaa).write(0x00);
        Heap.get(0xab).write(0x01);
        i.start(0, 0);

        assertEquals(0xff, Heap.get(0xaa).read());
        assertEquals(0x01, Heap.get(0xab).read());
        assertEquals(2, i.instructionsExecuted());
    }
}