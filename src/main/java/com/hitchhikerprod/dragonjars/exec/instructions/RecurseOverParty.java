package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RecurseOverParty implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.setWidth(false);
        i.setAH(0x00);
        final int funcPtr = i.readWord(ip.incr(1));

        final int partySize = i.heap().read(0x1f, 1);
        if (partySize != 0) {
            final int save_heap06 = i.heap().read(0x06, 1);
            for (int charId = 0; charId < partySize; charId++) {
                i.heap().write(0x06, 1, charId);
                i.start(new Address(ip.segment(), funcPtr));
            }
            i.heap().write(0x06, 1, save_heap06);
        }

        return ip.incr(OPCODE + ADDRESS);
    }
}
