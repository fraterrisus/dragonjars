package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public class ReadKeySwitch implements Instruction {
    @FunctionalInterface
    public interface KeyDetector {
        boolean match(KeyEvent ev);
    }

    public record KeyAction(
            int scanCode,
            KeyDetector function,
            Address destination
    ) {}

    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        i.drawString313e(); //?
        final int imm1 = i.memory().read(ip.incr(1), 1);
        int imm2 = i.memory().read(ip.incr(2), 1);
        // [2a44] <- imm1
        // [2a45] <- imm2
        int pointer = ip.offset() + 3;
        // [4a7c] <- imm2 & 0x20;
        if ((imm2 & 0x10) != 0) {
            // imm2 = i.memory().read(ip.incr(3), 1);
            pointer++;
        }
        // [2a46] <- imm2
        final List<KeyAction> prompts = new ArrayList<>();
        while (true) {
            final int ch = i.memory().read(ip.segment(), pointer, 1);
            if (ch == 0xff) break;
            final int target = i.memory().read(ip.segment(), pointer + 1, 2);
            prompts.add(new KeyAction(ch, detector(ch), new Address(ip.segment(), target)));
            pointer += 3;
        }
        i.setPrompt(prompts);
        return null;
    }

    public static KeyDetector detector(int code) {
        return switch(code) {
            case 0x00 -> (ev) -> true; // this is a default case, hopefully it's last
            case 0x01 -> (ev) -> ev.getCode().isDigitKey();
            case 0x88 -> (ev) -> ev.getCode() == KeyCode.LEFT;
            case 0x8a -> (ev) -> ev.getCode() == KeyCode.DOWN;
            case 0x8b -> (ev) -> ev.getCode() == KeyCode.UP;
            case 0x8d -> (ev) -> ev.getCode() == KeyCode.ENTER;
            case 0x95 -> (ev) -> ev.getCode() == KeyCode.RIGHT;
            case 0x9b -> (ev) -> ev.getCode() == KeyCode.ESCAPE;
            case 0xbf -> (ev) -> (ev.getCode() == KeyCode.SLASH) && ev.isShiftDown();
            default -> {
                try {
                    final KeyCode kc = KeyCode.valueOf(String.valueOf((char)(code & 0x7f)));
                    yield (ev) -> ev.getCode() == kc;
                } catch (IllegalArgumentException e) {
                    System.out.format("Invalid key code: %02x\n", code);
                    yield (ev) -> false;
                }
            }
        };
    }

    public static int scanCode(KeyCode key, boolean withShift, boolean withControl) {
        return switch(key) {
            case LEFT -> 0x88;
            case DOWN -> 0x8a;
            case UP -> 0x8b;
            case ENTER -> 0x8d;
            case RIGHT -> 0x95;
            case ESCAPE -> 0x9b;
            case SLASH -> (withShift) ? 0xbf : 0xaf;
            case DIGIT0, DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6, DIGIT7, DIGIT8, DIGIT9 -> 0x01;
            default -> 0x80 | key.getCode();
        };
    }
}
