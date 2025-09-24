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
        final Heap.Access selectedPC = Heap.get(Heap.SELECTED_PC);
        final Heap.Access partySize = Heap.get(Heap.PARTY_SIZE);
        if (partySize.read() != 0) {
            final int oldSelectedPC = selectedPC.read();
            int charId = 0;
            // The reentrant code might *change the party size* so read it fresh every time
            while (charId < partySize.read()) {
                selectedPC.write(charId);
                i.reenter(new Address(ip.segment(), funcPtr), () -> null);
                charId++;
            }
            selectedPC.write(oldSelectedPC);
        }

        return ip.incr(OPCODE + ADDRESS);
    }
}
