package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class DivAXImm implements Instruction {
    // heap[37]:4 <- ax:2 div imm:w
    // heap[3b]:2 <- ax:2 mod imm:w
    // 0xc div 0x5 = 0x2 mod 0x2
    // 0x6 div 0x5 = 0x1 mod 0x1
    // 0x54 div 0x7 = 0xc mod 0x0
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op2 = i.memory().read(ip.incr(1), i.width());
        final int op1 = i.getAX(true);
        final int divResult = op1 / op2;
        final int modResult = op1 % op2;
//        i.setMulResult(divResult);
//        i.setDivResult(modResult);
        Heap.get(0x37).write(divResult, 4);
        Heap.get(0x3b).write(modResult, 2);
        i.setAX(divResult);
        return ip.incr(OPCODE + i.width());
    }
}
