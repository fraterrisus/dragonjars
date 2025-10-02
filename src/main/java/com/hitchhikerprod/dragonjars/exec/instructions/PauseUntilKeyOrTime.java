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
    private Address nextIP;

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

        nextIP = i.getIP().incr(OPCODE);

        i.app().setKeyHandler(event -> moveAlong());

        sleepTask.setOnSucceeded(event -> moveAlong());
        Thread.ofPlatform().daemon().start(sleepTask);

        return null;
    }

    private void moveAlong() {
        if (handled.compareAndSet(false, true)) {
            sleepTask.cancel();
            i.app().setKeyHandler(null);
            i.start(nextIP);
        }
    }
}
