package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Action;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.Optional;

public class FindBoardAction implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        // The assembly for this is a lot more complicated; this is a major candidate for bug fixes.
        final Address nextIP = i.getIP().incr();
        if (Heap.get(Heap.BOARD_ID).read() != Heap.get(Heap.DECODED_BOARD_ID).read()) return nextIP;
        final PartyLocation loc = i.getPartyLocation();
        final int actionId = Heap.get(0x85).read();
        final MapData.Square square = i.mapDecoder().getSquare(loc.pos());
        final Optional<Action> action = i.mapDecoder().findAction(actionId, square.eventId());
        if (action.isPresent()) {
            final Address target = new Address(
                    Heap.get(Heap.BOARD_1_SEGIDX).read(),
                    i.mapDecoder().getEventPointer(action.get().event() + 1)
            );
            i.reenter(target, () -> { i.setCarryFlag(true); return nextIP; });
            return null;
        } else {
            i.setCarryFlag(false);
            return nextIP;
        }
    }
}
