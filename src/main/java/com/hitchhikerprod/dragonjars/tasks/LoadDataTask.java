package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.HuffmanDecoder;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import javafx.concurrent.Task;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class LoadDataTask extends Task<List<Chunk>> {
    public static final String MAGIC_STRING = "Created on an Apple ][ GS. Apple ][ Forever!";

    private final String executablePath;
    private final String data1Path;
    private final String data2Path;

    public LoadDataTask(String executablePath, String data1Path, String data2Path) {
        this.executablePath = executablePath;
        this.data1Path = data1Path;
        this.data2Path = data2Path;
    }

    @Override
    protected List<Chunk> call() throws Exception {
        final List<Chunk> chunks = new ArrayList<>();
        try (
                final RandomAccessFile exec = new RandomAccessFile(executablePath, "r");
                final RandomAccessFile data1 = new RandomAccessFile(data1Path, "r");
                final RandomAccessFile data2 = new RandomAccessFile(data2Path, "r")
        ) {
            final ChunkTable table = new ChunkTable(data1, data2);
            final int count = table.getChunkCount();

            for (int chunkId = 0; chunkId < count; chunkId++) {
                if (isCancelled()) return null;
                updateMessage("Loading segment " + (chunkId + 1) + " of " + count + 1);
                ModifiableChunk newChunk = new ModifiableChunk(table.readChunk(chunkId));

                if (newChunk.getSize() != 0) {
                    if (chunkId >= 0x18) {
                        // compressed chunks
                        updateMessage("Decompressing segment " + (chunkId + 1) + " of " + count + 1);
                        final HuffmanDecoder huffman = new HuffmanDecoder(newChunk);
                        newChunk = new ModifiableChunk(huffman.decode());
                        if (chunkId <= 0x1d) {
                            // full-screen image chunks
                            updateMessage("Decoding segment " + (chunkId + 1) + " of " + count + 1);
                            applyRollingXor(newChunk);
                        }
                        if (chunkId >= 0x100) {
                            // sound files
                            updateMessage("Decoding segment " + (chunkId + 1) + " of " + count + 1);
                            if (chunkId % 2 != 0) applyRollingAddition(newChunk, 0x0000);
                            applyRollingAddition(newChunk, 0x0004);
                        }
                    }
                }

                for (Patch p : PATCHES) {
                    if (p.chunkId() == chunkId) {
                        final List<Byte> oldValue = newChunk.getBytes(p.address(), p.oldValue().size());
                        if (p.oldValue().equals(oldValue)) {
                            if (p.newValue().size() != p.oldValue().size()) {
                                System.err.println("Patch failed to apply: new value is wrong length");
                                System.err.println(p);
                                continue;
                            }
                            newChunk.setBytes(p.address(), p.newValue());
                        } else {
                            System.err.println("Patch failed to apply: old value doesn't match existing");
                            System.err.println(p);
                        }
                    }
                }

                chunks.add(newChunk);
                updateProgress(chunkId + 1, count + 1);
            }

            updateMessage("Loading code segment " + (count + 1) + " of " + (count + 1));
            final int codeSize = (int) (exec.length());
            if ((long) codeSize != exec.length()) {
                throw new RuntimeException("Executable is too big for one byte array");
            }
            final byte[] codeSegment = new byte[codeSize];
            exec.readFully(codeSegment);

            final byte[] checkString = MAGIC_STRING.getBytes();
            for (int i = 0; i < checkString.length; i++) {
                if (codeSegment[0x08 + i] != checkString[i]) {
                    throw new RuntimeException("DRAGON.COM does not appear to be valid");
                }
            }

            final Chunk codeChunk = new Chunk(codeSegment);
            chunks.add(codeChunk);
            updateProgress(count + 1, count + 1);

            updateMessage("Finished.");
        }
        return chunks;
    }

    private static void applyRollingAddition(ModifiableChunk chunk, int baseIndex) {
        int pointer = baseIndex;
        int running = 0;
        while (pointer < chunk.getSize()) {
            running = running + chunk.getUnsignedByte(pointer);
            chunk.write(pointer, 1, running);
            pointer++;
        }
    }

    private static void applyRollingXor(ModifiableChunk chunk) {
        int readAddress = 0x00;
        int writeAddress = 0xa0;
        while (writeAddress < chunk.getSize()) {
            final int b0 = chunk.getQuadWord(readAddress);
            final int b1 = chunk.getQuadWord(writeAddress);
//            System.out.format("%04x\n", writeAddress);
            chunk.write(writeAddress, 4, b0 ^ b1);
            readAddress += 4;
            writeAddress += 4;
        }
    }

    private record Patch(int chunkId, int address, List<Byte> oldValue, List<Byte> newValue) {
    }

    // The "Attack" action is disabled if you're in the back ranks and have a Thrown Weapon readied
    private static final Patch ENABLE_THROWN_WEAPONS_FROM_REAR =
            new Patch(0x012, 0x0156, List.of((byte) 0x0b), List.of((byte) 0x0c));

    // The code that looks up weapon skills during an attack skips over Thrown Weapons skill
    private static final Patch ENABLE_THROWN_WEAPON_SKILL_CHECK =
            new Patch(0x003, 0x0d29, List.of((byte) 0x0b), List.of((byte) 0x0c));

    // Purgatory: if you leave to the north, it drops you 2E of where you should be
    private static final Patch PURGATORY_DEPARTURE_N =
            new Patch(0x047, 0x1696, List.of((byte) 0x0f), List.of((byte) 0x0d));

    // Dwarf Ruins: move the Dwarven Hammer chest flag from [9e:80] to [9a:08]
    private static final Patch DWARF_HAMMER_CHEST_FLAG =
            new Patch(0x055, 0x02d7, List.of((byte) 0x28), List.of((byte) 0x0c));

    // Pilgrim dock: exits are _all_ messed up
    private static final Patch PILGRIM_DOCK_DEPARTURE_N =
            new Patch(0x060, 0x02bf, List.of((byte) 0x13), List.of((byte) 0x12));
    private static final Patch PILGRIM_DOCK_DEPARTURE_E =
            new Patch(0x060, 0x02c1, List.of((byte) 0x13), List.of((byte) 0x12));
    private static final Patch PILGRIM_DOCK_DEPARTURE_S =
            new Patch(0x060, 0x02c2, List.of((byte) 0x14), List.of((byte) 0x15));
    private static final Patch PILGRIM_DOCK_DEPARTURE_W =
            new Patch(0x060, 0x02c5, List.of((byte) 0x11), List.of((byte) 0x12));

    // Pilgrim dock: typos "lock in only accessable"
    private static final Patch PILGRIM_DOCK_TYPO_1 =
            new Patch(0x060, 0x0206 + 0x1c, List.of((byte)0x4e), List.of((byte)0x52));
    private static final Patch PILGRIM_DOCK_TYPO_2 =
            new Patch(0x060, 0x0206 + 0x24, List.of((byte)0x21), List.of((byte)0x25));

    // Nisir: Fix the misaligned spinner in the Wind Tunnel
    private static final Patch NISIR_SPINNER_19_23 =
            new Patch(0x061, 0x0357, List.of((byte) 0x05), List.of((byte) 0x04));
    private static final Patch NISIR_SPINNER_20_23 =
            new Patch(0x061, 0x035a, List.of((byte) 0x00), List.of((byte) 0x05));

    // Freeport: typo "is your's for the taking"
    private static final Patch FREEPORT_TYPO_1 = new Patch(0x057, 0x0700, List.of(
            (byte) 0xf4, (byte) 0xd2, (byte) 0x60, (byte) 0xfa, (byte) 0x56, (byte) 0x7c, (byte) 0x4a, (byte) 0x17,
            (byte) 0x9c, (byte) 0x3e, (byte) 0x3c, (byte) 0x4c, (byte) 0x62, (byte) 0xbd, (byte) 0xa1, (byte) 0x54,
            (byte) 0x83, (byte) 0x77, (byte) 0xd2, (byte) 0x3a, (byte) 0x90, (byte) 0x4e, (byte) 0xf8, (byte) 0x86,
            (byte) 0x69, (byte) 0x30, (byte) 0x66, (byte) 0x25, (byte) 0xa9, (byte) 0xc8, (byte) 0xc7, (byte) 0x7b,
            (byte) 0xeb, (byte) 0x25, (byte) 0xe1, (byte) 0xb2, (byte) 0x98, (byte) 0xc0, (byte) 0xcc, (byte) 0x4b,
            (byte) 0x30, (byte) 0x66, (byte) 0x93, (byte) 0x07, (byte) 0xd2, (byte) 0xb3, (byte) 0xe2, (byte) 0x5f,
            (byte) 0xf1, (byte) 0xde, (byte) 0x80
    ), List.of(
            (byte) 0xf4, (byte) 0xd2, (byte) 0x60, (byte) 0xfa, (byte) 0x56, (byte) 0x7c, (byte) 0x4a, (byte) 0x17,
            (byte) 0x9c, (byte) 0x3e, (byte) 0x3c, (byte) 0x4c, (byte) 0x62, (byte) 0xbd, (byte) 0xa1, (byte) 0x54,
            (byte) 0x83, (byte) 0x77, (byte) 0xd2, (byte) 0x32, (byte) 0x09, (byte) 0xdf, (byte) 0x10, (byte) 0xcd,
            (byte) 0x26, (byte) 0x0c, (byte) 0xc4, (byte) 0xb5, (byte) 0x39, (byte) 0x18, (byte) 0xef, (byte) 0x7d,
            (byte) 0x64, (byte) 0xbc, (byte) 0x36, (byte) 0x53, (byte) 0x18, (byte) 0x19, (byte) 0x89, (byte) 0x66,
            (byte) 0x0c, (byte) 0xd2, (byte) 0x60, (byte) 0xfa, (byte) 0x56, (byte) 0x7c, (byte) 0x4b, (byte) 0xfe,
            (byte) 0x3b, (byte) 0xd0, (byte) 0x00
    ));

    // the Snake Pit items list (SMD chunk 0x36) is missing the Jade Eyes at location 9, which you could show to a sad
    // dwarf in order to get a hint about the clan hall

    // BUGFIX
    private static final List<Patch> PATCHES = List.of(
            ENABLE_THROWN_WEAPONS_FROM_REAR,
            ENABLE_THROWN_WEAPON_SKILL_CHECK,
            PURGATORY_DEPARTURE_N,
            DWARF_HAMMER_CHEST_FLAG,
            PILGRIM_DOCK_DEPARTURE_N, PILGRIM_DOCK_DEPARTURE_E, PILGRIM_DOCK_DEPARTURE_S, PILGRIM_DOCK_DEPARTURE_W,
            NISIR_SPINNER_19_23, NISIR_SPINNER_20_23,
            FREEPORT_TYPO_1,
            PILGRIM_DOCK_TYPO_1, PILGRIM_DOCK_TYPO_2
    );
}
