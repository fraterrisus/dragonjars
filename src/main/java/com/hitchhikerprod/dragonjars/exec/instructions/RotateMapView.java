package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.data.RawData;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class RotateMapView implements Instruction {
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(), 1);

        final PartyLocation loc = Heap.getPartyLocation();

        // The four edges surrounding us are:
        //   N: the North wall of this square
        //   E: the West wall of the square at (+1,0)
        //   S: the North wall of the square at (0,-1)
        //   w: the West wall of this square
        final RawData thisSquare = RawData.from(i.mapDecoder().getSquare(loc.pos()));
        final RawData eastSquare = RawData.from(i.mapDecoder().getSquare(loc.pos().translate(1, 0)));
        final RawData southSquare = RawData.from(i.mapDecoder().getSquare(loc.pos().translate(0, -1)));

        final int northEdge = thisSquare.getNorthEdge();
        final int westEdge = thisSquare.getWestEdge();
        final int southEdge = southSquare.getNorthEdge();
        final int eastEdge = eastSquare.getWestEdge();

        // The goal is to set the "north" wall to the wall we're facing, and the "west" wall to the wall to our left.
        // If we're facing north, no action is needed.
        final RawData newData = switch (loc.facing()) {
            case NORTH -> thisSquare;
            case EAST -> thisSquare.setNorthEdge(eastEdge).setWestEdge(northEdge);
            case SOUTH -> thisSquare.setNorthEdge(southEdge).setWestEdge(eastEdge);
            case WEST -> thisSquare.setNorthEdge(westEdge).setWestEdge(southEdge);
        };

        Heap.get(heapIndex).write(newData.value(), 3);
        return ip.incr(OPCODE + IMMEDIATE);
    }

}
