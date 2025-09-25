package com.hitchhikerprod.dragonjars.data;

public record RawData(int value) {
    public static RawData from(MapData.Square sq) {
        return new RawData(sq.rawData());
    }

    public int getNorthEdge() {
        return (value & 0xf0) >> 4;
    }

    public int getWestEdge() {
        return value & 0x0f;
    }

    public RawData setNorthEdge(int edge) {
        return new RawData((value & 0xffff0f) | (edge << 4));
    }

    public RawData setWestEdge(int edge) {
        return new RawData((value & 0xfffff0) | edge);
    }

    @Override
    public String toString() {
        return String.format("RawData[value=%06x])", value);
    }
}
