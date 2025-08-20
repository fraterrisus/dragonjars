package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkTest {
    @Test
    public void signExtension() {
        final Chunk c = new Chunk(List.of((byte)0x80));
        assertEquals(0x80, c.getUnsignedByte(0x00));
        assertEquals(0xffffff80, c.getByte(0x00));
    }
}