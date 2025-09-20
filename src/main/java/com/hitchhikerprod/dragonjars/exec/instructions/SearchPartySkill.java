package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class SearchPartySkill implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int skillOffset = i.memory().read(ip.incr(1), 1);
        final int searchValue = i.memory().read(ip.incr(2), 1);
        for (int charId = 0; charId < Heap.get(Heap.PARTY_SIZE).read(); charId++) {
            Heap.get(Heap.SELECTED_PC).write(charId);
            final int charBaseAddress = Heap.get(Heap.MARCHING_ORDER + charId).read() << 8;
            final int skillValue = i.memory().read(Interpreter.PARTY_SEGMENT, charBaseAddress + skillOffset, 1);
            if (skillValue >= searchValue) {
                i.setCarryFlag(false);
                return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
            }
        }
        i.setCarryFlag(true);
        return ip.incr(OPCODE + IMMEDIATE + IMMEDIATE);
    }
}
