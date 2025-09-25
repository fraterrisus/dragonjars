package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.GridCoordinate;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.data.RawData;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class UnrotateMapView implements Instruction {
    // See RotateMapView for comments
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(), 1);
        final RawData input = new RawData(Heap.get(heapIndex).read(3));

        final PartyLocation loc = Heap.getPartyLocation();
        final GridCoordinate east = loc.pos().translate(1, 0);
        final GridCoordinate south = loc.pos().translate(0, -1);

        switch (loc.facing()) {
            case NORTH -> i.mapDecoder().setSquare(loc.pos(), input.value());
            case EAST -> {
                final int eastEdge = input.getNorthEdge();
                final int northEdge = input.getWestEdge();
                final RawData newEastSquare = RawData.from(i.mapDecoder().getSquare(east)).setWestEdge(eastEdge);
                i.mapDecoder().setSquare(east, newEastSquare.value());
                final RawData newThisSquare = RawData.from(i.mapDecoder().getSquare(loc.pos())).setNorthEdge(northEdge);
                i.mapDecoder().setSquare(loc.pos(), newThisSquare.value());
            }
            case SOUTH -> {
                final int southEdge = input.getNorthEdge();
                final int eastEdge = input.getWestEdge();
                final RawData newSouthSquare = RawData.from(i.mapDecoder().getSquare(south)).setNorthEdge(southEdge);
                i.mapDecoder().setSquare(south, newSouthSquare.value());
                final RawData newEastSquare = RawData.from(i.mapDecoder().getSquare(east)).setWestEdge(eastEdge);
                i.mapDecoder().setSquare(east, newEastSquare.value());
            }
            case WEST -> {
                final int westEdge = input.getNorthEdge();
                final int southEdge = input.getWestEdge();
                final RawData newThisSquare = RawData.from(i.mapDecoder().getSquare(loc.pos())).setWestEdge(westEdge);
                i.mapDecoder().setSquare(loc.pos(), newThisSquare.value());
                final RawData newSouthSquare = RawData.from(i.mapDecoder().getSquare(south)).setNorthEdge(southEdge);
                i.mapDecoder().setSquare(south, newSouthSquare.value());
            }
        }

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
