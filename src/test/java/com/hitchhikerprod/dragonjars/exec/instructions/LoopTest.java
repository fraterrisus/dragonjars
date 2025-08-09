package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoopTest {
    @Test
    public void downwards() {
        final Chunk program = new Chunk(List.of(
                (byte)0x4d, // RandomAX
                (byte)0x49, // LoopBX
                (byte)0x00, //   target (lo)
                (byte)0x00, //   target (hi)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setBL(0x03); // runs loop four times
        i.start();

        assertEquals(0xff, i.getBL());
        assertEquals(9, i.instructionsExecuted());
    }

    @Test
    public void upwards() {
        final Chunk program = new Chunk(List.of(
                (byte)0x4d, // RandomAX
                (byte)0x4a, // LoopBXLimit
                (byte)0x03, //   limit
                (byte)0x00, //   target (lo)
                (byte)0x00, //   target (hi)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setBL(0x00);
        i.start();

        assertEquals(0x03, i.getBL());
        assertEquals(7, i.instructionsExecuted());
    }
}