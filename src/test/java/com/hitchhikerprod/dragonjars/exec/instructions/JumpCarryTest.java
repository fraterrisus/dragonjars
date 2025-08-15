package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JumpCarryTest {
    @Test
    public void taken() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x4b, // SetCarry
                (byte)0x41, // JumpCarry
                (byte)0x06, //   target (lo)
                (byte)0x00, //   target (hi)
                (byte)0x55, // PopAX (skipped over)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program));
        i.setAL(0x00);
        i.push(0xff);
        i.start(0, 0);

        assertEquals(0x00, i.getAL());
        assertEquals(4, i.instructionsExecuted());
    }

    @Test
    public void notTaken() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x4c, // ClearCarry
                (byte)0x41, // JumpCarry
                (byte)0x06, //   target (lo)
                (byte)0x00, //   target (hi)
                (byte)0x55, // PopAX
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program));
        i.setAL(0x00);
        i.push(0xff);
        i.start(0, 0);

        assertEquals(0xff, i.getAL());
        assertEquals(5, i.instructionsExecuted());
    }
}