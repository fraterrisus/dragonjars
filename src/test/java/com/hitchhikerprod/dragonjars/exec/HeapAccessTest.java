package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HeapAccessTest {
    @Test
    public void roundTrip() {
        final Heap heap = new Heap();
        final Heap.Access access = heap.get(0x10);
        access.write(0xddccbbaa, 4);
        assertEquals(0x000000aa, access.read(1));
        assertEquals(0x0000bbaa, access.read(2));
        assertEquals(0x00ccbbaa, access.read(3));
        assertEquals(0xddccbbaa, access.read(4));
    }

    @Test
    public void writeTooManyBytes() {
        final Heap heap = new Heap();
        final Heap.Access access = heap.get(0x10);
        assertThrows(IllegalArgumentException.class, () -> access.write(0x0, 8));
    }

    @Test
    public void readTooManyBytes() {
        final Heap heap = new Heap();
        final Heap.Access access = heap.get(0x10);
        assertThrows(IllegalArgumentException.class, () -> access.read(8));
    }
}
