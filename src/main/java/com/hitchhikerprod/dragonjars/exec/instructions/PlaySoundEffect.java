package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PlaySoundEffect implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int soundId = i.memory().read(ip.incr(1), 1);

        if (soundId >= 1 && soundId <= 3) {
            i.app().musicService().playSimpleSound(soundId);
        } else if (soundId >= 4 && soundId <= 10) {
            final int chunkId = 0xfc + soundId;
            final int segmentId = i.getSegmentForChunk(chunkId, Frob.IN_USE);
            final Chunk data = i.memory().getSegment(segmentId);
            i.memory().setSegmentFrob(segmentId, Frob.FREE);
            i.app().musicService().playChunkSound(data);
        }

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
