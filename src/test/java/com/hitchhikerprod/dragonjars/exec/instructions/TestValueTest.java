package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestValueTest {
    public static final Chunk HEAP_PROGRAM = new Chunk(List.of(
            (byte) 0x66, // TestHeap
            (byte) 0x1a, //   heap index
            (byte) 0x5a  // Exit
    ));

    public static final Chunk AX_PROGRAM = new Chunk(List.of(
            (byte) 0x99, // TestAX
            (byte) 0x5a  // Exit
    ));

    private void heapHelper(boolean width, int heap, boolean zero, boolean sign) {
        final Interpreter i = new Interpreter(null, List.of(HEAP_PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(width);
        Heap.get(0x1a).write(heap, 2);
        i.start(0, 0);

        assertEquals(zero, i.getZeroFlag(), "Zero flag is wrong");
        assertEquals(sign, i.getSignFlag(), "Sign flag is wrong");
        assertEquals(2, i.instructionsExecuted(), "Wrong number of instructions executed");
    }

    private void axHelper(boolean width, int ax, boolean zero, boolean sign) {
        final Interpreter i = new Interpreter(null, List.of(AX_PROGRAM, Chunk.EMPTY)).init();
        i.setWidth(width);
        i.setAX(ax, true);
        i.start(0, 0);

        assertEquals(zero, i.getZeroFlag(), "Zero flag is wrong");
        assertEquals(sign, i.getSignFlag(), "Sign flag is wrong");
        assertEquals(2, i.instructionsExecuted(), "Wrong number of instructions executed");
    }

    @Test
    public void heapWide() {
        heapHelper(true, 0x1234, false, false);
    }

    @Test
    public void heapWideZero() {
        heapHelper(true, 0x0000, true, false);
    }

    @Test
    public void heapWideSign() {
        heapHelper(true, 0x8000, false, true);
    }

    @Test
    public void heapNarrow() {
        heapHelper(false, 0x0012, false, false);
    }

    @Test
    public void heapNarrowZero() {
        heapHelper(false, 0x0000, true, false);
    }

    @Test
    public void heapNarrowSign() {
        heapHelper(false, 0x0080, false, true);
    }

    @Test
    public void axWide() {
        axHelper(true, 0x1234, false, false);
    }


    @Test
    public void axWideZero() {
        axHelper(true, 0x0000, true, false);
    }

    @Test
    public void axWideSign() {
        axHelper(true, 0x8000, false, true);
    }

    @Test
    public void axNarrow() {
        axHelper(false, 0x0012, false, false);
    }

    @Test
    public void axNarrowZero() {
        axHelper(false, 0x0000, true, false);
    }

    @Test
    public void axNarrowSign() {
        axHelper(false, 0x0080, false, true);
    }
}