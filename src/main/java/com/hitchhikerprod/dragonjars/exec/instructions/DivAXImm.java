package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
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
        final int op2 = (i.isWide()) ? i.readWord(ip.incr(1)) : i.readByte(ip.incr(1));
        final int op1 = i.getAX(true);
        i.setHeapBytes(0x37, 4, op1 / op2);
        i.setHeapBytes(0x3b, 2, op1 % op2);
        return ip.incr(OPCODE + wordSize(i));
    }
}
