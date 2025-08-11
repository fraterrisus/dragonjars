package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterpreterAXTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void getAL() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setAX(0xffffffff);
        assertEquals(0x000000ff, uut.getAL());
    }

    @Test
    public void getAXWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setAX(0xffffffff);
        assertEquals(0x0000ffff, uut.getAX());
    }

    @Test
    public void getAXNarrow() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setAX(0xffffffff);
        uut.setWidth(false);
        assertEquals(0x000000ff, uut.getAX());
    }

    @Test
    public void getAXForce() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setAX(0xffffffff);
        uut.setWidth(false);
        assertEquals(0x0000ffff, uut.getAX(true));
    }

    @Test
    public void setAXNarrow1() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(false);
        uut.setAX(0xffffffff);
        assertEquals(0x000000ff, uut.getAX());
    }

    @Test
    public void setAXNarrow2() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setAX(0xffffffff);
        uut.setWidth(false);
        assertEquals(0x000000ff, uut.getAX());
    }

    @Test
    public void setAXNarrow3() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setAX(0xffffffff);
        uut.setWidth(false);
        uut.setAX(0xaaaaaaaa);
        assertEquals(0x000000aa, uut.getAX());
        uut.setWidth(true);
        assertEquals(0x0000ffaa, uut.getAX());
    }

    @Test
    public void setAL() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY), 0, 0);
        uut.setWidth(true);
        uut.setAX(0xffffffff);
        uut.setAL(0xaaaaaaaa);
        assertEquals(0x000000aa, uut.getAL());
        assertEquals(0x0000ffaa, uut.getAX());
    }
}