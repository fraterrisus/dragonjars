package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RotateMapView implements Instruction {
    // This is a bit of a cheat. The assembly reuses adjustForFacing() to load 4b of wall metadata into the target heap
    // slot (indexed by the 1b immediate). But in practice, the only time it's ever used is 06/03eb (the handler for
    // D:Create Wall) and 06/0409 (the handler for D:Soften Stone), and even then the actual check logic is based on
    // heap[26], where we've already loaded the wall metadata for whatever's right in front of us. So what the assembly
    // is trying to do is modify that metadata -- and ONLY that first byte! -- and then write it back (the job of
    // UnrotateMapView). So, long story short, all we do here is copy [26] to the target heap slot.
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(), 1);
        i.heap(heapIndex).write(i.heap(Heap.WALL_METADATA).read());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
