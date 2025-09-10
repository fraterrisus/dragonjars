package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class EraseSquareSpecial implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation location = i.getPartyLocation();
        i.mapDecoder().eraseSquareSpecial(location.pos());
        return i.getIP().incr();
    }
}
