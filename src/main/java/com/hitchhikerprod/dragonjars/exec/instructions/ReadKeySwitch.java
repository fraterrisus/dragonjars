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
            KeyDetector function,
            Address destination
    ) {}

    // 89 44a2
    //      ^^ hi byte & 0x80 = print footer, index = hi byte & 0x03
    //      ^  hi byte & 0x10 = read a third byte
    //    ^^   lo byte & 0x80: call 0x1eb0 ???

    // "argument" values seen:
    //   0000  everywhere
    //   0080  lots of places;      0x80 = print footer
    //   0083  00/012e
    //   009081  0c/0021, 0c/00c0;  0x80 + 0x10 = read a third byte
    //   00a0  0d/lots;             0x80 + 0x20 = sets [4a7c] <- 0x20?
    //   00c3  12/01ea              0x80 + 0x40
    //   0180  14/00a5              this implements some sort of range operation
    //   1080  0f/02d6              is looking for digits
    //   3840  01/0051              this is the main gameplay loop (also looking for digits)
    //   44a2  0f/0115              we don't care about the output, and any keypress advances
    //   8080  CS/1717              inside runAutomap()

    @Override
    public Address exec(Interpreter i) {
        // I know, this is weird, but it's how the game manages the spell icons.
        i.drawSpellIcons();
        if (! i.isPaused()) {
            i.drawPartyInfoArea();
            i.composeVideoLayers(true, true, false);
        }

        final Address ip = i.getIP();
        i.drawString313e(); //?
        final int imm_2a44 = i.memory().read(ip.incr(1), 1);
        final int imm_2a45 = i.memory().read(ip.incr(2), 1);
        int pointer = ip.offset() + 3; // stored at [3a41/seg,3a3f/adr]
        final int flag_4a7c = imm_2a45 & 0x20;

        // 0x2864
        final int imm_2a46;
        if ((imm_2a45 & 0x10) != 0) {
            imm_2a46 = i.memory().read(ip.incr(3), 1);
            pointer++;
        } else {
            imm_2a46 = 0x0; // maybe??
        }

        // 0x287a
        // if ((imm_2a44 & 0x80) > 0) pushVideoData();

        // 0x2884
        if ((imm_2a45 & 0x80) > 0) { // print footer
            i.printFooter(imm_2a45 & 0x3);
            i.composeVideoLayers(false, false, true);
        }

        // This is a GUESS. I don't know for sure that it's 0x40; it might be 0x04, which is also unique to 0f/0115
        if ((imm_2a44 & 0x40) != 0) {
            final Address nextIP = new Address(ip.segment(), pointer);
            i.setPrompt(List.of(new KeyAction((ev) -> true, nextIP)));
            return null;
        }

        final List<KeyAction> prompts = new ArrayList<>();
        while (true) {
            final int ch = i.memory().read(ip.segment(), pointer, 1);
            if (ch == 0xff) break;
            if (ch == 0x80) { pointer += 3; continue; }
            if ((ch & 0xc0) == 0x40) {
                // this is a range; read two characters
                final int min = ch;
                final int max = i.memory().read(ip.segment(), pointer + 1, 1);
                final int target = i.memory().read(ip.segment(), pointer + 2, 2);
                pointer += 4;
                final KeyDetector det = (ev) -> {
                    final int scancode = ev.getCode().getCode();
                    return (min <= scancode && scancode <= max);
                };
                prompts.add(new KeyAction(det, new Address(ip.segment(), target)));
            } else {
                final int target = i.memory().read(ip.segment(), pointer + 1, 2);
                pointer += 3;
                prompts.add(new KeyAction(detector(ch), new Address(ip.segment(), target)));
            }
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
            case 0xab -> (ev) -> ev.getCode() == KeyCode.PLUS;
            case 0xad -> (ev) -> ev.getCode() == KeyCode.MINUS;
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
            case PLUS -> 0xab;
            case MINUS -> 0xad;
            case SLASH -> (withShift) ? 0xbf : 0xaf;
            case DIGIT0, DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6, DIGIT7, DIGIT8, DIGIT9 -> 0x01;
            default -> 0x80 | key.getCode();
        };
    }
}
