package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LongCallAndReturnTest {
    final Chunk PROGRAM_1 = new Chunk(List.of(
            (byte)0x01, // SetNarrow
            (byte)0x58, // LongCall
            (byte)0x01, //   target (chunk)
            (byte)0x02, //   target (lo)
            (byte)0x00, //   target (hi)
            (byte)0x5a  // Exit
    ));

    final Chunk PROGRAM_2 = new Chunk(List.of(
            (byte)0xff, // padding
            (byte)0xff, // padding
            (byte)0x09, // LoadAXImm
            (byte)0xaa, //   immediate
            (byte)0x59  // LongReturn
    ));

    @Test
    public void roundTrip() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM_1, PROGRAM_2), 0, 0);
        i.setAL(0x00);
        i.start();

        assertEquals(0xaa, i.getAL());
        assertEquals(5, i.instructionsExecuted());
        assertEquals(new Address(0x00, 0x05), i.getIP());
    }
}
