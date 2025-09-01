package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RecurseOverInventory implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        i.setWidth(false); // 0x4237
        i.setAH(0x00);
        final Address ip = i.getIP();
        final Address nextIP = ip.incr(OPCODE + ADDRESS);
        final int functionPointer = i.memory().read(ip.incr(), 2);
        final int marchingOrder = i.heap(Heap.SELECTED_PC).read();
        final int pcBaseAddress = i.heap(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        i.heap(Heap.SELECTED_ITEM).write(0);
        for (int slotId = 0; slotId < 12; slotId++) {
            final int itemBaseAddress = pcBaseAddress + 0xec + (0x17 * slotId);
            if (i.memory().read(Interpreter.PARTY_SEGMENT, itemBaseAddress + 0x0b, 1) == 0) break;

            i.reenter(new Address(ip.segment(), functionPointer), () -> null);
            if (i.getCarryFlag()) return nextIP;
            i.heap(Heap.SELECTED_ITEM).write(slotId + 1);
        }
        i.setCarryFlag(false);
        return nextIP;
    }
}
