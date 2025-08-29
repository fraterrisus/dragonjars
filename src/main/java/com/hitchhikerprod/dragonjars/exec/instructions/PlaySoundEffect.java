package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.PlayChunkSound;
import com.hitchhikerprod.dragonjars.tasks.PlaySimpleSound;

public class PlaySoundEffect implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int soundId = i.memory().read(ip.incr(1), 1);
        System.out.print("PlaySoundEffect(" + soundId + ")\n");

        if (soundId >= 1 && soundId <= 3) {
            final PlaySimpleSound soundTask = new PlaySimpleSound(soundId);
            final Thread soundThread = new Thread(soundTask);
            soundThread.setDaemon(true);
            soundThread.start();
        } else if (soundId >= 4 && soundId <= 10) {
            final int chunkId = 0xfc + soundId;
            // getSegmentForChunk has the rolling-add logic included for audio chunks
            final int segmentId = i.getSegmentForChunk(chunkId, Frob.CLEAN);
            final Chunk data = i.memory().getSegment(segmentId);

            final PlayChunkSound task = new PlayChunkSound(data);
            final Thread soundThread = new Thread(task);
            soundThread.setDaemon(true);
            soundThread.start();
        }

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
