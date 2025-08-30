package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class Call implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int targetAddress = i.memory().read(ip.incr(1), 2);
        final Address returnAddress = ip.incr(OPCODE + ADDRESS);
        i.pushWord(returnAddress.offset());
        return new Address(ip.segment(), targetAddress);
    }
}
