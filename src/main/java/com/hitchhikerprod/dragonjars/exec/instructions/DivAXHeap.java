package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DivAXHeap implements Instruction {
    // heap[37]:4 <- heap[imm]:4 div ax:2
    // heap[3b]:2 <- heap[imm]:4 mod ax:2
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.readByte(ip.incr(1));
        final int op1 = i.heap().read(heapIndex, 4);
        i.setMulResult(op1); // 0x3dae
        final int op2 = i.getAX(true);
        final int divResult = op1 / op2;
        final int modResult = op1 % op2;
        i.setMulResult(divResult);
        i.setDivResult(modResult);
        i.heap().write(0x37, 4, divResult);
        i.heap().write(0x3b, 2, modResult);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
