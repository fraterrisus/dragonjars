package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class FindBoardAction implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        // The assembly for this is a lot more complicated; this is a major candidate for bug fixes.
        final Address nextIP = i.getIP().incr();
        if (i.heap(Heap.BOARD_ID).read() != i.heap(Heap.DECODED_BOARD_ID).read()) return nextIP;
        final PartyLocation loc = i.getPartyLocation();
        final int actionId = i.heap(0x85).read();
        final MapData.Square square = i.mapDecoder().getSquare(loc.pos());
        i.mapDecoder().findAction(actionId, square.eventId()).ifPresent(action -> {
            final Address target = new Address(
                    i.heap(Heap.BOARD_1_SEGIDX).read(),
                    i.mapDecoder().getEventPointer(action.special() + 1)
            );
            i.reenter(target, () -> { i.setCarryFlag(true); return nextIP; });
        });
        i.setCarryFlag(false);
        return nextIP;
    }
}
