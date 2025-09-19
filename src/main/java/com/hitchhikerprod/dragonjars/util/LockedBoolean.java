package com.hitchhikerprod.dragonjars.util;

import java.util.concurrent.locks.ReentrantLock;

public class LockedBoolean {
    private ReentrantLock lock = new ReentrantLock();
    private boolean value;

    public LockedBoolean(boolean value) {
        this.value = value;
    }

    public boolean get() {
        lock.lock();
        final boolean thisValue = value;
        lock.unlock();
        return thisValue;
    }

    public void set(boolean newValue) {
        lock.lock();
        value = newValue;
        lock.unlock();
    }
}
