package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IsPartyInBoxTest {
    record Rectangle(int x0, int y0, int x1, int y1) {}
    record Point(int x, int y) {}

    @Test
    public void inside() {
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x13, 0x04), true);
    }

    @Test
    public void outside() {
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x05, 0x04), false);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x1f, 0x04), false);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x13, 0x00), false);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x13, 0x10), false);
    }

    @Test
    public void onBoundary() {
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x10, 0x04), true);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x14, 0x04), true);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x13, 0x02), true);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x13, 0x06), true);
        helper(new Rectangle(0x10, 0x02, 0x14, 0x06), new Point(0x10, 0x02), true);
    }

    private void helper(Rectangle bounds, Point party, boolean expected) {
        final Chunk program = new Chunk(List.of(
                (byte)0x6a, // IsPartyInBox
                (byte)bounds.y0(),
                (byte)bounds.x0(),
                (byte)bounds.y1(),
                (byte)bounds.x1(),
                (byte)0x5a // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.heap(Heap.PARTY_Y).write(party.y());
        i.heap(Heap.PARTY_X).write(party.x());
        i.start(0, 0);

        assertEquals(expected, i.getZeroFlag());
        assertEquals(2, i.instructionsExecuted());
    }
}