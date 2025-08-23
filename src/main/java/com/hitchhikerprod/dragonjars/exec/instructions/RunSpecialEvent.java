package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RunSpecialEvent implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation location = i.getPartyLocation();
        final MapData.Square square = i.mapDecoder().getSquare(location.pos());
        if (square.eventId() != 0) {
            // TODO
            System.out.println("Can't run events yet");
        }
        return i.getIP().incr();
    }
}
