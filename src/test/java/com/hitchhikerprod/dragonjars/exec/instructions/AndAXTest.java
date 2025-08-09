package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AndAXTest {
    @Test
    public void andAXHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x37, // AndAXHeap
                (byte)0x1a, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(false);
        i.setHeap(0x1a, 0x7c);
        i.setHeap(0x1b, 0x81);
        i.setAL(0xff);
        i.setAH(0xff);
        i.start();

        assertEquals(0x817c, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void andAXImmNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x38, // AndAXHeap
                (byte)0x77, //   immediate
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(false);
        i.setAL(0xff);
        i.setAH(0xff);
        i.start();

        assertEquals(0xff77, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void andAXImmWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x38, // AndAXHeap
                (byte)0x77, //   immediate
                (byte)0x77, //   immediate
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(true);
        i.setAL(0xff);
        i.setAH(0xff);
        i.start();

        assertEquals(0x7777, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }
}