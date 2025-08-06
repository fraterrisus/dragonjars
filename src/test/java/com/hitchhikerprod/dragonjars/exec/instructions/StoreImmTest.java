package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreImmTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x1c, // StoreImm
                (byte) 0x06, //   addr (lo)
                (byte) 0x00, //   addr (hi)
                (byte) 0xaa, //   value (lo)
                (byte) 0xbb, //   value (hi)
                (byte) 0x5a  // Exit
        ));

        final Chunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(List.of(program, data), 0, 0);
        i.setDS(0x01);
        i.setWidth(true);
        i.start();

        assertEquals(0xbbaa, i.readWord(0x01, 0x06));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x1c, // StoreImm
                (byte) 0x06, //   addr (lo)
                (byte) 0x00, //   addr (hi)
                (byte) 0xaa, //   value (lo)
                (byte) 0x5a  // Exit
        ));

        final Chunk data = new ModifiableChunk(List.of(
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        ));

        final Interpreter i = new Interpreter(List.of(program, data), 0, 0);
        i.setDS(0x01);
        i.setWidth(false);
        i.start();

        assertEquals(0x00aa, i.readWord(0x01, 0x06));
        assertEquals(2, i.instructionsExecuted());
    }
}