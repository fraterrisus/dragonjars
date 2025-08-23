package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StepForward implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation loc = i.getPartyLocation();
        int x = loc.pos().x();
        int y = loc.pos().y();
        switch (loc.facing()) {
            case NORTH -> y++;
            case EAST -> x++;
            case SOUTH -> y--;
            case WEST -> x--;
        }
        final int flags = i.mapDecoder().getFlags(); // heap(0x23).get();
        // TODO: make sure we get the weird wrapping behavior on some maps right
        // (it's probably just incrementing past 0xff)
        if ((flags & 0x02) > 0) { // map wraps
            x = x % i.heap(0x21).read();
            y = y % i.heap(0x22).read();
        }
        i.heap(Heap.PARTY_Y).write(y);
        i.heap(Heap.PARTY_X).write(x);
        return i.getIP().incr();
    }
}
