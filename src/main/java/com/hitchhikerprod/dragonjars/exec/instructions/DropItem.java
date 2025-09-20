package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DropItem implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        int slotId = Heap.get(Heap.SELECTED_ITEM).read();
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        while (slotId < 11) {
            final int slotBaseAddress = pcBaseAddress + 0xec + (0x17 * slotId);
            final Address slotAddress = new Address(Interpreter.PARTY_SEGMENT, slotBaseAddress);
            i.memory().writeList(slotAddress, i.memory().readList(slotAddress.incr(0x17), 0x17));
            slotId++;
        }
        final int slotBaseAddress = pcBaseAddress + 0xec + (0x17 * 11);
        final Address slotAddress = new Address(Interpreter.PARTY_SEGMENT, slotBaseAddress);
        for (int b = 0; b < 0x17; b++) {
            i.memory().write(slotAddress.incr(b), 1, 0);
        }
        return i.getIP().incr();
    }
}
