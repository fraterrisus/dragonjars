package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DecHeapTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x26, // DecHeap
            (byte) 0xaa, //   heap index
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(List.of(PROGRAM), 0, 0);
        i.setWidth(false);
        i.setHeap(0xaa, 0x00);
        i.setHeap(0xab, 0x01);
        i.setWidth(true);
        i.start();

        i.setWidth(false);
        assertEquals(0xff, i.getHeap(0xaa));
        assertEquals(0x00, i.getHeap(0xab));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(List.of(PROGRAM), 0, 0);
        i.setWidth(false);
        i.setHeap(0xaa, 0x00);
        i.setHeap(0xab, 0x01);
        i.start();

        assertEquals(0xff, i.getHeap(0xaa));
        assertEquals(0x01, i.getHeap(0xab));
        assertEquals(2, i.instructionsExecuted());
    }
}