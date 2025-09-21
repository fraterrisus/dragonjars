package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;

public class SpellDecayTask extends Task<Void> {
    // Mage Light, Guidance: 3 ticks per point
    // Radiance, Disarm Traps, Sense Traps: 2 ticks
    // Cloak Arcane: 1 tick
    private static final int ANIMATION_DELAY_MS = 250;
    private static final int CYCLES_PER_POINT = 60000 / ANIMATION_DELAY_MS; // one minute per tick

    private final Interpreter interpreter;

    private static class DecayCounter {
        private int counter;
        private final int durationIndex;
        private final int effectIndex;

        private DecayCounter(int durationIndex, int effectIndex) {
            this.durationIndex = durationIndex;
            this.effectIndex = effectIndex;
            reset();
        }

        private void reset() {
            counter = CYCLES_PER_POINT;
        }

        private boolean decrement() {
            counter--;
            if (counter == 0) {
                reset();
                return true;
            } else {
                return false;
            }
        }

        public boolean decrementAndUpdate(Interpreter i) {
            final Heap.Access dur = Heap.get(durationIndex);
            dur.lock();
            try {
                final int duration = dur.lockedRead();
                if (!i.isPaused() && (Heap.get(Heap.COMBAT_MODE).lockedRead() == 0) && duration > 0) {
                    if (decrement()) {
                        dur.lockedWrite(duration - 1);
                        if (duration - 1 == 0) {
                            Heap.get(effectIndex).lockedWrite(0);
                            return true;
                        }
                    }
                }
                return false;
            } finally {
                dur.unlock();
            }
        }
    }

    private static final List<DecayCounter> decays = List.of(
            new DecayCounter(Heap.COMPASS_DURATION, Heap.COMPASS_ENABLED),
            new DecayCounter(Heap.DETECT_TRAPS_DURATION, Heap.DETECT_TRAPS_RANGE),
            new DecayCounter(Heap.SHIELD_DURATION, Heap.SHIELD_POWER),
            new DecayCounter(Heap.LIGHT_DURATION, Heap.LIGHT_RANGE)
    );

    public SpellDecayTask(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    protected Void call() throws Exception {
        while(true) {
            if (isCancelled()) return null;
            final boolean updates = decays.stream()
                    .map(d -> d.decrementAndUpdate(interpreter))
                    .reduce(Boolean::logicalOr)
                    .orElse(false);
            if (updates) Platform.runLater(() -> interpreter.drawSpellIcons(false));
            try { Thread.sleep(ANIMATION_DELAY_MS); } catch (InterruptedException _) {}
        }
    }
}
