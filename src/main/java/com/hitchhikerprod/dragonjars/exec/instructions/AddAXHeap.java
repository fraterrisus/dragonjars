package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AddAXHeap implements Instruction {
    // Even though the assembly copies the meta CF flag into CF with SHR, it uses ADD to do
    // the actual arithmetic and not ADC, so the input CF is ignored. The physical CF is
    // copied to meta CF using RCL later, so the two operations do need to be symmetrical.
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final ALU.Result result;
        final int heapIndex = i.readByte(ip.incr(1));
        if (i.isWide()) {
            result = ALU.addWord(i.getAX(), i.getHeapBytes(heapIndex, 2));
            i.setAX(result.value());
        } else {
            result = ALU.addByte(i.getAL(), i.getHeapBytes(heapIndex, 1));
            i.setAL(result.value());
        }
        // Unlike the subtraction and compare operations, we DON'T invert CF here
        i.setCarryFlag(result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
