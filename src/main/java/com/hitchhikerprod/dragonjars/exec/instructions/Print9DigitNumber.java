package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class Print9DigitNumber implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final int value = i.heap(heapIndex).read(4);
        Instructions.printNumber(i, value);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
