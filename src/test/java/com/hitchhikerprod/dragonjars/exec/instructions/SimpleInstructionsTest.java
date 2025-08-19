package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleInstructionsTest {
    /* Remember that the Interpreter starts at (-1,-1) */
    @Test
    public void copyHeap3E3F() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.heap(0x3e).write(0xff00, 2);

        final Address newIP = Instructions.COPY_HEAP_3E_3F.exec(i);

        assertEquals(0xff, i.heap(0x3e).read());
    }
    
    @Test
    public void exitInstruction() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));

        final Address newIP = Instructions.EXIT.exec(i);

        assertNull(newIP);
    }

    @Test
    public void incBL() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(true);
        i.setBX(0x0000ffff);
        final Instruction uut = new IncBL();

        final Address newIP = uut.exec(i);

        assertEquals(0x0000ff00, i.getBX());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void jump() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x52, // Jump
                (byte)0x05, //   target (lo)
                (byte)0x00, //   target (hi)
                (byte)0x55, // PopAX (skipped over)
                (byte)0x5a  // Exit
        ));
        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setAL(0x00);
        i.push(0xff);
        i.start(0, 0);

        assertEquals(0x00, i.getAL());
        assertEquals(3, i.instructionsExecuted());
    }

    @Test
    public void loadBLHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x05, // LoadBLHeap
                (byte)0x26, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.heap(0x26).write(0xaa);
        i.heap(0x27).write(0xbb);
        i.start(0, 0);

        assertEquals(0x000000aa, i.getBL());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void loadBLImm() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x06, // LoadBLImm
                (byte)0xaa, // immediate word (1B)
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.start(0, 0);

        assertEquals(0x000000aa, i.getBL());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void loadBLZero() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(true);
        i.setBX(0xffff);

        final Instruction uut = new LoadBLZero();
        final Address newIP = uut.exec(i);

        assertEquals(0xff00, i.getBX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void moveALBL() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(true);
        i.setAX(0xffff);
        i.setBX(0x0000);

        final Instruction uut = new MoveALBL();
        final Address newIP = uut.exec(i);

        assertEquals(0x000000ff, i.getBL());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void moveBXAX() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(true);
        i.setAX(0x0000);
        i.setBX(0xffff);
        i.setWidth(false);

        final Instruction uut = new MoveBXAX();
        final Address newIP = uut.exec(i);

        assertEquals(0x0000ffff, i.getAX(true));
        assertEquals(0, newIP.offset());
    }

    @Test
    public void randomAX() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(false);
        i.setAX(0x7f);

        final Address newIP = new RandomAX().exec(i);

        assertTrue(i.getAX() < 0x7f);
        assertEquals(0, newIP.offset());
    }

    @Test
    public void setNarrow() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(true);
        i.setAX(0x0000ffff);
        final Instruction uut = Instructions.SET_NARROW;

        final Address newIP = uut.exec(i);

        assertFalse(i.isWide());
        assertEquals(0x00000000, i.getAL());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void setWide() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        i.setWidth(false);
        final Instruction uut = Instructions.SET_WIDE;

        final Address newIP = uut.exec(i);

        assertTrue(i.isWide());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void storeBLHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x08, // StoreBLHeap
                (byte)0x7a, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(null, List.of(program, Chunk.EMPTY)).init();
        i.setBL(0xffff);
        i.start(0, 0);

        assertEquals(0x000000ff, i.heap(0x7a).read());
        assertEquals(3, i.instructionsExecuted());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void strToInt() {
        final Interpreter i = new Interpreter(null, List.of(Chunk.EMPTY));
        final char[] chars = new char[]{'1', '5', '3', '9', '4'};
        int pointer = 0xc6;
        for (char ch : chars) {
            i.heap(pointer++).write(((int)ch) | 0x80);
        }
        i.heap(pointer).write(0x00);

        final Address nextIP = new StrToInt().exec(i);

        assertEquals(15394, i.heap(0x37).read(4));
    }
}
