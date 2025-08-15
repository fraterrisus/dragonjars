package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubAXImmTest {
    public void helper(boolean wide, int ax, int imm, int result, boolean carry, boolean zero, boolean sign) {
        final List<Byte> instructions = new ArrayList<>();
        instructions.add((byte)0x32); // SubAXImm
        instructions.add((byte)(imm & 0xff));
        if (wide) instructions.add((byte)((imm >> 8)& 0xff));
        instructions.add((byte)0x5a); // Exit
        final Chunk program = new Chunk(instructions);
        final Interpreter i = new Interpreter(null, List.of(program));
        i.setWidth(wide);
        i.setAX(ax);
        i.start(0, 0);

        assertEquals(2, i.instructionsExecuted(), "Wrong number of instructions");
        assertEquals(result, i.getAX(), "Incorrect result");
        assertEquals(carry, i.getCarryFlag(), "Incorrect CF");
        assertEquals(zero, i.getZeroFlag(), "Incorrect ZF");
        assertEquals(sign, i.getSignFlag(), "Incorrect SF");
    }

    // Remember that CF gets flipped so the "expected" result is the opposite of what
    // the actual math should produce (i.e. 6 minus 3 doesn't borrow but CF=1)

    @Test
    public void wide() {
        helper(true, 0x0fff, 0x00f0, 0x0f0f, true, false, false);
    }

    @Test
    public void wideSign() {
        helper(true, 0xf005, 0x0001, 0xf004, true, false, true);
    }

    @Test
    public void wideZero() {
        helper(true, 0xabcd, 0xabcd, 0x0000, true, true, false);
    }

    @Test
    public void wideOverflow() {
        helper(true, 0x1003, 0x1006, 0xfffd, false, false, true);
    }

    @Test
    public void narrow() {
        helper(false, 0x06, 0x04, 0x02, true, false, false);
    }

    @Test
    public void narrowSign() {
        helper(false, 0xff, 0x03, 0xfc, true, false, true);
    }

    @Test
    public void narrowZero() {
        helper(false, 0x80, 0x80, 0x00, true, true, false);
    }

    @Test
    public void narrowOverflow() {
        helper(false, 0x03, 0x06, 0xfd, false, false, true);
    }
}