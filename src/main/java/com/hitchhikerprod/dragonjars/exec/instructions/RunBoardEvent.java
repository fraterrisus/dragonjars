package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.function.Supplier;

public class RunBoardEvent implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation location = i.getPartyLocation();
        final Address nextIP = i.getIP().incr();

        if (location.mapId() != (Heap.get(Heap.DECODED_BOARD_ID).read() & 0x7f)) return nextIP;

        final MapData.Square square = i.mapDecoder().getSquare(location.pos());
        if (square.eventId() != Heap.get(Heap.RECENT_EVENT).read()) {
            Heap.get(Heap.RECENT_EVENT).write(0);
            if (square.eventId() != 0) {
                Heap.get(Heap.NEXT_EVENT).write(square.eventId(), 1);
                final Address target = new Address(
                        Heap.get(Heap.BOARD_1_SEGIDX).read(),
                        i.mapDecoder().getEventPointer(square.eventId() + 1)
                );
                final After after = new After(i, location, nextIP);
                i.reenter(target, after);
                return null;
            }
        }

        final int address = i.mapDecoder().getEventPointer(0);
        i.reenter(0x46 + location.mapId(), address, () -> nextIP);
        return null;
    }

    private static class After implements Supplier<Address> {
        private final Interpreter i;
        private final PartyLocation oldLoc;
        private final Address nextIP;

        private After(Interpreter i, PartyLocation oldLoc, Address nextIP) {
            this.i = i;
            this.oldLoc = oldLoc;
            this.nextIP = nextIP;
        }

        @Override
        public Address get() {
            // maybe should be oldLoc.mapId()?
            // we're trying to catch when the event program moved us to a new board and exit quickly
            if (Heap.get(Heap.BOARD_ID).read(1) != Heap.get(Heap.DECODED_BOARD_ID).read(1)) return nextIP;

            final int address = i.mapDecoder().getEventPointer(0);
            i.reenter(0x46 + oldLoc.mapId(), address, () -> nextIP);
            return null;
        }
    }
}
