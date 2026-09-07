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

    @Test
    public void searchEasy() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x02, (byte)0x03);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(1, result);
    }

    @Test
    public void searchOneItem() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x03);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(2, result);
    }

    @Test
    public void searchFirstItem() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x01, (byte)0x02, (byte)0x03);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(0, result);
    }

    @Test
    public void searchLastItem() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x02, (byte)0x03, (byte)0x04);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(1, result);
    }

    @Test
    public void searchEqualLists() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(haystack);
        assertEquals(0, result);
    }

    @Test
    public void searchNotPresent() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x02, (byte)0x05);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(-1, result);
    }

    @Test
    public void searchOverlapping() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x02, (byte)0x03, (byte)0x04);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(3, result);
    }

    @Test
    public void searchReturnsFirstMatch() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04);
        final List<Byte> needle = List.of((byte)0x03);
        final Chunk c = new Chunk(haystack);
        final int result = c.search(needle);
        assertEquals(2, result);
    }

    @Test
    public void searchNullInput() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04);
        final Chunk c = new Chunk(haystack);
        assertThrows(IllegalArgumentException.class, () -> c.search(null));
    }

    @Test
    public void searchEmptyInput() {
        final List<Byte> haystack = List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x02, (byte)0x03, (byte)0x04);
        final Chunk c = new Chunk(haystack);
        assertThrows(IllegalArgumentException.class, () -> c.search(List.of()));
    }
}