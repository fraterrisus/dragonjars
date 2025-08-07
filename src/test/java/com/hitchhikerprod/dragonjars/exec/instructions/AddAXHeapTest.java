package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AddAXHeapTest {
    private void helper(boolean wide, int ax, int heap, int total, boolean carryOut) {
        final Chunk program = new Chunk(List.of(
                (byte)0x2f, // AddAXHeap
                (byte)0x81, // heapIndex
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(wide);
        i.setAX(ax);
        i.setHeap(0x81, heap);
        i.start();

        assertEquals(carryOut, i.getCarryFlag());
        assertEquals(total, i.getAX());
        assertEquals(2, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void wide() {
        helper(true, 0x1111, 0xbbaa, 0xccbb, false);
    }

    @Test
    public void wideWithCarryOut() {
        helper(true, 0xaaaa, 0xbbbb, 0x6665, true);
    }

    @Test
    public void narrow() {
        helper(false, 0x22, 0x33, 0x55, false);
    }

    @Test
    public void narrowWithCarryOut() {
        helper(false, 0x00aa, 0x00bb, 0x0065, true);
    }
}