package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
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
        this.heap = Heap.get(Heap.DETECT_TRAPS_RANGE);
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

            interpreter.taskManager().setEyePhase(eyePhase);
            interpreter.runLater(j -> { if (!j.isPaused()) j.drawEyeHelper(); });

            try { Thread.sleep(ANIMATION_DELAY_MS); } catch (InterruptedException e) {}

            if (isCancelled() || heap.read() == 0) {
                interpreter.taskManager().setEyePhase(-1);
                interpreter.runLater(j -> { if (!j.isPaused()) j.drawEyeHelper(); });
                return null;
            }
        }
    }
}
