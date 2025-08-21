package com.hitchhikerprod.dragonjars.data;

public class WeaponDamage {
    private final int num;
    private final int sides;

    private static final int[] SIDES = { 4, 6, 8, 10, 12, 20, 30, 100 };

    public WeaponDamage(byte b) {
        this.num = 1 + (b & 0x1f);
        this.sides = SIDES[(b & 0xe0) >> 5];
    }

    public int getNum() {
        return num;
    }

    public int getSides() {
        return sides;
    }

    public String toString() {
        return num + "d" + sides;
    }
}
