package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class SubAXHeap implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final ALU.Result result;
        if (i.isWide()) {
            result = ALU.subWord(i.getAX(), Heap.get(heapIndex).read(2));
            i.setAX(result.value());
        } else {
            result = ALU.subByte(i.getAL(), Heap.get(heapIndex).read(1));
            i.setAL(result.value());
        }
        // I don't know why the assembly code flips CF before writing it, but it does
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
