package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXPartyAttribute implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int attributeOffset = i.memory().read(ip.incr(1), 1);
        final int value = i.memory().read(
                Interpreter.PARTY_SEGMENT,
                pcBaseAddress + attributeOffset,
                i.isWide() ? 2 : 1
        );
        i.setAX(value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
