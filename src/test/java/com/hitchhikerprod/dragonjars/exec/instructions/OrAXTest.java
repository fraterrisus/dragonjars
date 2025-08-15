package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrAXTest {
    @Test
    public void orAXHeapWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x39, // OrAXHeap
                (byte)0x1a, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program));
        i.setWidth(true);
        i.setHeap(0x1a, 0x5a);
        i.setHeap(0x1b, 0xc3);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0xffff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void orAXHeapNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x39, // OrAXHeap
                (byte)0x1a, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program));
        i.setWidth(false);
        i.setHeap(0x1a, 0x5a);
        i.setHeap(0x1b, 0xc3);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0x00ff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void orAXImmWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x3a, // OrAXImm
                (byte)0x5a, //   immediate (lo)
                (byte)0xc3, //   immediate (hi)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program));
        i.setWidth(true);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0xffff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void orAXImmNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x3a, // OrAXImm
                (byte)0x5a, //   immediate (lo)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program));
        i.setWidth(false);
        i.setAL(0xa5);
        i.setAH(0x3c);
        i.start(0, 0);

        assertEquals(0x3cff, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }
}
