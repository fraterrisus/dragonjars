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
            result = ALU.subWord(i.getAX(), i.memory().read(ip.incr(1), 2));
            System.out.format("  cmp ax=%04x imm=%04x -> zf:%d sf:%d cf:%d\n",
                    i.getAX(), i.memory().read(ip.incr(1), 2), result.zero() ? 1 : 0,
                    result.sign() ? 1 : 0, result.carry() ? 0 : 1);
        } else {
            result = ALU.subByte(i.getAL(), i.memory().read(ip.incr(1), 1));
            System.out.format("  cmp al=%02x imm=%02x -> zf:%d sf:%d cf:%d\n",
                    i.getAL(), i.memory().read(ip.incr(1), 1), result.zero() ? 1 : 0,
                    result.sign() ? 1 : 0, result.carry() ? 0 : 1);
        }
        i.setCarryFlag(!result.carry());
        i.setSignFlag(result.sign());
        i.setZeroFlag(result.zero());
        return ip.incr(OPCODE + wordSize(i));
    }
}
