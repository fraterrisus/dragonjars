package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DivAXHeap implements Instruction {
    // heap[37]:4 <- heap[imm]:4 div ax:2
    // heap[3b]:2 <- heap[imm]:4 mod ax:2
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(1), 1);
        final int op1 = Heap.get(heapIndex).read(4);
//        i.setMulResult(op1); // 0x3dae
        final int op2 = i.getAX(true);
        final int divResult = op1 / op2;
        final int modResult = op1 % op2;
//        i.setMulResult(divResult);
//        i.setDivResult(modResult);
        Heap.get(0x37).write(divResult, 4);
        Heap.get(0x3b).write(modResult, 2);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
