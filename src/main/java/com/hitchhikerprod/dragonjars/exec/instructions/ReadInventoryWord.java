package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class ReadInventoryWord implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int byteOffset = i.memory().read(ip.incr(), 1);
        final int marchingOrder = i.heap(Heap.SELECTED_PC).read();
        final int pcBaseAddress = i.heap(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int slotId = i.heap(Heap.SELECTED_ITEM).read();
        final int itemBaseAddress = pcBaseAddress + 0xec + (0x17 * slotId);
        final int itemWord = i.memory().read(Interpreter.PARTY_SEGMENT, itemBaseAddress + byteOffset, 2);
        i.setAX(itemWord);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
