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
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.writeHeap(0x74, 0xff);
        i.writeHeap(0x75, 0x01);
        i.setWidth(true);
        i.start(0, 0);

        i.setWidth(false);
        assertEquals(0x00, i.heap().read(0x74, 1));
        assertEquals(0x02, i.heap().read(0x75, 1));
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.writeHeap(0x74, 0xff);
        i.writeHeap(0x75, 0x01);
        i.start(0, 0);

        assertEquals(0x00, i.heap().read(0x74, 1));
        assertEquals(0x01, i.heap().read(0x75, 1));
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }
}