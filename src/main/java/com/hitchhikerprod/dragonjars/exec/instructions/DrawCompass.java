package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.VideoHelper;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DrawCompass implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation loc = Heap.getPartyLocation();
        final int regionId = VideoHelper.COMPASS_N + loc.facing().index();
        i.fg().drawRomImage(regionId);
        return null;
    }
}
