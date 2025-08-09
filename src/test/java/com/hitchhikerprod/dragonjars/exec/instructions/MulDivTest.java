package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MulDivTest {
    @Test
    public void divAXHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x35, // DivAXHeap
                (byte)0x10, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setAL(0x11);
        i.setAH(0x11);
        i.setHeapBytes(0x10, 4, 0x12345678);
        i.start();
        assertEquals(0x00011112, i.getHeapBytes(0x37, 4));
        assertEquals(0x0246, i.getHeapBytes(0x3b, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void divAXImmWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x36, // DivAXImm
                (byte)0x22, //   immediate (lo)
                (byte)0x11, //   immediate (hi)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(true);
        i.setAL(0xff);
        i.setAH(0xff);
        i.start();
        assertEquals(0x0000000e, i.getHeapBytes(0x37, 4));
        assertEquals(0x1023, i.getHeapBytes(0x3b, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void divAXImmNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x36, // DivAXImm
                (byte)0x22, //   immediate (lo)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(false);
        i.setAL(0xff);
        i.setAH(0xff);
        i.start();
        assertEquals(0x00000787, i.getHeapBytes(0x37, 4));
        assertEquals(0x0011, i.getHeapBytes(0x3b, 2));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void mulAXHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x33, // MulAXHeap
                (byte)0x10, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setAL(0x0a);
        i.setAH(0x00);
        i.setHeapBytes(0x10, 4, 0x00000020);
        i.start();
        assertEquals(0x00000140, i.getHeapBytes(0x37, 4));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void mulAXImmWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x34, // MulAXImm
                (byte)0x1a, //   immediate (lo)
                (byte)0x42, //   immediate (hi)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(true);
        i.setAL(0x11);
        i.setAH(0x11);
        i.start();
        assertEquals(0x04681dba, i.getHeapBytes(0x37, 4));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void mulAXImmNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x34, // MulAXImm
                (byte)0x1a, //   immediate (lo)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(false);
        i.setAL(0x11);
        i.setAH(0x11); // AX is always read on mul, even in narrow mode
        i.start();
        assertEquals(0x0001bbba, i.getHeapBytes(0x37, 4));
        assertEquals(2, i.instructionsExecuted());
    }

}
