package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncDecAXTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void incWide() {
        final Interpreter i = new Interpreter(null, List.of(EMPTY), 0, 0);
        i.setWidth(true);
        i.setAX(0x02ff);

        final Instruction uut = new IncAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x0300, i.getAX());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void incNarrow() {
        final Interpreter i = new Interpreter(null, List.of(EMPTY), 0, 0);
        i.setWidth(true);
        i.setAX(0x02ff);
        i.setWidth(false);

        final Instruction uut = new IncAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x0000, i.getAX());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void decNarrow() {
        final Interpreter i = new Interpreter(null, List.of(EMPTY), 0, 0);
        i.setWidth(false);
        i.setAH(0x43); // writes 0x00 because narrow
        i.setAL(0x00); // 0x00 - 1 = 0xff
        final Instruction uut = new DecAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x00ff, i.getAX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void decWide() {
        final Interpreter i = new Interpreter(null, List.of(EMPTY), 0, 0);
        i.setWidth(true);
        i.setAH(0x43);
        i.setAL(0x00);
        final Instruction uut = new DecAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x42ff, i.getAX(true));
        assertEquals(0, newIP.offset());
    }

}