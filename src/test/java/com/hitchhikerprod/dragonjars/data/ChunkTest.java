package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChunkTest {
    @Test
    public void getByte() {
        final Chunk c = new Chunk(List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04));
        assertEquals(3, c.getByte(2));
    }

    @Test
    public void getByteReturnsIntWithSignExtension() {
        final Chunk c = new Chunk(List.of((byte)0x01, (byte)0x02, (byte)0x80, (byte)0x04));
        assertEquals(0xffffff80, c.getByte(2));
    }

    @Test
    public void getBytes() {
        final Chunk c = new Chunk(List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04));
        final List<Byte> bytes = c.getBytes(2, 2);
        assertEquals(2, bytes.size());
        assertEquals((byte)0x03, bytes.getFirst());
        assertEquals((byte)0x04, bytes.getLast());
    }

    @Test
    public void getBytesReturnsEmptyListIfOffsetTooLarge() {
        final Chunk c = new Chunk(List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04));
        final List<Byte> bytes = c.getBytes(17, 2);
        assertEquals(0, bytes.size());
    }

    @Test
    public void getBytesTruncatesIfLengthTooLong() {
        final Chunk c = new Chunk(List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04));
        final List<Byte> bytes = c.getBytes(2, 17);
        assertEquals(2, bytes.size());
    }

    @Test
    public void getSize() {
        final Chunk c = new Chunk(List.of((byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04));
        assertEquals(4, c.getSize());
    }

    @Test
    public void getUnsignedByte() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04));
        assertEquals(1, c.getUnsignedByte(0));
    }

    @Test
    public void getUnsignedByteFailsOnIllegalIndex() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04));
        assertThrows(IndexOutOfBoundsException.class, () -> c.getByte(17));
    }

    @Test
    public void getUnsignedByteDoesNotSignExtend() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04));
        assertEquals(255, c.getUnsignedByte(1));
    }

    @Test
    public void getWordUsesLittleEndian() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04));
        assertEquals(0x0302, c.getWord(1));
    }

    @Test
    public void getWordFailsOnIllegalIndex() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04));
        assertThrows(IndexOutOfBoundsException.class, () -> c.getWord(3));
    }

    @Test
    public void getQuadUsesLittleEndian() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04));
        assertEquals(0x04030201, c.getQuadWord(0));
    }

    @Test
    public void getQuadFailsOnIllegalIndex() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04));
        assertThrows(IndexOutOfBoundsException.class, () -> c.getQuadWord(3));
    }

    @Test
    public void readOne() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04, (byte) 0x05));
        assertEquals(0x03, c.read(2, 1));
    }

    @Test
    public void readTwo() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04, (byte) 0x05));
        assertEquals(0x0403, c.read(2, 2));
    }

    @Test
    public void readThree() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04, (byte) 0x05));
        assertEquals(0x050403, c.read(2, 3));
    }

    @Test
    public void readFour() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04, (byte) 0x05));
        assertEquals(0x050403ff, c.read(1, 4));
    }

    @Test
    public void readFailsOnLongWord() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04, (byte) 0x05));
        assertThrows(IllegalArgumentException.class, () -> c.read(0,5));
    }

    @Test
    public void readFailsOnIllegalIndex() {
        final Chunk c = new Chunk(List.of((byte) 0x01, (byte) 0xff, (byte) 0x03, (byte) 0x04, (byte) 0x05));
        assertThrows(IndexOutOfBoundsException.class, () -> c.read(4,3));
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
