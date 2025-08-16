package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HeapTest {
    @Test
    public void roundTrip() {
        final Heap heap = new Heap();
        heap.write(0x10, 4, 0xddccbbaa);
        assertEquals(0x000000aa, heap.read(0x10, 1));
        assertEquals(0x0000bbaa, heap.read(0x10, 2));
        assertEquals(0x00ccbbaa, heap.read(0x10, 3));
        assertEquals(0xddccbbaa, heap.read(0x10, 4));
    }

    @Test
    public void writeTooManyBytes() {
        final Heap heap = new Heap();
        assertThrows(IllegalArgumentException.class, () -> heap.write(0x10, 8, 0));
    }

    @Test
    public void readTooManyBytes() {
        final Heap heap = new Heap();
        assertThrows(IllegalArgumentException.class, () -> heap.read(0x10, 8));
    }
}
