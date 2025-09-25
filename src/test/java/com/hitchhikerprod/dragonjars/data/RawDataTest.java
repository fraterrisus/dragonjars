package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawDataTest {
    @Test
    public void testSetNorthEdge() {
        final RawData r = new RawData(0xffffff).setNorthEdge(0x5);
        assertEquals(0xffff5f, r.value());
    }

    @Test
    public void testSetWestEdge() {
        final RawData r = new RawData(0xffffff).setWestEdge(0x1);
        assertEquals(0xfffff1, r.value());
    }

    @Test
    public void testGetNorthEdge() {
        final RawData r = new RawData(0x000080);
        assertEquals(0x8, r.getNorthEdge());
    }

    @Test
    public void testGetWestEdge() {
        final RawData r = new RawData(0x000002);
        assertEquals(0x2, r.getWestEdge());
    }
}