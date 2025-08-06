package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncHeapTest {
    public static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x23, // IncHeap
            (byte) 0x74, //   heap index
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM), 0, 0);
        i.setHeap(0x74, 0xff);
        i.setHeap(0x75, 0x01);
        i.setWidth(true);

        i.start();

        i.setWidth(false);
        assertEquals(0x00, i.getHeap(0x74));
        assertEquals(0x02, i.getHeap(0x75));
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM), 0, 0);
        i.setHeap(0x74, 0xff);
        i.setHeap(0x75, 0x01);

        i.start();

        assertEquals(0x00, i.getHeap(0x74));
        assertEquals(0x01, i.getHeap(0x75));
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }
}