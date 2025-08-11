package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterHeapTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void setHeapNarrow() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(false);
        uut.setHeap(0x10, 0xffffffff);
        assertEquals(0x000000ff, uut.getHeapBytes(0x10, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x11, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x12, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x13, 1));
    }

    @Test
    public void setHeapWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        assertEquals(0x000000dd, uut.getHeapBytes(0x10, 1));
        assertEquals(0x000000cc, uut.getHeapBytes(0x11, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x12, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x13, 1));
    }

    @Test
    public void getHeapWord() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        assertEquals(0x0000ccdd, uut.getHeapBytes(0x10, 2));
        assertEquals(0x000000cc, uut.getHeapBytes(0x11, 2));
        assertEquals(0x00000000, uut.getHeapBytes(0x12, 2));
        assertEquals(0x00000000, uut.getHeapBytes(0x13, 2));
    }

    @Test
    public void getHeapNarrow() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        uut.setWidth(false);
        assertEquals(0x000000dd, uut.getHeap(0x10));
    }

    @Test
    public void getHeapWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        assertEquals(0x0000ccdd, uut.getHeap(0x10));
    }

    @Test
    public void setHeapMasksAddress() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0xff10, 0xaabbccdd);
        assertEquals(0x000000dd, uut.getHeapBytes(0x10, 1));
        assertEquals(0x000000cc, uut.getHeapBytes(0x11, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x12, 1));
        assertEquals(0x00000000, uut.getHeapBytes(0x13, 1));
    }

    @Test
    public void getHeapBytes() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(false);
        uut.setHeap(0x10, 0xaa);
        uut.setHeap(0x11, 0xbb);
        uut.setHeap(0x12, 0xcc);
        uut.setHeap(0x13, 0xdd);
        assertEquals(0x000000aa, uut.getHeapBytes(0x10, 1));
        assertEquals(0x0000bbaa, uut.getHeapBytes(0x10, 2));
        assertEquals(0x00ccbbaa, uut.getHeapBytes(0x10, 3));
        assertEquals(0xddccbbaa, uut.getHeapBytes(0x10, 4));
    }
}