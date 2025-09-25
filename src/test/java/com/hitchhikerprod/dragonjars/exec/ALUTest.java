package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private void subHelper(boolean wide, int a, int b, int expected, boolean carry, boolean zero, boolean sign) {
        ALU.Result result = (wide) ? ALU.subWord(a, b) : ALU.subByte(a, b);
        assertEquals(expected, result.value(), "Result doesn't match");
        assertEquals(carry, result.carry(), "Carry flag doesn't match");
        assertEquals(zero, result.zero(), "Zero flag doesn't match");
        assertEquals(sign, result.sign(), "Sign flag doesn't match");
    }

    // These test cases have been confirmed in the emulator.

    @Test public void emulateSubtract1() {
        subHelper(false, 0x05, 0x08, 0xfd, true, false, true);
        subHelper(true, 0x05, 0x08, 0xfffd, true, false, true);
    }
    @Test public void emulateSubtract2() {
        subHelper(false, 0x03, 0x03, 0x00, false, true, false);
    }
    @Test public void emulateSubtract3() {
        subHelper(false, 0x90, 0x80, 0x10, false, false, false);
    }
    @Test public void emulateSubtract4() {
        subHelper(false, 0x01, 0xff, 0x02, true, false, false);
        subHelper(true, 0x01, 0xffff, 0x02, true, false, false);
    }
    @Test public void emulateSubtract5() {
        subHelper(false, 0xfa, 0xfc, 0xfe, true, false, true);
        subHelper(true, 0xfffa, 0xfffc, 0xfffe, true, false, true);
    }
    @Test public void emulateSubtract6() {
        subHelper(false, 0xfc, 0xfc, 0x00, false, true, false);
        subHelper(true, 0xfffc, 0xfffc, 0x00, false, true, false);
    }
    @Test public void emulateSubtract7() {
        subHelper(false, 0xfe, 0xfc, 0x02, false, false, false);
        subHelper(true, 0xfffe, 0xfffc, 0x02, false, false, false);
    }
}
