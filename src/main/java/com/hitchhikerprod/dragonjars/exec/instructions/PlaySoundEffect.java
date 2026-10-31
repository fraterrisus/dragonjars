package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Segment;

public class PlaySoundEffect implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int soundId = i.memory().read(ip.incr(1), 1);

        if (soundId >= 1 && soundId <= 3) {
            i.app().musicService().playSimpleSound(soundId);
        } else if (soundId >= 4 && soundId <= 10) {
            final int chunkId = 0xfc + soundId;
            final Segment musicSegment = i.memory().allocateSegment(chunkId, Frob.IN_USE);
            i.memory().freeSegment(musicSegment.id());
            i.app().musicService().playChunkSound(musicSegment.chunk());
        }

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
