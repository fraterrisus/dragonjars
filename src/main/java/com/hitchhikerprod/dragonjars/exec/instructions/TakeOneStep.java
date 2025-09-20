package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Facing;
import com.hitchhikerprod.dragonjars.data.GridCoordinate;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class TakeOneStep implements Instruction {
    private final boolean backwards;

    public TakeOneStep(boolean backwards) {
        this.backwards = backwards;
    }

    @Override
    public Address exec(Interpreter i) {
        final PartyLocation loc = i.getPartyLocation();
        final Facing direction = backwards ? loc.facing().reverse() : loc.facing();
        moveOneStep(i, loc.pos(), direction);
        return i.getIP().incr();
    }

    public static void moveOneStep(Interpreter i, GridCoordinate pos, Facing facing) {
        int x = pos.x();
        int y = pos.y();
        switch (facing) {
            case NORTH -> y++; // could use ALU.addByte here
            case EAST -> x++;
            case SOUTH -> y--;
            case WEST -> x--;
        }
        // TODO: make sure we get the weird wrapping behavior on some maps right
        // (it's probably just incrementing past 0xff)
        if (i.mapDecoder().isWrapping()) {
            x = x % Heap.get(Heap.BOARD_MAX_X).read();
            y = y % Heap.get(Heap.BOARD_MAX_Y).read();
        }
        Heap.get(Heap.PARTY_Y).write(y);
        Heap.get(Heap.PARTY_X).write(x);
    }
}
