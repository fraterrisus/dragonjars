package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class LoadAXImm implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        // in narrow mode, reading two bytes doesn't hurt and we just don't write AH
        final int value = i.memory().read(ip.incr(1), 2);
        i.setAX(value);
        return ip.incr(OPCODE + wordSize(i));
    }
}
