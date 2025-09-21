package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.MonsterAnimationTask;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ShowMonsterImage implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr(OPCODE);

        final int monsterId = i.getAL();
        if (monsterId == i.activeMonster()) return nextIP;

        i.disableMonsterAnimation();

        final int priChunkId = 0x8a + (2 * monsterId);
        final int priSegment = i.getSegmentForChunk(priChunkId, Frob.IN_USE);
        final Chunk priChunk = i.memory().getSegment(priSegment);

        final int secChunkId = priChunkId + 1;
        final int secSegment = i.getSegmentForChunk(secChunkId, Frob.IN_USE);
        final Chunk secChunk = i.memory().getSegment(secSegment);

        i.enableMonsterAnimation(monsterId); //, priSegment, secSegment);

        final MonsterAnimationTask monsterAnimationTask = new MonsterAnimationTask(i, priChunk, secChunk);

        final EventHandler<WorkerStateEvent> taskEnd = event -> {
            i.freeSegmentForChunk(priChunkId);
            i.freeSegmentForChunk(secChunkId);
        };
        monsterAnimationTask.setOnSucceeded(taskEnd);
        monsterAnimationTask.setOnFailed(taskEnd);
        monsterAnimationTask.setOnCancelled(taskEnd);

        i.startMonsterAnimation(monsterAnimationTask);
        return nextIP;
    }
}
