package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.CombatData;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;
import com.hitchhikerprod.dragonjars.ui.AppPreferences;

import java.util.concurrent.atomic.AtomicBoolean;

public class PauseUntilKeyOrTime implements Instruction {
    private final AtomicBoolean handled;
    private final SleepTask sleepTask;
    private final Interpreter i;
    private int nextChunkId;
    private int nextAddress;

    public PauseUntilKeyOrTime(Interpreter i) {
        this.i = i;
        i.combatData().ifPresent(CombatData::turnDone);
        this.handled = new AtomicBoolean(false);
        final double sleepTimeSec = AppPreferences.getInstance().combatDelayProperty().get();
        this.sleepTask = new SleepTask(Math.round(1000 * sleepTimeSec));
    }

    @Override
    public Address exec(Interpreter ignored) {
        i.drawPartyInfoArea(); // 0x4840
        i.drawStringBuffer(); // 0x4843

        final Address nextIP = i.getIP().incr(OPCODE);
        this.nextChunkId = nextIP.chunkId(i.memory());
        this.nextAddress = nextIP.offset();

        i.setKeyHandler(event -> moveAlong());

        sleepTask.setOnSucceeded(event -> moveAlong());
        Thread.ofVirtual().start(sleepTask);

        return null;
    }

    private void moveAlong() {
        if (handled.compareAndSet(false, true)) {
            sleepTask.cancel();
            i.start(nextChunkId, nextAddress);
        }
    }
}
