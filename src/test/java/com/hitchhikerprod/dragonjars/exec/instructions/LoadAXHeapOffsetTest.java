package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadAXHeapOffsetTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x0b, // LoadAXHeapOffset
                (byte)0x26, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setBL(0x04);
        Heap.get(0x26).write(0xaa);
        Heap.get(0x27).write(0xbb);
        Heap.get(0x28).write(0xcc);
        Heap.get(0x29).write(0xdd);
        Heap.get(0x2a).write(0xee);
        Heap.get(0x2b).write(0xff);

        i.start(0, 0);

        assertEquals(0x0000ffee, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x0b, // LoadAXHeapOffset
                (byte)0x26, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setBL(0x04);
        Heap.get(0x26).write(0xaa);
        Heap.get(0x27).write(0xbb);
        Heap.get(0x28).write(0xcc);
        Heap.get(0x29).write(0xdd);
        Heap.get(0x2a).write(0xee);
        Heap.get(0x2b).write(0xff);

        i.start(0, 0);

        assertEquals(0x000000ee, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}