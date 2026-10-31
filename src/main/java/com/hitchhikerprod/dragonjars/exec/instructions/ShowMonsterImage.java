package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Segment;
import com.hitchhikerprod.dragonjars.exec.VideoHelper;
import com.hitchhikerprod.dragonjars.tasks.MonsterAnimationTask;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ShowMonsterImage implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr(OPCODE);

        final int monsterId = i.getAL();
        if (monsterId == i.taskManager().activeMonster()) return nextIP;

        i.taskManager().disableMonsterAnimation();
        if (monsterId == 0xff) {
            // Covers [4c/078e], which otherwise is probably expecting to draw straight to the screen
            i.drawStringBuffer();
            final PixelRectangle gameplayArea = i.fg().getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel();
            i.fg().drawRectangle(gameplayArea, (byte)0);

            final int nextChunkId = i.memory().segmentToChunk(nextIP.segment());
            final int nextAddress = nextIP.offset();

            final SleepTask sleepTask = new SleepTask(1000);
            sleepTask.setOnSucceeded(event -> i.restart(nextChunkId, nextAddress));
            Thread.ofVirtual().start(sleepTask);

            return null;
        }

        final int priChunkId = 0x8a + (2 * monsterId);
        final Segment priSegment = i.memory().allocateSegment(priChunkId, Frob.IN_USE);

        final int secChunkId = priChunkId + 1;
        final Segment secSegment = i.memory().allocateSegment(secChunkId, Frob.IN_USE);

        i.taskManager().enableMonsterAnimation(monsterId);

        final MonsterAnimationTask monsterAnimationTask = new MonsterAnimationTask(i, priSegment.chunk(), secSegment.chunk());

        final EventHandler<WorkerStateEvent> taskEnd = event -> {
            i.memory().freeChunk(priSegment.chunkId());
            i.memory().freeChunk(secSegment.chunkId());
        };
        monsterAnimationTask.setOnSucceeded(taskEnd);
        monsterAnimationTask.setOnFailed(taskEnd);
        monsterAnimationTask.setOnCancelled(taskEnd);

        i.taskManager().startMonsterAnimation(monsterAnimationTask);
        return nextIP;
    }
}
