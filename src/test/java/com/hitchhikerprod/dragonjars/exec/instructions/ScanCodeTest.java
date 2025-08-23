package com.hitchhikerprod.dragonjars.exec.instructions;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanCodeTest {
    @Test
    public void leftArrow() {
        assertEquals(0x88, ReadKeySwitch.scanCode(KeyCode.LEFT, false, false));
    }

    @Test
    public void letterD() {
        assertEquals(0xc4, ReadKeySwitch.scanCode(KeyCode.D, false, false));
    }

    @Test
    public void digits() {
        assertEquals(0x01, ReadKeySwitch.scanCode(KeyCode.DIGIT5, false, false));
    }

    @Test
    public void escape() {
        assertEquals(0x9b, ReadKeySwitch.scanCode(KeyCode.ESCAPE, false, false));
    }

    @Test
    public void questionMark() {
        assertEquals(0xbf, ReadKeySwitch.scanCode(KeyCode.SLASH, true, false));
    }

    @Test
    public void slash() {
        assertEquals(0xaf, ReadKeySwitch.scanCode(KeyCode.SLASH, false, false));
    }
}