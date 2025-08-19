package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RecurseOverParty implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.setWidth(false);
        i.setAH(0x00);
        final int funcPtr = i.memory().read(ip.incr(1), 2);

        final int partySize = i.heap(0x1f).read();
        if (partySize != 0) {
            final int save_heap06 = i.heap(0x06).read();
            for (int charId = 0; charId < partySize; charId++) {
                i.heap(0x06).write(charId);
                i.start(new Address(ip.segment(), funcPtr));
            }
            i.heap(0x06).write(save_heap06);
        }

        return ip.incr(OPCODE + ADDRESS);
    }
}
