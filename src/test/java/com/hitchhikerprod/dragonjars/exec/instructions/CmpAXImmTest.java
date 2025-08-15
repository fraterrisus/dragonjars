package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CmpAXImmTest {
    @Test
    public void above() {
        helper(0x97, 0x96, true, false, false);
    }

    @Test
    public void equal() {
        helper(0x96, 0x96, true, false, true);
    }

    @Test
    public void below() {
        helper(0x95, 0x96, false, true, false);
    }

    private void helper(int ax, int immediate, boolean carry, boolean sign, boolean zero) {
        final Chunk program = new Chunk(List.of(
                (byte)0x3e, // CmpAXImm
                (byte)immediate, //   immediate value
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setWidth(false);
        i.setAX(ax);
        i.start(0, 0);

        assertEquals(carry, i.getCarryFlag(), "Carry flag is wrong"); // because 97 > 96 but carry is flipped
        assertEquals(sign, i.getSignFlag(), "Sign flag is wrong");
        assertEquals(zero, i.getZeroFlag(), "Zero flag is wrong");
    }
}