package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterHeapTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void writeHeapNarrow() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(false);
        uut.writeHeap(0x10, 0xffffffff);
        assertEquals(0x000000ff, uut.heap().read(0x10, 1));
        assertEquals(0x00000000, uut.heap().read(0x11, 1));
        assertEquals(0x00000000, uut.heap().read(0x12, 1));
        assertEquals(0x00000000, uut.heap().read(0x13, 1));
    }

    @Test
    public void writeHeapWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.writeHeap(0x10, 0xaabbccdd);
        assertEquals(0x000000dd, uut.heap().read(0x10, 1));
        assertEquals(0x000000cc, uut.heap().read(0x11, 1));
        assertEquals(0x00000000, uut.heap().read(0x12, 1));
        assertEquals(0x00000000, uut.heap().read(0x13, 1));
    }

    @Test
    public void readHeapWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.heap().write(0x10, 4, 0xaabbccdd);
        uut.setWidth(true);
        assertEquals(0x0000ccdd, uut.readHeap(0x10));
        assertEquals(0x0000bbcc, uut.readHeap(0x11));
        assertEquals(0x0000aabb, uut.readHeap(0x12));
        assertEquals(0x000000aa, uut.readHeap(0x13));
    }

    @Test
    public void readHeapNarrow() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.heap().write(0x10, 4, 0xaabbccdd);
        uut.setWidth(false);
        assertEquals(0x000000dd, uut.readHeap(0x10));
        assertEquals(0x000000cc, uut.readHeap(0x11));
        assertEquals(0x000000bb, uut.readHeap(0x12));
        assertEquals(0x000000aa, uut.readHeap(0x13));
    }

    @Test
    public void writeHeapMasksAddress() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.writeHeap(0xff10, 0xaabbccdd);
        assertEquals(0x000000dd, uut.heap().read(0x10, 1));
        assertEquals(0x000000cc, uut.heap().read(0x11, 1));
        assertEquals(0x00000000, uut.heap().read(0x12, 1));
        assertEquals(0x00000000, uut.heap().read(0x13, 1));
    }
}