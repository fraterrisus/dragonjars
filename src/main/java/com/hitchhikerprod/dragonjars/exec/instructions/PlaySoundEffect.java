package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PlaySoundEffect implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int soundId = i.memory().read(ip.incr(1), 1);
        // TODO
        // soundId 0 : 0x4ecb ret [no sound]
        // soundId 1 : 0x4ea8 call 0x4eb6(dx:0xf0, bx:0x200) [footstep]
        // soundId 2 : 0x4ea0 call 0x4eb6(dx:0x28, bx:0x400) [open door]
        // soundId 3 : 0x4eb0 call 0x4eb6(dx:0xc8, bx:0x800) [hit wall]
        // soundId 4-a: 0x4ecc call 0x43cc, load chunk soundId + 0xfc (i.e. 0x100 - 0x106)
        System.out.print("PlaySoundEffect(" + soundId + ")\n");
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
