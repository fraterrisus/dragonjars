package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Memory;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;

public class SpellDecayTask extends Task<Void> {
    // Summon spells: 4 ticks per point
    // Mage Light, Guidance: 3 ticks per point
    // Radiance, Disarm Traps, Sense Traps: 2 ticks
    // Cloak Arcane: 1 tick
    private static final int ANIMATION_DELAY_MS = 250;
    public static final int CYCLES_PER_POINT = 60000 / ANIMATION_DELAY_MS; // one minute per tick

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
            final int duration = dur.lockedRead();
            if (duration > 0) {
                if (decrement()) {
                    dur.lockedWrite(duration - 1);
                    if (duration - 1 == 0) {
                        Heap.get(effectIndex).lockedWrite(0);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static final List<DecayCounter> decays = List.of(
            new DecayCounter(Heap.COMPASS_DURATION, Heap.COMPASS_ENABLED),
            new DecayCounter(Heap.DETECT_TRAPS_DURATION, Heap.DETECT_TRAPS_RANGE),
            new DecayCounter(Heap.SHIELD_DURATION, Heap.SHIELD_POWER),
            new DecayCounter(Heap.LIGHT_DURATION, Heap.LIGHT_RANGE)
    );

    private final Interpreter i;

    public SpellDecayTask(Interpreter interpreter) {
        this.i = interpreter;
    }

    @Override
    protected Void call() throws Exception {
        while(true) {
            try { Thread.sleep(ANIMATION_DELAY_MS); } catch (InterruptedException _) {}
            if (isCancelled()) return null;

            // Don't run the timers if the game is paused or we're in combat
            if (i.isPaused() || Heap.get(Heap.COMBAT_MODE).read() > 0) continue;

            Heap.lock();
            try {
                if (decaySummons()) Platform.runLater(i::drawPartyInfoArea);
                if (decays.stream()
                        .map(d -> d.decrementAndUpdate(i))
                        .reduce(Boolean::logicalOr)
                        .orElse(false)) Platform.runLater(() -> i.drawSpellIcons(false));
            } finally {
                Heap.unlock();
            }
        }
    }

    private boolean decaySummons() {
        boolean updates = false;
        final int partySize = Heap.get(Heap.PARTY_SIZE).lockedRead();
        final int partySeg = Interpreter.PARTY_SEGMENT;
        for (int pcid = 0; pcid < partySize; pcid++) {
            final int base = Heap.get(Heap.MARCHING_ORDER + pcid).lockedRead() << 8;

            // If summon is already dead, don't bother decrementing its life counter.
            final Address status = new Address(partySeg, base + Memory.PARTY_STATUS);
            final int statusValue = i.memory().read(status, 1);
            if ((statusValue & 0x01) > 0) continue;

            // This is written by [06:0b86] when the spell is cast
            final Address lifespanMacro = new Address(partySeg, base + Memory.PARTY_SUMMONED_LIFESPAN);
            // This isn't in the code; I'm stealing an otherwise unused byte of party data.
            final Address lifespanMicro = new Address(partySeg, base + Memory.PARTY_SUMMONED_TICKS);

            final int bigCounter = i.memory().read(lifespanMacro, 2);
            final int littleCounter = i.memory().read(lifespanMicro, 2);
            if (littleCounter > 0) {
                if (littleCounter == 1 && bigCounter == 0) {
                    i.memory().write(partySeg, base + Memory.PARTY_HEALTH_CURRENT, 4, 0);
                    i.memory().write(partySeg, base + Memory.PARTY_STUN_CURRENT, 4, 0);
                    i.memory().write(status, 1, statusValue | 0x01); // mark Dead
                    Heap.get(Heap.PC_DIRTY + pcid).lockedWrite(0);
                    updates = true;
                } else {
                    i.memory().write(lifespanMicro, 2, littleCounter - 1);
                }
            } else {
                if (bigCounter > 0) {
                    i.memory().write(lifespanMacro, 2, bigCounter - 1);
                    i.memory().write(lifespanMicro, 2, CYCLES_PER_POINT);
                }
            }
        }
        return updates;
    }
}
