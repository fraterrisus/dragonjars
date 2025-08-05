package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StoreAXHeapOffsetTest {
    @Test
    public void wide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x13, // StoreAXHeapOffset
                (byte)0x3a, // heap index
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setWidth(true);
        i.setAX(0x1234);
        i.setBL(0x02);

        i.start();

        assertEquals(0x00001234, i.getHeapWord(0x3c));
        assertEquals(2, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void narrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x13, // StoreAXHeapOffset
                (byte)0x3a, // heap index
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setWidth(true);
        i.setAX(0x1234);
        i.setBL(0x02);
        i.setWidth(false);

        i.start();

        assertEquals(0x00000034, i.getHeapWord(0x3c));
        assertEquals(2, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}