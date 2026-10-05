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
        final PartyLocation location = Heap.getPartyLocation();
        final Address nextIP = i.getIP().incr();

        if (location.mapId() != (Heap.get(Heap.DECODED_BOARD_ID).read() & 0x7f)) return nextIP;

        final MapData.Square square = i.mapDecoder().getSquare(location.pos());
        if (square.specialId() != Heap.get(Heap.RECENT_SPECIAL).read()) {
            Heap.get(Heap.RECENT_SPECIAL).write(0);
            if (square.specialId() != 0) {
                Heap.get(Heap.NEXT_SPECIAL).write(square.specialId(), 1);
                final int eventPointer = i.mapDecoder().getEventPointer(square.specialId());
                if (eventPointer != 0) { // event was disabled dynamically
                    i.reenter(
                            i.memory().getSegmentChunk(Heap.get(Heap.BOARD_1_SEGIDX).read()),
                            eventPointer,
                            new After(i, location)
                    );
                    return nextIP;
                }
            }
        }

        final int address = i.mapDecoder().getEventPointer(0);
        i.reenter(0x46 + location.mapId(), address, () -> {});
        return nextIP;
    }

    private record After (Interpreter i, PartyLocation oldLoc) implements Runnable {
        @Override
        public void run() {
            // maybe should be oldLoc.mapId()?
            // we're trying to catch when the event program moved us to a new board and exit quickly
            if (Heap.get(Heap.BOARD_ID).read(1) != Heap.get(Heap.DECODED_BOARD_ID).read(1)) return;

            final int address = i.mapDecoder().getEventPointer(0);
            i.reenter(0x46 + oldLoc.mapId(), address, () -> {});
        }
    }
}
