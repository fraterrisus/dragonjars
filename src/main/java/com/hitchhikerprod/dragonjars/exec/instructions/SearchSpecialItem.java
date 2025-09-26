package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Item;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Memory;

public class SearchSpecialItem implements Instruction {
    // TODO: Check?
    @Override
    public Address exec(Interpreter i) {
        final Address nextIP = i.getIP().incr(OPCODE);
        final int boardItemId = i.getAL();
        final Item boardItem = i.mapDecoder().getItem(boardItemId);
        final int partySize = Heap.get(Heap.PARTY_SIZE).read();
        for (int pcId = 0; pcId < partySize; pcId++) {
            final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + pcId).read() << 8;
            for (int slotId = 0; slotId < 12; slotId++) {
                final int slotBaseAddress = pcBaseAddress + Memory.PC_INVENTORY + (0x17 * slotId);
                final Chunk partyChunk = i.memory().getSegment(Interpreter.PARTY_SEGMENT);
                final Item myItem = new Item(partyChunk).decode(slotBaseAddress);
                if (boardItem.equals(myItem)) {
                    Heap.get(Heap.SELECTED_PC).write(pcId);
                    Heap.get(Heap.SELECTED_ITEM).write(slotId);
                    i.setZeroFlag(true);
                    return nextIP;
                }
            }
        }
        i.setZeroFlag(false);
        return nextIP;
    }

}
