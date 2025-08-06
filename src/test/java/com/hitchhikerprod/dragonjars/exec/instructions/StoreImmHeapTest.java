package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreImmHeapTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x1a, // StoreAXHeapImm
                (byte)0x3a, // heap index
                (byte)0xaa, // immediate value (2B)
                (byte)0xbb,
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);

        i.start();

        assertEquals(0x0000bbaa, i.getHeapWord(0x3a));
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x1a, // StoreAXHeapImm
                (byte)0x3a, // heap index
                (byte)0xaa, // immediate value (1B)
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);

        i.start();

        assertEquals(0x000000aa, i.getHeapWord(0x3a));
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}