package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class PushAXTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x56, // PushAX
            (byte) 0x5a  // Exit
    ));

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM));
        i.setAH(0x81);
        i.setAL(0xdd);
        i.setWidth(true);
        i.start(0, 0);

        assertEquals(0x81, i.pop());
        assertEquals(0xdd, i.pop());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM));
        i.setAH(0x81);
        i.setAL(0xdd);
        i.setWidth(false);
        i.start(0, 0);

        assertEquals(0xdd, i.pop());
        assertThrows(NoSuchElementException.class, i::pop);
        assertEquals(2, i.instructionsExecuted());
    }
}