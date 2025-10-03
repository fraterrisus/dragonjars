package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.data.Facing;
import com.hitchhikerprod.dragonjars.data.GridCoordinate;
import com.hitchhikerprod.dragonjars.data.PartyLocation;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/** This isn't really a heap, but it is a chunk of the memory space that a lot of active data gets written to. */
public class Heap {
    private static final Heap INSTANCE = new Heap();
    private static final ReentrantLock LOCK = new ReentrantLock();

    public static Heap getInstance() { return INSTANCE; }

    public static final int PARTY_Y = 0x00;
    public static final int PARTY_X = 0x01;
    public static final int BOARD_ID = 0x02;
    public static final int PARTY_FACING = 0x03;
    public static final int SELECTED_PC = 0x06;
    public static final int SELECTED_ITEM = 0x07;
    // note that marching order is *2 so that the offset into party data is just <<8 (0x000, 0x200, 0x400 etc.)
    public static final int MARCHING_ORDER = 0x0a; // through 0x10
    public static final int PC_DIRTY = 0x18; // used for a number of different "has been touched" loops
    public static final int PARTY_SIZE = 0x1f;
    public static final int BOARD_MAX_X = 0x21;
    public static final int BOARD_MAX_Y = 0x22;
    public static final int BOARD_FLAGS = 0x23;
    public static final int RANDOM_ENCOUNTERS = 0x24;
    public static final int WALL_METADATA = 0x26;
    public static final int LONGPTR_ADR = 0x34;
    public static final int COMBAT_MODE = 0x35; // 0xff combat, 0x00 travel
    public static final int LONGPTR_SEG = 0x36;
    public static final int RECENT_SPECIAL = 0x3e;
    public static final int NEXT_SPECIAL = 0x3f;
    public static final int BOARD_1_SEGIDX = 0x56;
    public static final int DECODED_BOARD_ID = 0x57;
    public static final int BOARD_2_SEGIDX = 0x5a;
    public static final int GAME_STATE_GLOBAL = 0x99;
    public static final int GAME_STATE_BOARD = 0xb9;
    public static final int COMPASS_ENABLED = 0xbe;       // see 0x4a47
    public static final int DETECT_TRAPS_RANGE = 0xbf;
    public static final int SHIELD_POWER = 0xc0;
    public static final int LIGHT_RANGE = 0xc1;
    public static final int COMPASS_DURATION = 0xc2;
    public static final int DETECT_TRAPS_DURATION = 0xc3;
    public static final int SHIELD_DURATION = 0xc4;
    public static final int LIGHT_DURATION = 0xc5;
    public static final int INPUT_STRING = 0xc6;

    private final int[] storage = new int[256];

    private Heap() {}

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

    // Mostly for testing, since having a static Heap gets messy
    public static void reset() {
        for (int i = 0; i < 256; i++) INSTANCE.storage[i] = 0x00;
    }

    // Moved from the Interpreter because it's all static access now
    public static PartyLocation getPartyLocation() {
        LOCK.lock();
        try {
            return new PartyLocation(
                    get(BOARD_ID).lockedRead(),
                    new GridCoordinate(get(PARTY_X).lockedRead(), get(PARTY_Y).lockedRead()),
                    Facing.valueOf(get(PARTY_FACING).lockedRead())
            );
        } finally {
            LOCK.unlock();
        }
    }

    public static Address getPCBaseAddress() {
        return getPCBaseAddress(SELECTED_PC);
    }

    public static Address getPCBaseAddress(int heapIndex) {
        LOCK.lock();
        try {
            final int marchingOrder = get(heapIndex).lockedRead();
            return new Address(
                    Interpreter.PARTY_SEGMENT,
                    get(MARCHING_ORDER + marchingOrder).lockedRead() << 8
            );
        } finally {
            LOCK.unlock();
        }
    }

    public static Access get(int index) {
        return new Access(INSTANCE, index);
    }

    public static void lock() {
        LOCK.lock();
    }

    public static void unlock() {
        LOCK.unlock();
    }

    public static class Access {
        private final Heap heap;
        private final int index;

        private Access(Heap heap, int index) {
            this.heap = heap;
            this.index = index;
        }

        public void lock() {
            LOCK.lock();
        }

        public void unlock() {
            LOCK.unlock();
        }

        public int lockedRead() {
            return lockedRead(1);
        }

        public int lockedRead(int count) {
            if (!LOCK.isHeldByCurrentThread()) throw new RuntimeException("lockedRead() called without lock()");
            return heap.read(index, count);
        }

        public int read() {
            return read(1);
        }

        public int read(int count) {
            if (LOCK.isHeldByCurrentThread()) throw new RuntimeException("read() called after lock()");
            LOCK.lock();
            try { return lockedRead(count); }
            finally { LOCK.unlock(); }
        }

        public void lockedWrite(int val) {
            lockedWrite(val, 1);
        }

        public void lockedWrite(int val, int count) {
            if (!LOCK.isHeldByCurrentThread()) throw new RuntimeException("lockedWrite() called without lock()");
            heap.write(index, count, val);
        }

        public void write(int val) {
            write(val, 1);
        }

        public void write(int val, int count) {
            if (LOCK.isHeldByCurrentThread()) throw new RuntimeException("write() called after lock()");
            LOCK.lock();
            try { lockedWrite(val, count); }
            finally { LOCK.unlock(); }
        }

        public void modify(int count, Function<Integer, Integer> fn) {
            if (LOCK.isHeldByCurrentThread()) throw new RuntimeException("modify() called after lock()");
            LOCK.lock();
            try {
                final int before = lockedRead(count);
                final int after = fn.apply(before);
                lockedWrite(after, count);
            }
            finally { LOCK.unlock(); }
        }
    }
}
