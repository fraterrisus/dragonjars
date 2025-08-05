package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterHeapTest {
    @Test
    public void setHeapNarrow() {
        final Interpreter uut = new Interpreter(List.of(), 0, 0);
        uut.setWidth(false);
        uut.setHeap(0x10, 0xffffffff);
        assertEquals(0x000000ff, uut.getHeapByte(0x10));
        assertEquals(0x00000000, uut.getHeapByte(0x11));
        assertEquals(0x00000000, uut.getHeapByte(0x12));
        assertEquals(0x00000000, uut.getHeapByte(0x13));
    }

    @Test
    public void setHeapWide() {
        final Interpreter uut = new Interpreter(List.of(), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        assertEquals(0x000000dd, uut.getHeapByte(0x10));
        assertEquals(0x000000cc, uut.getHeapByte(0x11));
        assertEquals(0x00000000, uut.getHeapByte(0x12));
        assertEquals(0x00000000, uut.getHeapByte(0x13));
    }

    @Test
    public void getHeapWord() {
        final Interpreter uut = new Interpreter(List.of(), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        assertEquals(0x0000ccdd, uut.getHeapWord(0x10));
        assertEquals(0x000000cc, uut.getHeapWord(0x11));
        assertEquals(0x00000000, uut.getHeapWord(0x12));
        assertEquals(0x00000000, uut.getHeapWord(0x13));
    }

    @Test
    public void getHeapNarrow() {
        final Interpreter uut = new Interpreter(List.of(), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        uut.setWidth(false);
        assertEquals(0x000000dd, uut.getHeap(0x10));
    }

    @Test
    public void getHeapWide() {
        final Interpreter uut = new Interpreter(List.of(), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0x10, 0xaabbccdd);
        assertEquals(0x0000ccdd, uut.getHeap(0x10));
    }

    @Test
    public void setHeapMasksAddress() {
        final Interpreter uut = new Interpreter(List.of(), 0, 0);
        uut.setWidth(true);
        uut.setHeap(0xff10, 0xaabbccdd);
        assertEquals(0x000000dd, uut.getHeapByte(0x10));
        assertEquals(0x000000cc, uut.getHeapByte(0x11));
        assertEquals(0x00000000, uut.getHeapByte(0x12));
        assertEquals(0x00000000, uut.getHeapByte(0x13));
    }
}