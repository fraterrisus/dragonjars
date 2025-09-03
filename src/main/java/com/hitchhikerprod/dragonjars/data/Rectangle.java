package com.hitchhikerprod.dragonjars.data;

public record Rectangle(int x0, int y0, int x1, int y1) {
    public boolean contains(int x, int y) {
        return x >= x0 && y >= y0 && x < x1 && y < y1;
    }
}
