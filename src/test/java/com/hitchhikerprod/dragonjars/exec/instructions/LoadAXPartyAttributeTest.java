package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadAXPartyAttributeTest {
    private static final Chunk PROGRAM = new Chunk(List.of(
            (byte) 0x5d, // LoadAXPartyAttribute
            (byte) 0x1e, //   character attribute offset
            (byte) 0x5a  // Exit
    ));

    private static final Chunk CODE = new Chunk(List.of());

    @Test
    public void wide() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, CODE)).init();
        i.setWidth(true);
        // value to retrieve
        i.memory().write(Interpreter.PARTY_SEGMENT, 0x21e, 2, 0xf1c3);
        i.heap(0x06).write(0x02); // character ID
        i.start(0, 0);

        assertEquals(0xf1c3, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(null, List.of(PROGRAM, CODE)).init();
        i.memory().write(Interpreter.PARTY_SEGMENT, 0x21e, 2, 0xf1c3);
        i.heap(0x06).write(0x02); // character ID
        i.start(0, 0);

        assertEquals(0x00c3, i.getAX(true));
        assertEquals(2, i.instructionsExecuted());
    }
}