package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class WritePCField implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int newValue = i.getAX();
        final int wordOffset = i.memory().read(ip.incr(), 1);
        final int marchingOrder = i.heap(Heap.SELECTED_PC).read();
        i.heap(Heap.PC_DIRTY + marchingOrder).write(0x0);
        final int pcBaseAddress = i.heap(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int pcOffset = wordOffset + i.getBL(); // 0x0c to skip PC name
        i.memory().write(
                Interpreter.PARTY_SEGMENT,
                pcBaseAddress + pcOffset,
                i.isWide() ? 2 : 1,
                newValue
        );
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
