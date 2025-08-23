package com.hitchhikerprod.dragonjars.exec;

import java.util.function.Function;

/** This isn't really a heap, but it is a chunk of the memory space that a lot of active data gets written to. */
public class Heap {
    public static final int PARTY_Y = 0x00;
    public static final int PARTY_X = 0x01;
    public static final int BOARD_ID = 0x02;
    public static final int PARTY_FACING = 0x03;
    public static final int BOARD_MAX_X = 0x21;
    public static final int BOARD_MAX_Y = 0x22;
    public static final int BOARD_FLAGS = 0x23;
    public static final int WALL_METADATA = 0x26;
    public static final int LONGPTR_ADR = 0x34;
    public static final int LONGPTR_SEG = 0x36;
    public static final int BOARD_1_SEGIDX = 0x56;
    public static final int BOARD_2_SEGIDX = 0x5a;

    private final int[] storage = new int[256];

    private void write(int index, int count, int val) {
        if (val != 0 && count > 4) throw new IllegalArgumentException();
        for (int i = 0; i < count; i++) {
//            System.out.format("  heap[%02x] = %02x\n", (index + i) & 0xff, val & 0xff);
            storage[(index + i) & 0xff] = val & 0xff;
            val = val >> 8;
        }
    }

    private int read(int index, int count) {
        if (count > 4) throw new IllegalArgumentException();
        int value = 0;
        for (int i = count - 1; i >= 0; i--) {
            value = value << 8;
            value = value | (storage[(index & 0xff) + i] & 0xff);
        }
        return value;
    }

    public Access get(int index) {
        return new Access(index);
    }

    public class Access {
        private final int index;

        private Access(int index) {
            this.index = index;
        }

        public int read() {
            return read(1);
        }

        public int read(int count) {
            return Heap.this.read(index, count);
        }

        public void write(int val) {
            write(val, 1);
        }

        public void write(int val, int count) {
            Heap.this.write(index, count, val);
        }

        public void modify(int count, Function<Integer, Integer> fn) {
            final int before = read(count);
            final int after = fn.apply(before);
            write(after, count);
        }
    }
}
