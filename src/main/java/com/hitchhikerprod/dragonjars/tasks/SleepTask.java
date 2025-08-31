package com.hitchhikerprod.dragonjars.tasks;

import javafx.concurrent.Task;

public class SleepTask extends Task<Void> {
    private final long sleepTimeMs;

    public SleepTask(long sleepTimeMs) {
        this.sleepTimeMs = sleepTimeMs;
    }

    @Override
    protected Void call() throws Exception {
        try { Thread.sleep(sleepTimeMs); } catch (InterruptedException e) {}
        return null;
    }
}
