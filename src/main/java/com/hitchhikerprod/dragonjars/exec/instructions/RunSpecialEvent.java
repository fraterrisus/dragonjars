package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RunSpecialEvent implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final PartyLocation location = i.getPartyLocation();
        final Address nextIP = i.getIP().incr();

        if (location.mapId() != (i.heap(Heap.BOARD_1_MAPID).read() & 0x7f)) return nextIP;

        final MapData.Square square = i.mapDecoder().getSquare(location.pos());
        if (square.eventId() != i.heap(0x3e).read()) {
            i.heap(0x3e).write(0);
            if (square.eventId() != 0) {
                i.heap(0x3f).write(square.eventId(), 1);

                final int address = i.mapDecoder().getEventPointer(square.eventId() + 1);
                i.start(0x46 + location.mapId(), address);

                if (i.heap(Heap.BOARD_ID).read(1) != i.heap(Heap.BOARD_1_MAPID).read(1)) return nextIP;
            }
        }

        final int address = i.mapDecoder().getEventPointer(0);
        i.start(0x46 + location.mapId(), address);
        return nextIP;
    }
}
