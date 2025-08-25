package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class EraseSquareSpecial implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation location = i.getPartyLocation();
        final MapData.Square square = i.mapDecoder().getSquare(location.pos());
        final int newValue = square.rawData() & 0xffff00;
        i.mapDecoder().setSquare(location.pos().x(), location.pos().y(), newValue);
        return i.getIP().incr();
    }
}
