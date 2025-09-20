package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MapData {
    private static final int FLAG_UNK_5 = 0x20;
    private static final int FLAG_CREATE_WALL = 0x10;
    private static final int FLAG_LIGHT = 0x08;
    private static final int FLAG_COMPASS = 0x04;
    private static final int FLAG_WRAPPING = 0x02;
    private static final int FLAG_UNK_1 = 0x01;

    private final StringDecoder stringDecoder;

    private int mapId = -1;
    private ModifiableChunk primaryData;
    private Chunk secondaryData;

    private int chunkPointer;

    private int xMax;
    private int yMax;
    // private int flags; we're reading this live now
    // private int randomEncounters;

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

    private final List<Action> actions = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Encounter> encounters = new ArrayList<>();
    private final List<Monster> monsters = new ArrayList<>();

    public MapData(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    public List<Integer> chunkIds() {
        if (this.mapId == -1) return List.of();
        final List<Integer> ids = new ArrayList<>();
        // loadMapChunks [cs/5544] does seem to unload the previous value of heap[5a]
        ids.add(0x46 + mapId); // primary map segment
        ids.add(0x1e + mapId); // secondary map segment
        ids.addAll(textureChunks5677.stream()
                .map(t -> 0x6e + (0x7f & t))
                .toList());
        return ids;
    }

    public void parse(int mapId, ModifiableChunk primary, Chunk secondary) {
        this.mapId = mapId;
        this.primaryData = primary;      // primary = mapId + 0x46
        this.secondaryData = secondary;  // secondary = mapId + 0x1e

        chunkPointer = 0;

        this.xMax = primaryData.getByte(chunkPointer);
        // for some reason this is one larger than it should be; see below
        this.yMax = primaryData.getByte(chunkPointer + 1);
        // this.randomEncounters = primaryData.getUnsignedByte(chunkPointer + 3);
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

        parseEncounters();
        parseItems();
        parseActions();
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

    public int flags() {
        return primaryData.getUnsignedByte(2);
    }

    public boolean allowsCreateWall() {
        return (flags() & FLAG_CREATE_WALL) != 0;
    }

    public boolean isLit() {
        return (flags() & FLAG_LIGHT) != 0;
    }

    public boolean hasCompass() {
        return (flags() & FLAG_COMPASS) != 0;
    }

    public boolean isWrapping() {
        return (flags() & FLAG_WRAPPING) != 0;
    }

    public void setStepped(int x, int y) {
        if (x < 0 || y < 0 || x >= xMax || y >= yMax) return;
        final int offset = rowPointers57e4.get(y + 1) + (3 * x);
        final int rawData = primaryData.getUnsignedByte(offset+1);
        primaryData.write(offset+1, 1, rawData | 0x08);
    }

    public void setSquare(int x, int y, int newData) {
        // FIXME check?
        final int offset = rowPointers57e4.get(y + 1) + (3 * x);
        primaryData.write(offset, 3, newData);
    }

    public Item getItem(int index) {
        return items.get(index);
    }

    public int getEventPointer(int eventId) {
        return primaryPointers.get(eventId);
    }

    public int getRandomEncounters() {
        return primaryData.getUnsignedByte(0x03);
    }

    private Square stripSquare(int x, int y) {
        // see 0x52eb
        final Square sq = getSquare(x, y);
        return new Square(
                sq.rawData & 0x00f000, // remove everything but roof and floor
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                sq.roofTexture,
                sq.floorTextureChunk,
                Optional.empty(),
                false,
                0
        );
    }

    public void eraseSquareSpecial(GridCoordinate position) {
        eraseSquareSpecial(position.x(), position.y());
    }

    public void eraseSquareSpecial(int x, int y) {
        final int offset = rowPointers57e4.get(y + 1) + (3 * x);
        primaryData.write(offset+2, 1, 0x00);
    }

    public Square getSquare(GridCoordinate position) {
        return getSquare(position.x(), position.y());
    }

    public Square getSquare(int xin, int yin) {
        if (rowPointers57e4.isEmpty()) { throw new RuntimeException("parse() hasn't been called"); }

        int x, y;
        // Wrapping logic. I bet this breaks in the Dwarf Clan Hall.
        if (isWrapping()) {
            x = xin % xMax;
            y = yin % yMax;
            if (x < 0) x += xMax;
            if (y < 0) y += yMax;
//            System.out.format("getSquare(%d,%d) -> (%d,%d)\n", xin, yin, x, y);
        } else {
            // Map coordinates *should* be bytes, but if we tried to do math they might be negative ints.
            if (xin < 0 || xin > 0x80) x = 0;
            else if (xin >= xMax) x = xMax - 1;
            else x = xin;

            if (yin < 0 || yin > 0x80) y = 0;
            else if (yin >= yMax) y = yMax - 1;
            else y = yin;

            if (x != xin || y != yin) {
//                System.out.format("getSquare(%d,%d) -> stripSquare(%d,%d)\n", xin, yin, x, y);
                return stripSquare(x, y);
            }
        }

        if ((x < 0) || (y < 0)) throw new RuntimeException("Error: illegal coordinates (" + x + "," + y + ")");

        // The list of row pointers has one-too-many, and the "extra" is at the START
        // So 52b8:fetchMapSquare() starts at 0x57e6 i.e. [0x5734+2] i.e. it skips the extra pointer
        final int offset = rowPointers57e4.get(y + 1) + (3 * x);

        final int rawData = primaryData.getQuadWord(offset) & 0x00ffffff;

        final int northWallTextureIndex = (rawData >> 4) & 0xf;
        final Optional<Integer> northWallTextureId;
        final Optional<Integer> northWallTextureMetadata;
        if (northWallTextureIndex == 0) {
            northWallTextureId = Optional.empty();
            northWallTextureMetadata = Optional.empty();
        } else {
            final int textureIndex = 0xff & wallTextures54a7.get(northWallTextureIndex - 1);
            if (textureIndex > 0x6e) {
                northWallTextureId = Optional.of(textureIndex);
            } else {
                northWallTextureId = Optional.of(0x6e + (0x7f & textureChunks5677.get(textureIndex)));
            }
            northWallTextureMetadata = Optional.of(0xff & wallMetadata54b6.get(northWallTextureIndex - 1));
        }

        final int westWallTextureIndex = (rawData) & 0xf;
        final Optional<Integer> westWallTextureId;
        final Optional<Integer> westWallTextureMetadata;
        if (westWallTextureIndex == 0) {
            westWallTextureId = Optional.empty();
            westWallTextureMetadata = Optional.empty();
        } else {
            final int textureIndex = 0xff & wallTextures54a7.get(westWallTextureIndex - 1);
            if (textureIndex > 0x6e) {
                westWallTextureId = Optional.of(textureIndex);
            } else {
                westWallTextureId = Optional.of(0x6e + (0x7f & textureChunks5677.get(textureIndex)));
            }
            westWallTextureMetadata = Optional.of(0xff & wallMetadata54b6.get(westWallTextureIndex - 1));
        }

        // Note that we look up the texture index on the master texture list but DO NOT add 0x6e for chunk id
        final int roofTextureIndex0 = (rawData >> 14) & 0x3;
        final int roofTextureIndex1 = 0xff & roofTextures54c5.get(roofTextureIndex0);
        final int roofTextureId = 0x7f & textureChunks5677.get(roofTextureIndex1);

        final int floorTextureIndex0 = (rawData >> 12) & 0x3;
        final int floorTextureIndex1 = 0xff & floorTextures54c9.get(floorTextureIndex0);
        final int floorTextureId = 0x6e + (0x7f & textureChunks5677.get(floorTextureIndex1));

        final boolean touched = (rawData & 0x000800) > 0;

        final int otherTextureIndex = (rawData >> 8) & 0x7;
        final Optional<Integer> otherTextureId;
        if (otherTextureIndex == 0) {
            otherTextureId = Optional.empty();
        } else {
            final int overallTextureIndex = 0xff & otherTextures54cd.get(otherTextureIndex - 1);
            otherTextureId = Optional.of(0x6e + (0x7f & textureChunks5677.get(overallTextureIndex)));
        }

        final int eventId = (rawData >> 16) & 0xff;

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

    public Optional<Action> findAction(int header, int specialId) {
        return actions.stream()
                .filter(a -> a.special() == 0 || a.special() == specialId)
                .filter(a -> a.header() == header)
                .findFirst();
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

    // See mfn.72()
    private void parseActions() {
        if (actionsPtr == 0) return;

        int pointer = actionsPtr;
        while (true) {
            final int header = primaryData.getUnsignedByte(pointer);
            if (header == 0xff) {
                // 0xff indicates the end of the list of actions
                return;
            } else if (header == 0x80) {
                // 0x80 indicates an action triggered by using an item
                // The item byte refers to the map data item list -- the item in your inventory
                // must match that item perfectly to trigger the action.
                actions.add(new ItemAction(
                        primaryData.getUnsignedByte(pointer + 1),
                        primaryData.getUnsignedByte(pointer + 2),
                        primaryData.getUnsignedByte(pointer + 3)));
                pointer += 4;
            } else if (header <= 0x3c) {
                // 0x00-0x3c indicates an action triggered by casting a spell
                actions.add(new SpellAction(header,
                        primaryData.getUnsignedByte(pointer + 1),
                        primaryData.getUnsignedByte(pointer + 2)));
                pointer += 3;
            } else if (header >= 0x8c && header <= 0xba) {
                // 0x8c-0xba indicates an action triggered by using a skill
                actions.add(new SkillAction(header,
                        primaryData.getUnsignedByte(pointer + 1),
                        primaryData.getUnsignedByte(pointer + 2)));
                pointer += 3;
            } else if (header == 0xfd || header == 0xfe) {
                // Miscellaneous "catch-all" actions; they trigger whenever you use something that
                // wasn't caught by a previous Action.
                // 0xfd triggers if the special ID matches the current square
                // 0xfe always triggers
                actions.add(new MatchAction(header,
                        primaryData.getUnsignedByte(pointer + 1),
                        primaryData.getUnsignedByte(pointer + 2)));
                pointer += 3;
            } else {
                actions.add(new Action(header,
                        primaryData.getUnsignedByte(pointer + 1),
                        primaryData.getUnsignedByte(pointer + 2)));
                pointer += 3;
            }
        }
    }

    private void parseItems() {
        if (itemListPtr == 0) return;

        for (int offset : discoverPointers(secondaryData, itemListPtr)) {
            this.items.add(new Item(secondaryData).decode(offset));
        }
    }

    private void parseEncounters() {
        if ((monsterDataPtr == 0) ||
                (monsterDataPtr == tagLinesPtr) ||
                (monsterDataPtr == encountersPtr)) { return; }

        for (int offset : discoverPointers(secondaryData, monsterDataPtr + 1)) {
            monsters.add(new Monster(secondaryData, stringDecoder).decode(offset));
        }

        final List<String> taglines = new ArrayList<>();
        for (int offset : discoverPointers(secondaryData, tagLinesPtr)) {
            stringDecoder.decodeString(secondaryData, offset);
            taglines.add(stringDecoder.getDecodedString());
        }

        for (int offset : discoverPointers(secondaryData, encountersPtr + 1)) {
            final Encounter enc = new Encounter(secondaryData).decode(offset);
            enc.setTagline(taglines.get(enc.getTaglineIndex()));
            this.encounters.add(enc);
        }
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
}
