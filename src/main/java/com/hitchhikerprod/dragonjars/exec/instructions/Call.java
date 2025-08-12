package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class Call implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int targetAddress = i.readWord(ip.incr(1));
        final Address returnAddress = ip.incr(OPCODE + ADDRESS);
        i.push(returnAddress.offset());
        i.push(returnAddress.offset() >> 8);
        return new Address(ip.segment(), targetAddress);
    }
}
