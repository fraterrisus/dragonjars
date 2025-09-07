package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class TorchAnimationTask extends Task<Void> {
    private final Interpreter interpreter;
    private final Heap.Access heap;

    private int phaseIndex;

    public TorchAnimationTask(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.heap = interpreter.heap(Heap.LIGHT_SOURCE);
    }

    @Override
    protected Void call() throws Exception {
        while (true) {
            phaseIndex = (phaseIndex + 1 + (int)(Math.random() * 4)) % 5;
            Platform.runLater(() -> interpreter.setTorchPhase(phaseIndex));

            sleepHelper(100);

            if (isCancelled() || heap.read() == 0) {
                interpreter.setTorchPhase(-1);
                return null;
            }
        }
    }

    private void sleepHelper(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
