package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class UnrotateMapView implements Instruction {
    // See RotateMapView for comments
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int heapIndex = i.memory().read(ip.incr(), 1);
        final int wallByte = i.heap(heapIndex).read();

        final PartyLocation loc = i.getPartyLocation();
        final int x;
        final int y;
        final int shiftDistance;
        switch (loc.facing()) {
            case NORTH -> {
                x = loc.pos().x();
                y = loc.pos().y();
                shiftDistance = 4;
            }
            case EAST -> {
                x = loc.pos().x() + 1;
                y = loc.pos().y();
                shiftDistance = 0;
            }
            case SOUTH -> {
                x = loc.pos().x();
                y = loc.pos().y() - 1;
                shiftDistance = 4;
            }
            case WEST -> {
                x = loc.pos().x();
                y = loc.pos().y();
                shiftDistance = 0;
            }
            default -> throw new IllegalArgumentException("This shouldn't be possible");
        }

        final MapData.Square square = i.mapDecoder().getSquare(x, y);
        final int bitmask = 0xffffff & ~(0xf << shiftDistance);
        final int oldValue = square.rawData() & bitmask;
        final int newValue = oldValue | (wallByte & 0xf) << shiftDistance;
        i.mapDecoder().setSquare(x, y, newValue);

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
