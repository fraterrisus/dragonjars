package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.util.Pair;

import java.util.List;

public class EyeAnimationTask extends Task<Void> {
    private static final int ANIMATION_DELAY_MS = 40;

    // arrays at 0x4a2b (icon) and 0x4a39 (duration)
    private static final List<Pair<Integer, Integer>> PHASES = List.of(
            new Pair<>(0x03, 0x50),
            new Pair<>(0x04, 0x05),
            new Pair<>(0x05, 0x1e),
            new Pair<>(0x04, 0x05),
            new Pair<>(0x03, 0x50),
            new Pair<>(0x04, 0x05),
            new Pair<>(0x05, 0x28),
            new Pair<>(0x04, 0x05),
            new Pair<>(0x03, 0x03),
            new Pair<>(0x02, 0x03),
            new Pair<>(0x01, 0x03),
            new Pair<>(0x00, 0x08),
            new Pair<>(0x01, 0x03),
            new Pair<>(0x02, 0x03)
    );

    private final Interpreter interpreter;
    private final Heap.Access heap;

    private int phaseIndex = -1;
    private int phaseCounter = 0;

    public EyeAnimationTask(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.heap = interpreter.heap(Heap.DETECT_TRAPS_RANGE);
    }

    @Override
    protected Void call() throws Exception {
        while (true) {
            if (phaseCounter <= 0) {
                phaseIndex = (phaseIndex + 1) % PHASES.size();
                phaseCounter = PHASES.get(phaseIndex).getValue();
            } else {
                phaseCounter--;
            }
            final int eyePhase = PHASES.get(phaseIndex).getKey();

            Platform.runLater(() -> interpreter.setEyePhase(eyePhase));

            try { Thread.sleep(ANIMATION_DELAY_MS); } catch (InterruptedException e) {}

            // not threadsafe
            if (isCancelled() || heap.read() == 0) {
                interpreter.setEyePhase(-1);
                return null;
            }
        }
    }
}
