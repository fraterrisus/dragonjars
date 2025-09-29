package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StringEncoder {
    private static final Map<Character, List<Byte>> ENCODER_MAP = new HashMap<>() {{
        put(' ', List.of((byte)0x01));
        put('a', List.of((byte)0x02));
        put('b', List.of((byte)0x03));
        put('c', List.of((byte)0x04));
        put('d', List.of((byte)0x05));
        put('e', List.of((byte)0x06));
        put('f', List.of((byte)0x07));
        put('g', List.of((byte)0x08));
        put('h', List.of((byte)0x09));
        put('i', List.of((byte)0x0a));
        put('j', List.of((byte)0x1f, (byte)0x00));
        put('k', List.of((byte)0x0b));
        put('l', List.of((byte)0x0c));
        put('m', List.of((byte)0x0d));
        put('n', List.of((byte)0x0e));
        put('o', List.of((byte)0x0f));
        put('p', List.of((byte)0x10));
        put('q', List.of((byte)0x1f, (byte)0x01));
        put('r', List.of((byte)0x11));
        put('s', List.of((byte)0x12));
        put('t', List.of((byte)0x13));
        put('u', List.of((byte)0x14));
        put('v', List.of((byte)0x15));
        put('w', List.of((byte)0x16));
        put('x', List.of((byte)0x1f, (byte)0x02));
        put('y', List.of((byte)0x17));
        put('z', List.of((byte)0x1f, (byte)0x03));
        put('A', List.of((byte)0x1e, (byte)0x02));
        put('B', List.of((byte)0x1e, (byte)0x03));
        put('C', List.of((byte)0x1e, (byte)0x04));
        put('D', List.of((byte)0x1e, (byte)0x05));
        put('E', List.of((byte)0x1e, (byte)0x06));
        put('F', List.of((byte)0x1e, (byte)0x07));
        put('G', List.of((byte)0x1e, (byte)0x08));
        put('H', List.of((byte)0x1e, (byte)0x09));
        put('I', List.of((byte)0x1e, (byte)0x0a));
        put('J', List.of((byte)0x1e, (byte)0x1f, (byte)0x00));
        put('K', List.of((byte)0x1e, (byte)0x0b));
        put('L', List.of((byte)0x1e, (byte)0x0c));
        put('M', List.of((byte)0x1e, (byte)0x0d));
        put('N', List.of((byte)0x1e, (byte)0x0e));
        put('O', List.of((byte)0x1e, (byte)0x0f));
        put('P', List.of((byte)0x1e, (byte)0x10));
        put('Q', List.of((byte)0x1e, (byte)0x1f, (byte)0x01));
        put('R', List.of((byte)0x1e, (byte)0x11));
        put('S', List.of((byte)0x1e, (byte)0x12));
        put('T', List.of((byte)0x1e, (byte)0x13));
        put('U', List.of((byte)0x1e, (byte)0x14));
        put('V', List.of((byte)0x1e, (byte)0x15));
        put('W', List.of((byte)0x1e, (byte)0x16));
        put('X', List.of((byte)0x1e, (byte)0x1f, (byte)0x02));
        put('Y', List.of((byte)0x1e, (byte)0x17));
        put('Z', List.of((byte)0x1e, (byte)0x1f, (byte)0x03));
        put('0', List.of((byte)0x1f, (byte)0x04));
        put('1', List.of((byte)0x1f, (byte)0x05));
        put('2', List.of((byte)0x1f, (byte)0x06));
        put('3', List.of((byte)0x1f, (byte)0x07));
        put('4', List.of((byte)0x1f, (byte)0x08));
        put('5', List.of((byte)0x1f, (byte)0x09));
        put('6', List.of((byte)0x1f, (byte)0x0a));
        put('7', List.of((byte)0x1f, (byte)0x0b));
        put('8', List.of((byte)0x1f, (byte)0x0c));
        put('9', List.of((byte)0x1f, (byte)0x0d));
        put('.', List.of((byte)0x18));
        put('"', List.of((byte)0x19));
        put('\'', List.of((byte)0x1a));
        put(',', List.of((byte)0x1b));
        put('!', List.of((byte)0x1c));
        put('\n', List.of((byte)0x1d));
        put('(', List.of((byte)0x1f, (byte)0x32));
        put(')', List.of((byte)0x1f, (byte)0x33));
        put('/', List.of((byte)0x1f, (byte)0x34));
        put('\\', List.of((byte)0x1f, (byte)0xdc));
        put('#', List.of((byte)0x1f, (byte)0x36));
        put('*', List.of((byte)0x1f, (byte)0x37));
        put('?', List.of((byte)0x1f, (byte)0x38));
        put('<', List.of((byte)0x1f, (byte)0x39));
        put('>', List.of((byte)0x1f, (byte)0x3a));
        put(':', List.of((byte)0x1f, (byte)0x3b));
        put(';', List.of((byte)0x1f, (byte)0x3c));
        put('-', List.of((byte)0x1f, (byte)0x3d));
        put('%', List.of((byte)0x1f, (byte)0x3e));
    }};

    public List<Byte> encodeString(String s) {
        final List<Byte> unpacked = new ArrayList<>();
        final char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char ch;
            if (chars[i] == '\\' && chars[i+1] == 'n') {
                ch = '\n';
                i++;
            } else {
                ch = chars[i];
            }
            final List<Byte> pieces = ENCODER_MAP.get(ch);
            if (Objects.isNull(pieces)) {
                throw new RuntimeException("Can't translate character " + ch);
            }
            for (Byte p : pieces) System.out.format(" %02x", p);
            System.out.print(" (" + (ch == '\n' ? "\\n" : ch) + ")");
            unpacked.addAll(pieces);
        }
        unpacked.add((byte)0);
        System.out.println(" 00");

        final List<Byte> packed = pack(unpacked);
        System.out.format("remaining mask: %02x\n", outMask);
        return packed;
    }

    private List<Byte> pack(List<Byte> bytes) {
        packed = new ArrayList<>();
        newByte = 0;
        outMask = 0x80;
        for (int idx = 0; idx < bytes.size(); idx++) {
            Byte b = bytes.get(idx);
            packHelper(b, 5);
            if (b == 0x1f) {
                packHelper(bytes.get(++idx), 6);
            }
        }
        if (outMask != 0x80) packed.add(newByte);
        return packed;
    }

    private List<Byte> packed;
    private byte newByte;
    private int outMask;

    private void packHelper(byte b, int length) {
        int inMask = 0x1 << (length - 1);
        while (inMask > 0) {
            if ((b & inMask) > 0) newByte |= outMask;
            inMask >>= 1;
            outMask >>= 1;
            if (outMask == 0) {
                packed.add(newByte);
                newByte = 0;
                outMask = 0x80;
            }
        }
    }
}
