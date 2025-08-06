package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoveDataTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x1b, // MoveData
            (byte) 0x04, //   src addr (lo)
            (byte) 0x00, //   src addr (hi)
            (byte) 0x06, //   dst addr (lo)
            (byte) 0x00, //   dst addr (hi)
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xaa, (byte) 0xbb, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data), 0, 0);
        i.setDS(0x01);
        i.setWidth(true);
        i.start();

        assertEquals(0xbbaa, i.readWord(0x01, 0x06));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final ModifiableChunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0xaa, (byte) 0xbb, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(null, List.of(PROGRAM, data), 0, 0);
        i.setDS(0x01);
        i.setWidth(false);
        i.start();

        assertEquals(0x00aa, i.readWord(0x01, 0x06));
        assertEquals(2, i.instructionsExecuted());
    }

}