package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class AddAXImm implements Instruction {
    // See AddAXHeap for comments
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final ALU.Result result;
        if (i.isWide()) {
            result = ALU.addWord(i.getAX(), i.memory().read(ip.incr(1), 2));
            i.setAX(result.value());
        } else {
            result = ALU.addByte(i.getAL(), i.memory().read(ip.incr(1), 1));
            i.setAL(result.value());
        }
        i.setCarryFlag(result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + i.width());
    }
}
