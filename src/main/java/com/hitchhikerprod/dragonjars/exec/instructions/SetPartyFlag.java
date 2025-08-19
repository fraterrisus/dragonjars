package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;

public class SetPartyFlag implements Instruction {
    // Sets a flag out of character data, i.e. one bit in a character field
    // The character ID is in heap[06], but that has to be looked up at heap[0a + id] to convert a "marching order"
    //   slot number into the character's location in the on-disk party data.
    // The field number is the immediate value (usually 0x4c, Status) plus an offset which comes from AX.
    // AX is split into two parts:
    //   *****... field number offset (see above)
    //   .....*** bitmask shift distance (see below)
    // The bit we're looking for is 0x80 shifted right by the number of bits in the lower half of AX.
    //
    // Example:
    //   heap[06] = 0x03
    //   heap[0a] = 0x00 0x01 0x03 0x02 0x04 0x05 0x06  // we swapped characters 2 and 3 at some point
    //   base address = heap[0a + 03] << 8 = 0x200
    //   program = 5f 4c
    //   ax = 0x13
    //   ax[hi] = 0b00010, so the character field is 4c + 02 or 0x4e
    //   ax[lo] = 0b011, so the bitmask is 0x80 >> 3 or 0x10
    //   so we look up character #2, field 0x4e, and set bit 0x10 of that value
    //   (simple!)
    @Override
    public Address exec(Interpreter i) {
        final Address ip = i.getIP();
        final int charId = i.heap(0x06).read();
        final int charBaseAddress = i.heap(0x0a + charId).read() << 8;
        final int offset = (i.getAL() >> 3) + i.memory().read(ip.incr(1), 1);
        final int value = i.memory().read(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1);
        final int bitmask = 0x80 >> (i.getAL() & 0x07);

        final int result = value | bitmask;
        i.memory().write(Interpreter.PARTY_SEGMENT, charBaseAddress + offset, 1, result);

        return ip.incr(OPCODE + IMMEDIATE);
    }
}
