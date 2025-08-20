package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MapData {
    private final StringDecoder stringDecoder;

    private int mapId = -1;
    private ModifiableChunk primaryData;
    private ModifiableChunk secondaryData;

    private int chunkPointer;

    private int xMax;
    private int yMax;
    private int flags;
    private int randomEncounters;

    private List<Integer> primaryPointers;
    // private List<Integer> secondaryPointers;
    // private List<Integer> itemPointers;

    private int titleStringPtr;
    private int defaultEventPtr;
    private int actionsPtr;
    private int itemListPtr;
    private int monsterDataPtr;
    private int encountersPtr;
    private int tagLinesPtr;

    private List<Integer> titleChars;

    private final List<Byte> wallTextures54a7 = new ArrayList<>();
    // 54b5: translates map square bits [0:3]
    private final List<Byte> wallMetadata54b6 = new ArrayList<>();
    // 54c5: texture array? maxlen 4, values are indexes into 5677
    private final List<Byte> roofTextures54c5 = new ArrayList<>();
    private final List<Byte> floorTextures54c9 = new ArrayList<>();
    private final List<Byte> otherTextures54cd = new ArrayList<>();
    // 5677: list of texture chunks (+0x6e); see 0x5786()
    // 57c4: list of texture? segments
    private final List<Byte> textureChunks5677 = new ArrayList<>();
    // 5734: list of pointers to square data rows
    private final List<Integer> rowPointers57e4 = new ArrayList<>();

    // private final List<Action> actions = new ArrayList<>();
    // private final List<Item> items = new ArrayList<>();
    // private final List<Encounter> encounters = new ArrayList<>();
    // private final List<Monster> monsters = new ArrayList<>();

    public MapData(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    public void parse(int mapId, ModifiableChunk primary, boolean isDirty, Chunk secondary) {
        if (mapId == this.mapId) return;

        this.mapId = mapId;
        this.primaryData = (isDirty) ? primary : decompressChunk(primary); // primary = mapId + 0x46
        this.secondaryData = decompressChunk(secondary); // secondary = mapId + 0x1e

        chunkPointer = 0;

        this.xMax = primaryData.getByte(chunkPointer);
        // for some reason this is one larger than it should be; see below
        this.yMax = primaryData.getByte(chunkPointer + 1);
        this.flags = primaryData.getUnsignedByte(chunkPointer + 2);
        this.randomEncounters = primaryData.getUnsignedByte(chunkPointer + 3);
        chunkPointer += 4;

        byteReader(textureChunks5677::add);

        byte f = 0;
        while ((f & 0x80) == 0) {
            f = primaryData.getByte(chunkPointer);
            wallTextures54a7.add((byte)(f & 0x7f));
            wallMetadata54b6.add(primaryData.getByte(chunkPointer+1));
            chunkPointer += 2;
        }

        byteReader((b) -> roofTextures54c5.add((byte)(b & 0x7f)));
        byteReader((b) -> floorTextures54c9.add((byte)(b & 0x7f)));
        byteReader((b) -> otherTextures54cd.add((byte)(b & 0x7f)));

        decodeTitleString();

        int ptr = chunkPointer;
        int xInc = xMax * 3;
        for (int y = 0; y <= yMax; y++) {
            rowPointers57e4.add(ptr);
            ptr += xInc;
        }
        // 55bb:parseMapData() builds this array backwards, from yMax down to 0, so it can include an "end" pointer(?)
        Collections.reverse(rowPointers57e4);

        primaryPointers = discoverPointers(primaryData, rowPointers57e4.get(0));
        defaultEventPtr = primaryPointers.get(0);
        actionsPtr = primaryPointers.get(1);

        monsterDataPtr = secondaryData.getWord(0);
        encountersPtr = secondaryData.getWord(2);
        tagLinesPtr = secondaryData.getWord(4);
        itemListPtr = secondaryData.getWord(6);

        // parseEncounters();
        // parseItems();
        // parseActions();
    }

    public List<Integer> getTitleChars() {
        return titleChars;
    }

    public int getMaxX() {
        return xMax;
    }

    public int getMaxY() {
        return yMax;
    }

    public int getFlags() {
        return flags;
    }

    public void setStepped(int x, int y) {
        final int offset = rowPointers57e4.get(y + 1) + (3 * x);
        final int rawData = primaryData.getUnsignedByte(offset+1);
        primaryData.write(offset+1, 1, rawData | 0x08);
    }

    public record Square(
            int rawData,
            Optional<Integer> northWallTextureChunk,
            Optional<Integer> northWallTextureMetadata,
            Optional<Integer> westWallTextureChunk,
            Optional<Integer> westWallTextureMetadata,
            int roofTexture,
            int floorTextureChunk,
            Optional<Integer> otherTextureChunk,
            boolean touched,
            int eventId
    ) {}

    public Square getSquare(int x, int y) {
        if (rowPointers57e4.isEmpty()) { throw new RuntimeException("parse() hasn't been called"); }

        // The list of row pointers has one-too-many, and the "extra" is at the START
        // So 52b8:fetchMapSquare() starts at 0x57e6 i.e. [0x5734+2] i.e. it skips the extra pointer
        // FIXME: wrapping?
        final int offset = rowPointers57e4.get(y + 1) + (3 * x);
        final int rawData = (primaryData.getUnsignedByte(offset) << 16) |
                (primaryData.getUnsignedByte(offset+1) << 8) |
                (primaryData.getUnsignedByte(offset+2));

        final int northWallTextureIndex = (rawData >> 20) & 0xf;
        final Optional<Integer> northWallTextureId;
        final Optional<Integer> northWallTextureMetadata;
        if (northWallTextureIndex == 0) {
            northWallTextureId = Optional.empty();
            northWallTextureMetadata = Optional.empty();
        } else {
            northWallTextureId = Optional.of(textureHelper(wallTextures54a7, northWallTextureIndex));
            northWallTextureMetadata = Optional.of(0xff & wallMetadata54b6.get(northWallTextureIndex));
        }

        final int westWallTextureIndex = (rawData >> 16) & 0xf;
        final Optional<Integer> westWallTextureId;
        final Optional<Integer> westWallTextureMetadata;
        if (westWallTextureIndex == 0) {
            westWallTextureId = Optional.empty();
            westWallTextureMetadata = Optional.empty();
        } else {
            westWallTextureId = Optional.of(textureHelper(wallTextures54a7, westWallTextureIndex));
            westWallTextureMetadata = Optional.of(0xff & wallMetadata54b6.get(westWallTextureIndex));
        }

        final int roofTextureIndex = (rawData >> 14) & 0x3;
        final int roofTextureId = 0xff & roofTextures54c5.get(roofTextureIndex);

        final int floorTextureIndex = (rawData >> 12) & 0x3;
        final int floorTextureId = textureHelper(floorTextures54c9, floorTextureIndex);

        final boolean touched = (rawData & 0x000800) > 0;

        final int otherTextureIndex = (rawData >> 8) & 0x7;
        final Optional<Integer> otherTextureId;
        if (otherTextureIndex == 0) {
            otherTextureId = Optional.empty();
        } else {
            otherTextureId = Optional.of(0xff & textureChunks5677.get(otherTextureIndex));
        }

        final int eventId = (rawData) & 0xff;

        return new Square(
                rawData,
                northWallTextureId,
                northWallTextureMetadata,
                westWallTextureId,
                westWallTextureMetadata,
                roofTextureId,
                floorTextureId,
                otherTextureId,
                touched,
                eventId
        );
    }

    private int textureHelper(List<Byte> sectionTextures, int index) {
        final int textureIndex = 0xff & sectionTextures.get(index);
        return 0x6e + (0xff & textureChunks5677.get(textureIndex));
    }

    public Square[][] getView(int x, int y, int facing) {
        final Square[][] view = new Square[4][3];
        switch (facing) {
            case 0: // North
                for (int d = 0; d < 4; d++) {
                    view[3 - d][0] = getSquare(x - 1, y + d);
                    view[3 - d][1] = getSquare(x, y + d);
                    view[3 - d][2] = getSquare(x + 1, y + d);
                }
                return view;
            case 1: // East
                for (int d = 0; d < 4; d++) {
                    view[3 - d][0] = getSquare(x + d, y + 1);
                    view[3 - d][1] = getSquare(x + d, y);
                    view[3 - d][2] = getSquare(x + d, y - 1);
                }
                return view;
            case 2: // South
                for (int d = 0; d < 4; d++) {
                    view[3 - d][0] = getSquare(x + 1, y - d);
                    view[3 - d][1] = getSquare(x, y - d);
                    view[3 - d][2] = getSquare(x - 1, y - d);
                }
                return view;
            case 3: // West
                for (int d = 0; d < 4; d++) {
                    view[3 - d][0] = getSquare(x - d, y - 1);
                    view[3 - d][1] = getSquare(x - d, y);
                    view[3 - d][2] = getSquare(x - d, y + 1);
                }
                return view;
            default:
                throw new IllegalArgumentException("Unexpected facing: " + facing);
        }
    }

    public int getRoofTexture(int index) {
        final int textureIndex = roofTextures54c5.get(index);
        return textureChunks5677.get(textureIndex);
    }

    private void byteReader(Consumer<Byte> consumer) {
        byte f = 0;
        while ((f & 0x80) == 0) {
            f = primaryData.getByte(chunkPointer);
            chunkPointer++;
            consumer.accept(f);
        }
    }

    private void decodeTitleString() {
        titleStringPtr = primaryData.getWord(chunkPointer);
        chunkPointer += 2;

        stringDecoder.decodeString(primaryData, titleStringPtr);
        titleChars = stringDecoder.getDecodedChars();
    }

    private ModifiableChunk decompressChunk(Chunk chunk) {
        final HuffmanDecoder mapDecoder = new HuffmanDecoder(chunk);
        final List<Byte> decodedMapData = mapDecoder.decode();
        return new ModifiableChunk(decodedMapData);
    }

    private List<Integer> discoverPointers(Chunk chunk, int basePtr) {
        final List<Integer> pointers = new ArrayList<>();
        int thisPtr = basePtr;
        int firstPtr = chunk.getWord(basePtr);
        while (thisPtr < firstPtr) {
            int nextPtr = chunk.getWord(thisPtr);
            // if (nextPtr > chunk.getSize()) { return pointers; }
            pointers.add(nextPtr);
            if ((nextPtr != 0) && (nextPtr < firstPtr)) { firstPtr = nextPtr; }
            thisPtr += 2;
        }
        return pointers;
    }
}
