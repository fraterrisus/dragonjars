package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RecurseOverParty implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.setWidth(false);
        i.setAH(0x00);
        final int funcPtr = i.memory().read(ip.incr(1), 2);
        final Heap.Access selectedPC = i.heap(Heap.SELECTED_PC);

        final int partySize = i.heap(Heap.PARTY_SIZE).read();
        if (partySize != 0) {
            final int oldSelectedPC = selectedPC.read();
            for (int charId = 0; charId < partySize; charId++) {
                selectedPC.write(charId);
                i.reenter(new Address(ip.segment(), funcPtr), () -> null);
            }
            selectedPC.write(oldSelectedPC);
        }

        return ip.incr(OPCODE + ADDRESS);
    }
}
