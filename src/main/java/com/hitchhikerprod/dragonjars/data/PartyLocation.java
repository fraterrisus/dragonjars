package com.hitchhikerprod.dragonjars.data;

import com.hitchhikerprod.dragonjars.exec.ALU;

public record PartyLocation(int mapId, GridCoordinate pos, Facing facing) {
    public GridCoordinate translate(int squareId) {
        return switch (facing) {
            case NORTH -> {
                final int dx = (squareId % 3) - 1;
                final int dy = 3 - (squareId / 3);
                yield new GridCoordinate(ALU.addByte(pos.x(), dx).value(), ALU.addByte(pos.y(), dy).value());
            }
            case SOUTH -> {
                final int dx = 1 - (squareId % 3);
                final int dy = (squareId / 3) - 3;
                yield new GridCoordinate(ALU.addByte(pos.x(), dx).value(), ALU.addByte(pos.y(), dy).value());
            }
            case EAST -> {
                final int dx = 3 - (squareId / 3);
                final int dy = 1 - (squareId % 3);
                yield new GridCoordinate(ALU.addByte(pos.x(), dx).value(), ALU.addByte(pos.y(), dy).value());
            }
            case WEST -> {
                final int dx = (squareId / 3) - 3;
                final int dy = (squareId % 3) - 1;
                yield new GridCoordinate(ALU.addByte(pos.x(), dx).value(), ALU.addByte(pos.y(), dy).value());
            }
        };
    }

    @Override
    public String toString() {
        return String.format("PartyLocation[mapId=0x%02x,x=%d,y=%d,facing=%s]",
                mapId, pos.x(), pos.y(), facing);
    }
}
