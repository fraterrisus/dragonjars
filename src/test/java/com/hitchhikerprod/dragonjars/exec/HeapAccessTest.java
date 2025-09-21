package com.hitchhikerprod.dragonjars.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HeapAccessTest {
    @Test
    public void roundTrip() {
        final Heap.Access access = Heap.get(0x10);
        access.write(0xddccbbaa, 4);
        assertEquals(0x000000aa, access.read(1));
        assertEquals(0x0000bbaa, access.read(2));
        assertEquals(0x00ccbbaa, access.read(3));
        assertEquals(0xddccbbaa, access.read(4));
    }

    @Test
    public void writeTooManyBytes() {
        final Heap.Access access = Heap.get(0x10);
        assertThrows(IllegalArgumentException.class, () -> access.write(0xf, 8));

    }

    @Test
    public void writeTooManyZeroes() {
        Heap.get(0x17).write(0xff, 1);
        final Heap.Access access = Heap.get(0x10);
        access.write(0x00, 8);
        assertEquals(0x00, Heap.get(0x17).read());
    }

    @Test
    public void readTooManyBytes() {
        final Heap.Access access = Heap.get(0x10);
        assertThrows(IllegalArgumentException.class, () -> access.read(8));
    }
}
