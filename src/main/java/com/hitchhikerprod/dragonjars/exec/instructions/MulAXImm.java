package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class MulAXImm implements Instruction {
    // heap[37]:4 <- ax:2 * imm:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op1 = i.memory().read(ip.incr(1), i.isWide() ? 2 : 1);
        final int op2 = i.getAX(true);
        final int result = op1 * op2;
        i.setMulResult(result);
        i.heap().write(0x37, 4, result);
        return ip.incr(OPCODE + wordSize(i));
    }
}
