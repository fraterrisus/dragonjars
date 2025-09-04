package com.hitchhikerprod.dragonjars.data;

public record CharRectangle(int x0, int y0, int x1, int y1) {
    public boolean contains(int x, int y) {
        return x >= x0 && y >= y0 && x < x1 && y < y1;
    }

    public PixelRectangle toPixel() {
        return new PixelRectangle(x0 * 8, y0, x1 * 8, y1);
    }

    @Override
    public String toString() {
        return String.format("rect(x0:%03dch,y0:%03dpx,x1:%03dch,y1:%03dpx)", x0, y0, x1, y1);
    }
}
