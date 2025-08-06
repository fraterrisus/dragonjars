package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CallAndReturnTest {
    final Chunk PROGRAM = new Chunk(List.of(
            (byte)0x01, // SetNarrow
            (byte)0x53, // Call
            (byte)0x05, //   target (lo)
            (byte)0x00, //   target (hi)
            (byte)0x5a, // Exit
            (byte)0x09, // LoadAXImm
            (byte)0xff, //   immediate
            (byte)0x54  // Return
    ));

    @Test
    public void roundTrip() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM), 0, 0);
        i.setAL(0x00);
        i.start();

        assertEquals(0xff, i.getAL());
        assertEquals(5, i.instructionsExecuted());
        assertEquals(0x04, i.getIP().offset());
    }
}