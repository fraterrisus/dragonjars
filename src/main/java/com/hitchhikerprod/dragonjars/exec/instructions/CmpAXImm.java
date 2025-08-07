package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class CmpAXImm implements Instruction {
    // See CmpAXHeap for comments
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final ALU.Result result;
        if (i.isWide()) {
            result = ALU.subWord(i.getAX(), i.readWord(ip.incr(1)));
        } else {
            result = ALU.subByte(i.getAL(), i.readByte(ip.incr(1)));
        }
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + wordSize(i));
    }
}
