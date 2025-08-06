package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
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

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setBL(0x04);
        i.setHeap(0x26, 0xaa);
        i.setHeap(0x27, 0xbb);
        i.setHeap(0x28, 0xcc);
        i.setHeap(0x29, 0xdd);
        i.setHeap(0x2a, 0xee);
        i.setHeap(0x2b, 0xff);

        i.start();

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

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setBL(0x04);
        i.setHeap(0x26, 0xaa);
        i.setHeap(0x27, 0xbb);
        i.setHeap(0x28, 0xcc);
        i.setHeap(0x29, 0xdd);
        i.setHeap(0x2a, 0xee);
        i.setHeap(0x2b, 0xff);

        i.start();

        assertEquals(0x000000ee, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}