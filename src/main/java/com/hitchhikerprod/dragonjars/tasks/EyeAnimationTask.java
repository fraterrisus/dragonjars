package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;

public class EyeAnimationTask extends Task<Void> {
    private static final List<Integer> phases = List.of(
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 5, 5, 4, 4,
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 5, 5, 4, 4,
            3, 3, 3, 3, 3, 3, 2, 1, 0, 0, 1, 2
    );

    private final Interpreter interpreter;
    private final Heap.Access heap;
    private int phaseIndex = 10;

    public EyeAnimationTask(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.heap = interpreter.heap(Heap.DETECT_TRAPS_DURATION);
    }

    @Override
    protected Void call() throws Exception {
        while (true) {
            Platform.runLater(() -> {
                interpreter.setEyePhase(phases.get(phaseIndex));
            });

            sleepHelper(250);

            if (isCancelled() || heap.read() == 0) {
                interpreter.setEyePhase(-1);
                return null;
            }

            if (interpreter.isPaused()) continue;

            // TODO decrement spell duration counter (or do it elsewhere)
            // System.out.println("Detect Traps duration: " + heap.read() + " / 4s");
            // heap.modify(1, x -> x - 1);

            phaseIndex = (phaseIndex + 1) % phases.size();
        }
    }

    private void sleepHelper(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
