package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterpreterBXTest {
    private static final Chunk EMPTY = new Chunk(List.of());

    @Test
    public void getBL() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setBX(0xffffffff);
        assertEquals(0x000000ff, uut.getBL());
    }

    @Test
    public void getBXWide() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.setBX(0xffffffff);
        assertEquals(0x0000ffff, uut.getBX());
    }

    @Test
    public void getBXNarrow() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.setBX(0xffffffff);
        uut.setWidth(false);
        assertEquals(0x000000ff, uut.getBX());
    }

    @Test
    public void getBXForce() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.setBX(0xffffffff);
        uut.setWidth(false);
        assertEquals(0x0000ffff, uut.getBX(true));
    }

    @Test
    public void setBXNarrow1() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(false);
        uut.setBX(0xffffffff);
        assertEquals(0x000000ff, uut.getBX());
    }

    @Test
    public void setBXNarrow2() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.setBX(0xffffffff);
        uut.setWidth(false);
        assertEquals(0x000000ff, uut.getBX());
    }

    @Test
    public void setBXNarrow3() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.setBX(0xffffffff);
        uut.setWidth(false);
        uut.setBX(0xaaaaaaaa);
        assertEquals(0x000000aa, uut.getBX());
        uut.setWidth(true);
        assertEquals(0x0000ffaa, uut.getBX());
    }

    @Test
    public void setBL() {
        final Interpreter uut = new Interpreter(null, List.of(EMPTY));
        uut.setWidth(true);
        uut.setBX(0xffffffff);
        uut.setBL(0xaaaaaaaa);
        assertEquals(0x000000aa, uut.getBL());
        assertEquals(0x0000ffaa, uut.getBX());
    }
}