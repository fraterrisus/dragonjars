package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PopAXTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x55, // PopAX
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.pushByte(0x7d);
        i.pushByte(0xf3);
        i.setWidth(true);
        i.start(0, 0);

        assertEquals(0xf37d, i.getAX());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, Chunk.EMPTY)).init();
        i.pushByte(0x7d);
        i.pushByte(0xf3);
        i.setWidth(false);
        i.start(0, 0);

        assertEquals(0x00f3, i.getAX());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void roundTripWide() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x00, // SetWide
                (byte) 0x09, // LoadAXImm
                (byte) 0xbb, //   immediate (lo)
                (byte) 0xaa, //   immediate (hi)
                (byte) 0x56, // PushAX
                (byte) 0x09, // LoadAXImm
                (byte) 0x00, //   immediate (lo)
                (byte) 0x00, //   immediate (hi)
                (byte) 0x55, // PopAX
                (byte) 0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.start(0, 0);

        assertEquals(0xaabb, i.getAX());
        assertEquals(6, i.instructionsExecuted());
    }
}