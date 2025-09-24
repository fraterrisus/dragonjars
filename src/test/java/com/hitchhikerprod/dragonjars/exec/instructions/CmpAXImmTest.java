package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CmpAXImmTest {
    @Test
    public void above() {
        helper(false, 0x97, 0x96, true, false, false);
    }

    @Test
    public void equal() {
        helper(false, 0x96, 0x96, true, false, true);
    }

    @Test
    public void below() {
        helper(false, 0x95, 0x96, false, true, false);
    }

    private void helper(boolean width, int ax, int immediate, boolean carry, boolean sign, boolean zero) {
        final List<Byte> programBytes = new ArrayList<>();
        programBytes.add((byte)0x3e); // CmpAXImm
        programBytes.add((byte)(immediate & 0xff));
        if (width) {
            programBytes.add((byte)((immediate >> 8) & 0xff));
        }
        programBytes.add((byte)0x1e); // Exit

        final Chunk program = new Chunk(programBytes);
        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(width);
        i.setAX(ax);
        i.start(0, 0);

        assertEquals(carry, i.getCarryFlag(), "Carry flag is wrong"); // because 97 > 96 but carry is flipped
        assertEquals(sign, i.getSignFlag(), "Sign flag is wrong");
        assertEquals(zero, i.getZeroFlag(), "Zero flag is wrong");
    }
}