package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class OrAXImm implements Instruction {
    // ax:w <- ax:w | imm:w
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int op1 = i.memory().read(ip.incr(1), (i.isWide()) ? 2 : 1);
        final int op2 = i.getAX();
        final int result = op1 | op2;
        i.setAX(result);
        return ip.incr(OPCODE + wordSize(i));
    }
}
