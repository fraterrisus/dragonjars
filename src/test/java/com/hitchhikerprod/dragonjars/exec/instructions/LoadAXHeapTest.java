package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadAXHeapTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x0a, // LoadAXHeap
                (byte)0x26, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setAH(0xff);
        i.setAL(0xff);
        i.setHeap(0x26, 0xaa);
        i.setHeap(0x27, 0xbb);
        i.start();

        assertEquals(0x0000bbaa, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x0a, // LoadAXHeap
                (byte)0x26, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setAH(0xff);
        i.setAL(0xff);
        i.setHeap(0x26, 0xaa);
        i.setHeap(0x27, 0xbb);
        i.start();

        assertEquals(0x000000aa, i.getAX());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}