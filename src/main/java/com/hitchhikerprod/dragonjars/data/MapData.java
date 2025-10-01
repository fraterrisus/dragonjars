package com.hitchhikerprod.dragonjars.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    private final List<Action> actions = new ArrayList<>();
    private final List<Encounter> encounters = new ArrayList<>();
    private final List<Monster> monsters = new ArrayList<>();

    public MapData(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    public int mapId() {
        return mapId;
    }

    // primary map data pointers
    private static final int PTR_X_MAX = 0;
    private static final int PTR_Y_MAX = 1;
    private static final int PTR_FLAGS = 2;
    private static final int PTR_RANDOM_ENCOUNTERS = 3;
    private static final int PTR_TEXTURE_CHUNKS = 4;
    private int PTR_WALL_DATA;
    private int PTR_ROOF_TEXTURES;
    private int PTR_FLOOR_TEXTURES;
    private int PTR_DECO_TEXTURES;
    private int PTR_TITLE_STRING;
    private int PTR_SQUARE_DATA;
    private int PTR_DEFAULT_EVENT;
    private int PTR_ACTIONS;
    private int PTR_EVENTS;

    // secondary map data pointers
    private int PTR_ITEMS;
    private int PTR_MONSTERS;
    private int PTR_ENCOUNTERS;
    private int PTR_TAGLINES;

    public void parse(int mapId, ModifiableChunk primary, Chunk secondary) {
        this.mapId = mapId;
        this.primaryData = primary;      // primary = mapId + 0x46
        this.secondaryData = secondary;  // secondary = mapId + 0x1e

        int xMax = xMax();
        int yMax = yMax();

        PTR_WALL_DATA = skipArray(PTR_TEXTURE_CHUNKS, 1);
        PTR_ROOF_TEXTURES = skipArray(PTR_WALL_DATA, 2);
        PTR_FLOOR_TEXTURES = skipArray(PTR_ROOF_TEXTURES, 1);
        PTR_DECO_TEXTURES = skipArray(PTR_FLOOR_TEXTURES, 1);

        int ptr = skipArray(PTR_DECO_TEXTURES, 1);
        PTR_TITLE_STRING = primaryData.getWord(ptr);
        PTR_SQUARE_DATA = ptr + 2;
        ptr = PTR_SQUARE_DATA + (yMax * xMax * 3);
        PTR_DEFAULT_EVENT = primaryData.getWord(ptr);
        PTR_ACTIONS = primaryData.getWord(ptr + 2);
        PTR_EVENTS = ptr + 4;

        PTR_MONSTERS = secondaryData.getWord(0);
        PTR_ENCOUNTERS = secondaryData.getWord(2);
        PTR_TAGLINES = secondaryData.getWord(4);
        PTR_ITEMS = secondaryData.getWord(6);

        parseActions();
//        parseEncounters();
    }

    public int xMax() {
        return primaryData.getUnsignedByte(PTR_X_MAX);
    }

    public int yMax() {
        return primaryData.getUnsignedByte(PTR_Y_MAX);
    }

    public int flags() {
        return primaryData.getUnsignedByte(PTR_FLAGS);
    }

    public int randomEncounters() {
        return primaryData.getUnsignedByte(PTR_RANDOM_ENCOUNTERS);
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

    public List<Integer> chunkIds() {
        if (this.mapId == -1) return List.of();
        final List<Integer> ids = new ArrayList<>();
        // loadMapChunks [cs/5544] does seem to unload the previous value of heap[5a]
        ids.add(0x46 + mapId); // primary map segment
        ids.add(0x1e + mapId); // secondary map segment
        ids.addAll(textureChunks().stream()
                .map(t -> 0x6e + t)
                .toList());
        return ids;
    }

    private int skipArray(int pointer, int width) {
        int b = 0;
        while ((b & 0x80) == 0) {
            b = primaryData.getUnsignedByte(pointer);
            pointer += width;
        }
        return pointer;
    }

    private int getArrayValue(int basePointer, int index) {
        return 0x7f & primaryData.getByte(basePointer + index);
    }

    private int getTextureChunk(int index) {
        return getTextureNumber(index) + 0x6e;
    }

    private int getTextureNumber(int index) {
        return getArrayValue(PTR_TEXTURE_CHUNKS, index);
    }

    private List<Byte> textureChunks() {
        final List<Byte> chunks = new ArrayList<>();
        int ptr = PTR_TEXTURE_CHUNKS;
        byte b = 0;
        while ((b & 0x80) == 0) {
            b = primaryData.getByte(ptr++);
            chunks.add((byte)(0x7f & b));
        }
        return List.copyOf(chunks);
    }

    private int getWallTextureIndex(int index) {
        final int ptr = PTR_WALL_DATA + (2 * index);
        return 0x7f & primaryData.getByte(ptr);
    }

    private int getWallMetadata(int index) {
        final int ptr = PTR_WALL_DATA + (2 * index) + 1;
        return primaryData.getUnsignedByte(ptr);
    }

    private int getFloorTextureIndex(int index) {
        return getArrayValue(PTR_FLOOR_TEXTURES, index);
    }

    private int getRoofTextureIndex(int index) {
        return getArrayValue(PTR_ROOF_TEXTURES, index);
    }

    private int getDecoTextureIndex(int index) {
        return getArrayValue(PTR_DECO_TEXTURES, index);
    }

    public List<Integer> getTitleChars() {
        stringDecoder.decodeString(primaryData, PTR_TITLE_STRING);
        return stringDecoder.getDecodedChars();
    }

    public String getTitleString() {
        stringDecoder.decodeString(primaryData, PTR_TITLE_STRING);
        return stringDecoder.getDecodedString();
    }

    private int getSquareOffset(int x, int y) {
        final int rowOffset = (yMax() - (y + 1)) * xMax() * 3;
        final int colOffset = x * 3;
        return PTR_SQUARE_DATA + rowOffset + colOffset;
    }

    /**
     * Updates the "have I stepped on this square" flag (raw data 0x000800) for the given square. If the provided
     * coordinate is outside the board's dimensions but the map is marked as "wrapping", a modulus operator is applied.
     * If the board is not marked "wrapping", the update is silently ignored.
     * @param loc The coordinates of the square to update.
     */
    public void setStepped(GridCoordinate loc) {
        final GridCoordinate temp;
        if (isWrapping()) {
            temp = loc.modulus(xMax(), yMax());
        } else {
            if (loc.isOutside(xMax(), yMax())) return;
            temp = loc;
        }
        final int offset = getSquareOffset(temp.x(), temp.y());
        final int rawData = primaryData.getUnsignedByte(offset+1);
        primaryData.write(offset+1, 1, rawData | 0x08);
    }

    /**
     * Updates the 'raw' three-byte data for the given square. If the provided coordinates are outside the map's
     * dimensions (less than zero or greater than the map size), the update is silently ignored.
     * @param loc The coordinates of the square to update.
     * @param newData The new raw data value.
     */
    public void setSquare(GridCoordinate loc, int newData) {
        setSquare(loc.x(), loc.y(), newData);
    }

    /**
     * Updates the 'raw' three-byte data for the given square. If the provided coordinates are outside the map's
     * dimensions (less than zero or greater than the map size), the update is silently ignored.
     * @param x The X coordinate of the square to update.
     * @param y The Y coordinate of the square to update.
     * @param newData The new raw data value.
     */
    public void setSquare(int x, int y, int newData) {
        if (x < 0 || y < 0 || x >= xMax() || y >= yMax()) return;
        final int offset = getSquareOffset(x, y);
        primaryData.write(offset, 3, newData);
    }

    /**
     * Sets the 'special ID' field of a map square to zero, i.e. no special event exists here.
     * @param position The coordinates of the square to update.
     */
    public void eraseSquareSpecial(GridCoordinate position) {
        eraseSquareSpecial(position.x(), position.y());
    }

    /**
     * Sets the 'special ID' field of a map square to zero, i.e. no special event exists here.
     * @param x The X coordinate of the square to update.
     * @param y The Y coordinate of the square to update.
     */
    public void eraseSquareSpecial(int x, int y) {
        final int offset = getSquareOffset(x, y);
        primaryData.write(offset+2, 1, 0x00);
    }

    public Item getItem(int index) {
        final int pointer = secondaryData.getWord(PTR_ITEMS + (2 * index));
        return new Item(secondaryData).decode(pointer);
    }

    public int getEventPointer(int eventId) {
        if (eventId == 0) return PTR_DEFAULT_EVENT;
        final int pointer = PTR_EVENTS + (2 * (eventId - 1));
        return primaryData.getWord(pointer);
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

    public Square getSquare(int xin, int yin) {
        return getSquare(new GridCoordinate(xin, yin));
    }

    public Square getSquare(GridCoordinate position) {
        if (mapId == -1) { throw new RuntimeException("parse() hasn't been called"); }

        final int xMax = xMax();
        final int yMax = yMax();
        final int x, y;
        if (isWrapping()) {
            final GridCoordinate temp = position.modulus(xMax, yMax);
            x = temp.x();
            y = temp.y();
        } else {
            int xin = position.x();
            int yin = position.y();
            // Map coordinates *should* be bytes, but if we tried to do math they might be negative ints.
            if (xin < 0 || xin > 0x80) xin = 0;
            else if (xin >= xMax) xin = xMax - 1;

            if (yin < 0 || yin > 0x80) yin = 0;
            else if (yin >= yMax) yin = yMax - 1;

            if (xin != position.x() || yin != position.y()) {
                return stripSquare(xin, yin);
            } else {
                x = xin;
                y = yin;
            }
        }

        // The list of row pointers has one-too-many, and the "extra" is at the START
        // So 52b8:fetchMapSquare() starts at 0x57e6 i.e. [0x5734+2] i.e. it skips the extra pointer
        final int offset = getSquareOffset(x, y);

        final int rawData = primaryData.getQuadWord(offset) & 0x00ffffff;

        final int northWallTextureIndex = (rawData >> 4) & 0xf;
        final Optional<Integer> northWallTextureId;
        final Optional<Integer> northWallTextureMetadata;
        if (northWallTextureIndex == 0) {
            northWallTextureId = Optional.empty();
            northWallTextureMetadata = Optional.empty();
        } else {
            final int textureIndex = getWallTextureIndex(northWallTextureIndex - 1);
            northWallTextureId = Optional.of((textureIndex > 0x6e) ? textureIndex : getTextureChunk(textureIndex));
            northWallTextureMetadata = Optional.of(getWallMetadata(northWallTextureIndex - 1));
        }

        final int westWallTextureIndex = (rawData) & 0xf;
        final Optional<Integer> westWallTextureId;
        final Optional<Integer> westWallTextureMetadata;
        if (westWallTextureIndex == 0) {
            westWallTextureId = Optional.empty();
            westWallTextureMetadata = Optional.empty();
        } else {
            final int textureIndex = getWallTextureIndex(westWallTextureIndex - 1);
            westWallTextureId = Optional.of((textureIndex > 0x6e) ? textureIndex : getTextureChunk(textureIndex));
            westWallTextureMetadata = Optional.of(getWallMetadata(westWallTextureIndex - 1));
        }

        // Note that we look up the texture number on the master texture list but DO NOT add 0x6e for chunk id
        final int roofTextureIndex0 = (rawData >> 14) & 0x3;
        final int roofTextureIndex1 = getRoofTextureIndex(roofTextureIndex0);
        final int roofTextureId = getTextureNumber(roofTextureIndex1);

        final int floorTextureIndex0 = (rawData >> 12) & 0x3;
        final int floorTextureIndex1 = getFloorTextureIndex(floorTextureIndex0);
        final int floorTextureId = getTextureChunk(floorTextureIndex1);

        final boolean touched = (rawData & 0x000800) > 0;

        final int decoTextureIndex = (rawData >> 8) & 0x7;
        final Optional<Integer> decoTextureId;
        if (decoTextureIndex == 0) {
            decoTextureId = Optional.empty();
        } else {
            final int textureIndex = getDecoTextureIndex(decoTextureIndex - 1);
            decoTextureId = Optional.of(getTextureChunk(textureIndex));
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
                decoTextureId,
                touched,
                eventId
        );
    }

    // See mfn.72()
    private void parseActions() {
        if (PTR_ACTIONS == 0) return;

        int pointer = PTR_ACTIONS;
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

    public List<Action> actions() {
        // It seems like the actions themselves never get modified, so it's okay to parse them statically.
        // We can't even do a simple getAction(int index), because actions are different sizes so you _have_ to
        // iterate over the raw list from the beginning each time.
        return actions;
    }

    private void parseEncounters() {
        if ((PTR_MONSTERS == 0) ||
                (PTR_MONSTERS == PTR_TAGLINES) ||
                (PTR_MONSTERS == PTR_ENCOUNTERS)) { return; }

        for (int offset : discoverPointers(secondaryData, PTR_MONSTERS + 1)) {
            monsters.add(new Monster(secondaryData, stringDecoder).decode(offset));
        }

        final List<String> taglines = new ArrayList<>();
        for (int offset : discoverPointers(secondaryData, PTR_TAGLINES)) {
            stringDecoder.decodeString(secondaryData, offset);
            taglines.add(stringDecoder.getDecodedString());
        }

        for (int offset : discoverPointers(secondaryData, PTR_ENCOUNTERS + 1)) {
            final Encounter enc = new Encounter(secondaryData).decode(offset);
            enc.setTagline(taglines.get(enc.getTaglineIndex()));
            this.encounters.add(enc);
        }
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

    public record Square(
            int rawData,
            Optional<Integer> northWallTextureChunk,
            Optional<Integer> northWallTextureMetadata,
            Optional<Integer> westWallTextureChunk,
            Optional<Integer> westWallTextureMetadata,
            int roofTexture,
            int floorTextureChunk,
            Optional<Integer> decoTextureChunk,
            boolean touched,
            int specialId
    ) {}
}
