package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadHeapOnesTest {
    public static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x9a, // LoadHeapOnes
            (byte) 0x48, //   heap index
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(true);
        i.heap(0x48).write(0x0000, 2);
        i.start(0, 0);
        assertEquals(0xffff, i.heap(0x48).read(2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(false);
        i.heap(0x48).write(0x0000, 2);
        i.start(0, 0);
        assertEquals(0x00ff, i.heap(0x48).read(2));
        assertEquals(2, i.instructionsExecuted());
    }

}