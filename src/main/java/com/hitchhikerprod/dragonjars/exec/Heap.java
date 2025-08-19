package com.hitchhikerprod.dragonjars.exec;

import java.util.function.Function;

/** This isn't really a heap, but it is a chunk of the memory space that a lot of active data gets written to. */
public class Heap {
    private final int[] storage = new int[256];

    public void write(int index, int count, int val) {
        if (count > 4) throw new IllegalArgumentException();
        for (int i = 0; i < count; i++) {
            storage[(index + i) & 0xff] = val & 0xff;
            val = val >> 8;
        }
    }

    public int read(int index, int count) {
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
