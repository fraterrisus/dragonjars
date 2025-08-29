package com.hitchhikerprod.dragonjars.data;

public record PartyLocation(int mapId, GridCoordinate pos, Facing facing) {
    public GridCoordinate translate(int squareId) {
        return switch (facing) {
            case NORTH -> {
                final int dx = (squareId % 3) - 1;
                final int dy = 3 - (squareId / 3);
                yield new GridCoordinate(pos.x() + dx, pos.y() + dy);
            }
            case SOUTH -> {
                final int dx = 1 - (squareId % 3);
                final int dy = (squareId / 3) - 3;
                yield new GridCoordinate(pos.x() + dx, pos.y() + dy);
            }
            case EAST -> {
                final int dx = 3 - (squareId / 3);
                final int dy = 1 - (squareId % 3);
                yield new GridCoordinate(pos.x() + dx, pos.y() + dy);
            }
            case WEST -> {
                final int dx = (squareId / 3) - 3;
                final int dy = (squareId % 3) - 1;
                yield new GridCoordinate(pos.x() + dx, pos.y() + dy);
            }
        };
    }
}
