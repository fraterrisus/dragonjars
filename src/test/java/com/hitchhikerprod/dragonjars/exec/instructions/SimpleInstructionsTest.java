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
    public void exitInstruction() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        final Instruction uut = new ExitInstruction();

        final Address newIP = uut.exec(i);

        assertNull(newIP);
    }

    @Test
    public void incBL() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.setWidth(true);
        i.setBX(0x0000ffff);
        final Instruction uut = new IncBL();

        final Address newIP = uut.exec(i);

        assertEquals(0x0000ff00, i.getBX());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void incHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x01, // SetNarrow
                (byte)0x23, // IncHeap
                (byte)0x74, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setWidth(false);
        i.setHeap(0x74, 0xff);
        i.setHeap(0x75, 0x01);

        i.start();

        i.setWidth(true);
        assertEquals(0x00000200, i.getHeap(0x74));
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void loadBLHeap() {
        final Chunk program = new Chunk(List.of(
                (byte)0x00, // SetWide
                (byte)0x05, // LoadBLHeap
                (byte)0x26, // heap index
                (byte)0x5a  // Exit
        ));

        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setHeap(0x26, 0xaa);
        i.setHeap(0x27, 0xbb);
        i.start();

        assertEquals(0x000000aa, i.getBL());
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

        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.start();

        assertEquals(0x000000aa, i.getBL());
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }

    @Test
    public void loadBLZero() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.setWidth(true);
        i.setBX(0xffff);

        final Instruction uut = new LoadBLZero();
        final Address newIP = uut.exec(i);

        assertEquals(0x00000000, i.getBL());
        assertEquals(0x0000ff00, i.getBX());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void moveALBL() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
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
        final Interpreter i = new Interpreter(List.of(), 0, 0);
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
    public void setNarrow() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.setWidth(true);
        final Instruction uut = new SetNarrow();

        final Address newIP = uut.exec(i);

        assertFalse(i.isWide());
        assertEquals(0, newIP.offset());
    }

    @Test
    public void setWide() {
        final Interpreter i = new Interpreter(List.of(), 0, 0);
        i.setWidth(false);
        final Instruction uut = new SetWide();

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

        final Interpreter i = new Interpreter(List.of(program), 0, 0);
        i.setBL(0xffff);
        i.start();

        assertEquals(0x000000ff, i.getHeapByte(0x7a));
        assertEquals(program.getSize() - 1, i.getIP().offset());
    }
}
