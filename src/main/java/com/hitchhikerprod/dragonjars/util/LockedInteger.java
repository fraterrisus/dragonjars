package com.hitchhikerprod.dragonjars.util;

import java.util.concurrent.locks.ReentrantLock;

public class LockedInteger {
    private ReentrantLock lock = new ReentrantLock();
    private int value;

    public LockedInteger(int value) {
        this.value = value;
    }

    public int get() {
        lock.lock();
        final int thisValue = value;
        lock.unlock();
        return thisValue;
    }

    public void set(int newValue) {
        lock.lock();
        value = newValue;
        lock.unlock();
    }
}
