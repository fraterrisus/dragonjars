package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class XorAXTest {
    @Test
    public void xorAXHeapWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x3b, // XorAXHeap
                (byte)0x1a, //   heap index
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(true);
        Heap.get(0x1a).write(0xff);
        Heap.get(0x1b).write(0xff);
        i.setAL(0xff);
        i.setAH(0xff);
        i.start(0, 0);

        assertEquals(0x0000, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void xorAXHeapNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x3b, // XorAXHeap
                (byte)0x1a, //   heap index
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(false);
        Heap.get(0x1a).write(0x5a);
        Heap.get(0x1b).write(0xc3);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0x00ff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void xorAXImmWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x3c, // XorAXImm
                (byte)0x5a, //   immediate (lo)
                (byte)0xc3, //   immediate (hi)
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(true);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0xffff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void xorAXImmNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x3c, // XorAXImm
                (byte)0x5a, //   immediate (lo)
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(false);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0x3cff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }
}
