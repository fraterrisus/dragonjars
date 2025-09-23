package com.hitchhikerprod.dragonjars.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GridCoordinateTest {
    @Test
    public void testModulusInside() {
        final GridCoordinate in = new GridCoordinate(5, 5);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(5, out.x());
        assertEquals(5, out.y());
    }

    @Test
    public void testModulusAbove() {
        final GridCoordinate in = new GridCoordinate(13, 13);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(3, out.x());
        assertEquals(3, out.y());
    }

    @Test
    public void testModulusWayAbove() {
        final GridCoordinate in = new GridCoordinate(25, 27);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(5, out.x());
        assertEquals(7, out.y());
    }

    @Test
    public void testModulusNegative() {
        final GridCoordinate in = new GridCoordinate(-2, -3);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(8, out.x());
        assertEquals(7, out.y());
    }

    @Test
    public void testModulusVeryNegative() {
        final GridCoordinate in = new GridCoordinate(-32, -33);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(8, out.x());
        assertEquals(7, out.y());
    }

    @Test
    public void testModulusZero() {
        final GridCoordinate in = new GridCoordinate(0, 0);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(0, out.x());
        assertEquals(0, out.y());
    }

    @Test
    public void testModulusExact() {
        final GridCoordinate in = new GridCoordinate(10, 10);
        final GridCoordinate out = in.modulus(10, 10);
        assertEquals(0, out.x());
        assertEquals(0, out.y());
    }

    @Test
    public void testOutsideInside() {
        assertFalse(new GridCoordinate(5, 5).isOutside(10, 10));
    }

    @Test
    public void testOutsideAbove() {
        assertTrue(new GridCoordinate(15, 5).isOutside(10, 10));
        assertTrue(new GridCoordinate(5, 15).isOutside(10, 10));
    }

    @Test
    public void testOutsideOn() {
        assertTrue(new GridCoordinate(5, 10).isOutside(10, 10));
        assertTrue(new GridCoordinate(10, 5).isOutside(10, 10));
    }

    @Test
    public void testOutsideZero() {
        assertFalse(new GridCoordinate(0, 5).isOutside(10, 10));
        assertFalse(new GridCoordinate(5, 0).isOutside(10, 10));
    }

    @Test
    public void testOutsideNegative() {
        assertTrue(new GridCoordinate(-1, 5).isOutside(10, 10));
        assertTrue(new GridCoordinate(5, -1).isOutside(10, 10));
    }

}