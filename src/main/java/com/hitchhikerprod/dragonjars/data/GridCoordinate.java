package com.hitchhikerprod.dragonjars.data;

public record GridCoordinate(int x, int y) {
    public GridCoordinate modulus(int maxx, int maxy) {
        int newx = x;
        int newy = y;
        while (newx < 0) newx += maxx;
        while (newx >= maxx) newx -= maxx;
        while (newy < 0) newy += maxy;
        while (newy >= maxy) newy -= maxy;
        return new GridCoordinate(newx, newy);
    }

    public boolean isOutside(int maxx, int maxy) {
        return (x < 0 || x >= maxx || y < 0 || y >= maxy);
    }
}
