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
        i.setWidth(true);
        i.heap(0x74).write(0xff);
        i.heap(0x75).write(0x01);
        i.start(0, 0);

        assertEquals(0x00, i.heap(0x74).read());
        assertEquals(0x02, i.heap(0x75).read());
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(false);
        i.heap(0x74).write(0xff);
        i.heap(0x75).write(0x01);
        i.start(0, 0);

        assertEquals(0x00, i.heap(0x74).read());
        assertEquals(0x01, i.heap(0x75).read());
        assertEquals(2, i.instructionsExecuted());
        assertEquals(PROGRAM.getSize() - 1, i.getIP().offset());
    }
}