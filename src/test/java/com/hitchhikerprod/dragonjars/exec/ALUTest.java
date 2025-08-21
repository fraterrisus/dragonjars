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
}
