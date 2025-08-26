package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class PickUpItem implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr();
        final Address itemToPickUp = new Address(i.getDS(), i.getAX(true));
        final int marchingOrder = i.heap(Heap.SELECTED_PC).read();
        final int pcBaseAddress = i.heap(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        for (int slotId = 0; slotId < 12; slotId++) {
            final int slotBaseAddress = pcBaseAddress + 0xec + (0x17 * slotId);
            final Address slotAddress = new Address(Interpreter.PARTY_SEGMENT, slotBaseAddress);
            if (i.memory().read(slotAddress.incr(0x0b), 1) != 0) continue;

            i.memory().writeList(slotAddress, i.memory().readList(itemToPickUp, 0x17));
            i.heap(Heap.SELECTED_ITEM).write(slotId);
            i.setZeroFlag(true);
            return nextIP;
        }
        i.setZeroFlag(false);
        return nextIP;
    }
}
