package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawCompass implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation loc = i.getPartyLocation();
        final int regionId = 0x0a + loc.facing().index();
        i.getImageWriter(writer -> i.imageDecoder().decodeRomImage(regionId, writer));
        return null;
    }
}
