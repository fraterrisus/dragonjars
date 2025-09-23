package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveHeapTest {
    @BeforeAll
    public static void setup() {
        Heap.reset();
    }

    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x19, // StoreAXHeap
                (byte)0x3a, // read index
                (byte)0x18, // write index
                (byte)0x1e  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(true);
        Heap.get(0x3a).write(0x1234, 2);

        i.start(0, 0);

        assertEquals(0x00001234, Heap.get(0x18).read(2));
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x19, // StoreAXHeap
                (byte)0x3a, // read index
                (byte)0x18, // write index
                (byte)0x1e  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(true);
        Heap.get(0x3a).write(0x1234, 2);

        i.start(0, 0);

        assertEquals(0x00000034, Heap.get(0x18).read(2));
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}