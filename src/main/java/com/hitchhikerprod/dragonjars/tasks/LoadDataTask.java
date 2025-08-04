package com.hitchhikerprod.dragonjars.tasks;

import javafx.concurrent.Task;

public class LoadDataTask extends Task<Void> {
    @Override
    protected Void call() throws Exception {
        for (double i = 0.0; i < 1.0; i += 0.01) {
            try {
                Thread.sleep(100);
                updateProgress(i, 1.0);
                updateMessage(String.valueOf(i));
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        return null;
    }
}
