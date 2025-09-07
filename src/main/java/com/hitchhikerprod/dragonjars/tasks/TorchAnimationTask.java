package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;

public class TorchAnimationTask extends Task<Void> {
    // the assembly uses a counter that's initialized to 0x02 and checked for 0x00
    // so this task runs 3x slower than the eye task
    public static final int ANIMATION_DELAY_MS = 120;

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

            try { Thread.sleep(ANIMATION_DELAY_MS); } catch (InterruptedException e) {}

            if (isCancelled() || heap.read() == 0) {
                interpreter.setTorchPhase(-1);
                return null;
            }
        }
    }

}
