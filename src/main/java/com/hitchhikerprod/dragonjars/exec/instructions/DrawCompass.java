package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.data.ImageDecoder;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawCompass implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation loc = i.getPartyLocation();
        final int regionId = ImageDecoder.COMPASS_N + loc.facing().index();
        i.imageDecoder().decodeRomImage(regionId);
        return null;
    }
}
