package com.hitchhikerprod.dragonjars.data;

public class PowerInt {
    private final int mantissa;
    private final int exponent;

    public PowerInt(byte b) {
        this.exponent = (b & 0xe0) >> 0x5;
        this.mantissa = (b & 0x1f);
    }

    private PowerInt(int exponent, int mantissa) {
        this.exponent = exponent;
        this.mantissa = mantissa;
    }

    public Integer toInteger() {
        if (exponent == 0 && mantissa == 0) {
            return null;
        } else {
            int value = mantissa;
            for (int i = 0; i < exponent; i++) {
                value *= 10;
            }
            return value;
        }
    }

    public PowerInt plus(int a) {
        return new PowerInt(this.exponent, this.mantissa + a);
    }

    public String toString() {
        return String.valueOf(toInteger());
    }
}
