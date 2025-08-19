package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartyFlagTest {
    @Test
    public void setFlag() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x5f, // SetPartyFlag
                (byte) 0x4c, //   chardata base offset
                (byte) 0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setAL((0x01 << 3) | 0x04);
        i.heap(0x06).write(0x03);
        i.heap(0x0d).write(0x02);
        i.start(0, 0);

        assertEquals(0x08, i.memory().read(Interpreter.PARTY_SEGMENT, 0x024d, 1));
    }

    @Test
    public void clearFlag() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x60, // ClearPartyFlag
                (byte) 0x4c, //  chardata base offset
                (byte) 0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setAL((0x01 << 3) | 0x04);
        i.heap(0x06).write(0x03);
        i.heap(0x0d).write(0x02);
        i.memory().write(Interpreter.PARTY_SEGMENT, 0x024d, 1, 0xff);
        i.start(0, 0);

        assertEquals(0xf7, i.memory().read(Interpreter.PARTY_SEGMENT, 0x024d, 1));
    }

    @Test
    public void testFlagSign() {
        final Chunk program = new Chunk(List.of(
                (byte) 0x61, // TestPartyFlag
                (byte) 0x4c, //  chardata base offset
                (byte) 0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setAL((0x01 << 3) | 0x00);
        i.heap(0x06).write(0x03);
        i.heap(0x0d).write(0x02);
        i.memory().write(Interpreter.PARTY_SEGMENT, 0x024d, 1, 0xff);
        i.start(0, 0);

        assertTrue(i.getSignFlag());
        assertFalse(i.getZeroFlag());
    }
}