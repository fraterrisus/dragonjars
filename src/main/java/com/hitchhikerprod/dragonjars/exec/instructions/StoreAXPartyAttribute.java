package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class StoreAXPartyAttribute implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        Heap.get(Heap.PC_DIRTY + marchingOrder).write(0x0);
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int attributeOffset = i.memory().read(ip.incr(1), 1);
        final int value = i.getAX();
        i.memory().write(Interpreter.PARTY_SEGMENT, pcBaseAddress + attributeOffset, i.isWide() ? 2 : 1, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
