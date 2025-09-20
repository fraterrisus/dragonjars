package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class ReadPCField implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int wordOffset = i.memory().read(ip.incr(), 1);
        final int marchingOrder = Heap.get(Heap.SELECTED_PC).read();
        final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + marchingOrder).read() << 8;
        final int pcOffset = wordOffset + i.getBL(); // 0x0c to skip PC name
        final int pcWord = i.memory().read(Interpreter.PARTY_SEGMENT, pcBaseAddress + pcOffset, 2);
        i.setAX(pcWord);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
