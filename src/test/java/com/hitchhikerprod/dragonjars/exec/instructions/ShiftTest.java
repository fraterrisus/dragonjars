package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShiftTest {
    @Test
    public void leftAXNarrow() {
        final Interpreter i = new Interpreter(null, List.of(), 0, 0);
        i.setWidth(false);
        i.setAH(0xff);
        i.setAL(0x79);

        final Address newIP = new LeftShiftAX().exec(i);

        // old: 1111 1111 0111 1001
        // new: 0000 0000 1111 0010
        assertEquals(0x00f2, i.getAX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void leftAXWide() {
        final Interpreter i = new Interpreter(null, List.of(), 0, 0);
        i.setWidth(true);
        i.setAH(0x40);
        i.setAL(0x81);

        new LeftShiftAX().exec(i);

        // old: 0100 0000 1000 0001
        // new: 1000 0001 0000 0010
        assertEquals(0x8102, i.getAX(true));
    }

    @Test
    public void rightAX() {
        final Interpreter i = new Interpreter(null, List.of(), 0, 0);
        i.setAH(0xf1);
        i.setAL(0x13);
        i.setWidth(false); // red herring, ignored

        final Address newIP = new RightShiftAX().exec(i);

        // old: 1111 0001 0001 0011
        // new: 0111 1000 1000 1001
        assertEquals(0x7889, i.getAX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void leftBL() {
        final Interpreter i = new Interpreter(null, List.of(), 0, 0);
        i.setBH(0xff);
        i.setBL(0x44);
        i.setWidth(false); // red herring, ignored

        final Address newIP = new LeftShiftBL().exec(i);

        // old: 1111 1111 0100 0100
        // new: 1111 1111 1000 1000
        assertEquals(0xff88, i.getBX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void rightBL() {
        final Interpreter i = new Interpreter(null, List.of(), 0, 0);
        i.setBH(0xff);
        i.setBL(0x44);
        i.setWidth(false); // red herring, ignored

        final Address newIP = new RightShiftBL().exec(i);

        // old: 1111 1111 0100 0100
        // new: 1111 1111 0010 0010
        assertEquals(0xff22, i.getBX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void rightHeapWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x2c, // ShiftRightHeap
                (byte)0x03, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(true);
        i.setHeap(0x03, 0x4181);
        i.start();

        // old: 0100 0001 1000 0001
        // new: 0010 0000 1100 0000
        assertEquals(0x20c0, i.getHeapWord(0x03));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void rightHeapNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x2c, // ShiftRightHeap
                (byte)0x03, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(false);
        i.setHeap(0x03, 0x81);
        i.setHeap(0x04, 0x41);
        i.start();

        // old: 0100 0001 1000 0001  bit[8] is shifted to [7], but the high byte
        // new: 0100 0001 1100 0000  doesn't get written back
        assertEquals(0x41c0, i.getHeapWord(0x03));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void leftHeapWide() {
        final Chunk program = new Chunk(List.of(
                (byte)0x29, // ShiftLeftHeap
                (byte)0x03, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(true);
        i.setHeap(0x03, 0x4181);
        i.start();

        // old: 0100 0001 1000 0001
        // new: 1000 0011 0000 0010
        assertEquals(0x8302, i.getHeapWord(0x03));
        assertEquals(2, i.instructionsExecuted());
    }

    @Test
    public void leftHeapNarrow() {
        final Chunk program = new Chunk(List.of(
                (byte)0x29, // ShiftLeftHeap
                (byte)0x03, //   heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program), 0, 0);
        i.setWidth(false);
        i.setHeap(0x03, 0xc1);
        i.setHeap(0x04, 0x41);
        i.start();

        // old: 0100 0001 1100 0001
        // new: 0100 0001 1000 0010
        assertEquals(0x4182, i.getHeapWord(0x03));
        assertEquals(2, i.instructionsExecuted());
    }
}