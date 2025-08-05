package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncAXTest {
    @Test
    public void wide() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.setWidth(true);
        i.setAX(0x02ff);

        final Instruction uut = new IncAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x0300, i.getAX());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void narrow() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.setWidth(true);
        i.setAX(0x02ff);
        i.setWidth(false);

        final Instruction uut = new IncAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x0000, i.getAX());
        assertEquals(0, newIP.offset());
    }
}