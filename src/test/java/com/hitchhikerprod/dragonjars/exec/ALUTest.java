package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ALUTest {
    @Test
    public void signExtendPositive() {
        assertEquals(0x0000007f, ALU.signExtend(0x0000007f, 1));
        assertEquals(0x00007fff, ALU.signExtend(0x00007fff, 2));
        assertEquals(0x007fffff, ALU.signExtend(0x007fffff, 3));
        assertEquals(0x7fffffff, ALU.signExtend(0x7fffffff, 4));
    }

    @Test
    public void signExtendNegative() {
        assertEquals(0xffffff80, ALU.signExtend(0x00000080, 1));
        assertEquals(0xffff8000, ALU.signExtend(0x00008000, 2));
        assertEquals(0xff800000, ALU.signExtend(0x00800000, 3));
        assertEquals(0x80000000, ALU.signExtend(0x80000000, 4));
    }

    // These four test cases have been confirmed in the emulator, ignoring the CF flip.

    @Test
    public void emulateSubtract1() {
        ALU.Result result = ALU.subByte(0x05, 0x08);
        assertEquals(0xfd, result.value());
        assertTrue(result.carry());
        assertFalse(result.zero());
        assertTrue(result.sign());
    }

    @Test
    public void emulateSubtract2() {
        ALU.Result result = ALU.subByte(0x03, 0x03);
        assertEquals(0x00, result.value());
        assertFalse(result.carry());
        assertTrue(result.zero());
        assertFalse(result.sign());
    }

    @Test
    public void emulateSubtract3() {
        ALU.Result result = ALU.subByte(0x90, 0x80);
        assertEquals(0x10, result.value());
        assertFalse(result.carry());
        assertFalse(result.zero());
        assertFalse(result.sign());
    }

    @Test
    public void emulateSubtract4() {
        ALU.Result result = ALU.subByte(0x01, 0xff);
        assertEquals(0x02, result.value());
        assertTrue(result.carry());
        assertFalse(result.zero());
        assertFalse(result.sign());
    }
}
