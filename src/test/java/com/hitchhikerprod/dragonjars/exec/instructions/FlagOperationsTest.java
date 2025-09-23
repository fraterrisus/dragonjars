package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlagOperationsTest {
    @Test
    public void setAL() {
        final Chunk program = new Chunk(List.of(
                (byte)0x4e, // FlagSetAL
                (byte)0x40, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setAL((0x03 << 3) | (0x4)); // 0x80 >> 4 = 0x08
        i.start(0, 0);
        // heap index 0x10 + 0x03
        // mask 0x80 >> 4 = 0000_1000 = 0x08
        assertEquals(0x08, Heap.get(0x43).read());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void clearAL() {
        final Chunk program = new Chunk(List.of(
                (byte)0x4f, // FlagClearAL
                (byte)0x02, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        Heap.get(0x12).write(0xff);
        i.setAL((0x10 << 3) | (0x3));
        i.start(0, 0);
        // heap index 0x10 + 0x02
        // mask 0x80 >> 3 = 0001_0000 = 0x10
        // value 1110_1111 = 0xef
        assertEquals(0xef, Heap.get(0x12).read());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void testALNotZero() {
        final Chunk program = new Chunk(List.of(
                (byte)0x50, // FlagTestAL
                (byte)0x15, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        Heap.get(0x25).write(0x02);
        i.setAL((0x10 << 3) | (0x6));
        i.start(0, 0);
        // heap index 0x10 + 0x15
        // mask 0x80 >> 6 = 0000_0010 = 0x02
        assertFalse(i.getCarryFlag());
        assertFalse(i.getZeroFlag());
        assertFalse(i.getSignFlag());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void testALZero() {
        final Chunk program = new Chunk(List.of(
                (byte)0x50, // FlagTestAL
                (byte)0x15, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        Heap.get(0x25).write(0xfd);
        i.setAL((0x10 << 3) | (0x6));
        i.start(0, 0);
        // heap index 0x10 + 0x15
        // mask 0x80 >> 6 = 0000_0010 = 0x02
        //                  1111_1101 = 0xfd
        assertFalse(i.getCarryFlag());
        assertTrue(i.getZeroFlag());
        assertFalse(i.getSignFlag());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void setImm() {
        final int val = (0x03 << 3) | 0x4;
        final Chunk program = new Chunk(List.of(
                (byte)0x9b, // FlagSetImm
                (byte)val,  //   flag index
                (byte)0x40, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.start(0, 0);
        // heap index 0x10 + 0x03
        // mask 0x80 >> 4 = 0000_1000 = 0x08
        assertEquals(0x08, Heap.get(0x43).read());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void clearImm() {
        final int val = (0x10 << 3) | 0x3;
        final Chunk program = new Chunk(List.of(
                (byte)0x9c, // FlagClearImm
                (byte)val,  //   flag index
                (byte)0x02, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        Heap.get(0x12).write(0xff);
        i.start(0, 0);
        // heap index 0x10 + 0x02
        // mask 0x80 >> 3 = 0001_0000 = 0x10
        // value 1110_1111 = 0xef
        assertEquals(0xef, Heap.get(0x12).read());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void testImmNotZero() {
        final int val = ((0x10 << 3) | (0x6));
        final Chunk program = new Chunk(List.of(
                (byte)0x9d, // FlagTestImm
                (byte)val,  //   flag index
                (byte)0x15, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        Heap.get(0x25).write(0x02);
        i.start(0, 0);
        // heap index 0x10 + 0x15
        // mask 0x80 >> 6 = 0000_0010 = 0x02
        assertFalse(i.getCarryFlag());
        assertFalse(i.getZeroFlag());
        assertFalse(i.getSignFlag());
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void testImmZero() {
        final int val = ((0x10 << 3) | (0x6));
        final Chunk program = new Chunk(List.of(
                (byte)0x9d, // FlagTestImm
                (byte)val,  //   flag index
                (byte)0x15, //   heap offset
                (byte)0x1e  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        Heap.get(0x25).write(0xfd);
        i.start(0, 0);
        // heap index 0x10 + 0x15
        // mask 0x80 >> 6 = 0000_0010 = 0x02
        //                  1111_1101 = 0xfd
        assertFalse(i.getCarryFlag());
        assertTrue(i.getZeroFlag());
        assertFalse(i.getSignFlag());
        assertEquals(2, i.instructionsExecuted());
    }}