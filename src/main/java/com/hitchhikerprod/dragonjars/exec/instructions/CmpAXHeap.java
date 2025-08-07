package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class CmpAXHeap implements Instruction {
    // Perform subtraction, set the flags the same way, but don't set the result
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final ALU.Result result;
        if (i.isWide()) {
            result = ALU.subWord(i.getAX(), i.getHeapBytes(heapIndex, 2));
        } else {
            result = ALU.subByte(i.getAL(), i.getHeapBytes(heapIndex, 1));
        }
        // I don't know why the assembly code flips CF before writing it, but it does
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
