package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;

import java.util.concurrent.atomic.AtomicBoolean;

public class PauseUntilKeyOrTime implements Instruction {
    // TODO: make this configurable
    private static final long SLEEP_TIME_MS = 5000;

    private final AtomicBoolean handled;
    private final SleepTask sleepTask;
    private final Interpreter i;
    private Address nextIP;

    public PauseUntilKeyOrTime(Interpreter i) {
        this.i = i;
        this.handled = new AtomicBoolean(false);
        this.sleepTask = new SleepTask(SLEEP_TIME_MS);
    }

    @Override
    public Address exec(Interpreter ignored) {
        // drawPartyInfo()   0x4840
        // drawString313e()  0x4843

        nextIP = i.getIP().incr(OPCODE);

        i.app().setKeyHandler(event -> moveAlong());

        sleepTask.setOnSucceeded(event -> moveAlong());
        final Thread thread = new Thread(sleepTask);
        thread.setDaemon(true);
        thread.start();

        return null;
    }

    private void moveAlong() {
        if (handled.compareAndSet(false, true)) {
            sleepTask.cancel();
            i.app().setKeyHandler(null);
            i.eraseTransient();
            i.start(nextIP);
        }
    }
}
