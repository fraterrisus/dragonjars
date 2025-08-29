package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.PlaySound;

public class PlaySoundEffect implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int soundId = i.memory().read(ip.incr(1), 1);
        System.out.print("PlaySoundEffect(" + soundId + ")\n");

        if (soundId >= 1 && soundId <= 3) {
            PlaySound soundTask = new PlaySound(soundId);
            final Thread soundThread = new Thread(soundTask);
            soundThread.setDaemon(true);
            soundThread.start();
        }
        // TODO
        // soundId 4-a: 0x4ecc call 0x43cc, load chunk soundId + 0xfc (i.e. 0x100 - 0x106)

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
