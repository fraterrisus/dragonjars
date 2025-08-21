package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class CmpBLImm implements Instruction {
    // See CmpAXHeap for comments
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final ALU.Result result;
        result = ALU.subByte(i.getBL(), i.memory().read(ip.incr(1), 1));
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
//        System.out.format("  cmp bl=%02x imm=%02x -> zf:%d sf:%d cf:%d\n",
//                i.getBL(), i.memory().read(ip.incr(1), 1), result.zero() ? 1 : 0,
//                result.sign() ? 1 : 0, result.carry() ? 0 : 1);
        return ip.incr(OPCODE + IMMEDIATE);
    }
}
