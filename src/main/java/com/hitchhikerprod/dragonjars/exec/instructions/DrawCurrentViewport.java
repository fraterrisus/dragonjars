package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.Facing;
import com.hitchhikerprod.dragonjars.data.GridCoordinate;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.exec.VideoHelper;
import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;

import java.util.List;

public class DrawCurrentViewport implements Instruction {
    private final Interpreter i;
    private final PixelRectangle gameplayArea;

    public DrawCurrentViewport(Interpreter i) {
        this.i = i;
        this.gameplayArea = i.fg().getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel();
    }

    @Override
    public Address exec(Interpreter ignored) {
        /* This emulates the slight performance delay that's baked into the old x86 processor (and emulators). Without
         * it, spots where the game takes multiple steps (Tracking in Tars, fleeing combat, etc.) all get smushed
         * together into a single video update. */
        final SleepTask sleep = new SleepTask(100);
        sleep.setOnSucceeded(ev -> draw());
        Thread.ofPlatform().daemon(true).start(sleep);
        return null;
    }

    private void draw() {
        i.unpause();
        i.disableMonsterAnimation();

        final PartyLocation loc = i.getPartyLocation();
        System.out.format("drawCurrentViewport(%s)\n", loc);

        i.decodeMap(loc.mapId());

        Heap.get(Heap.BOARD_MAX_X).write(i.mapDecoder().getMaxX());
        Heap.get(Heap.BOARD_MAX_Y).write(i.mapDecoder().getMaxY());
        Heap.get(Heap.BOARD_FLAGS).write(i.mapDecoder().flags());
        Heap.get(Heap.RANDOM_ENCOUNTERS).write(i.mapDecoder().getRandomEncounters());
        Heap.get(Heap.WALL_METADATA).write(getWallMetadata(loc.pos(), loc.facing()));
        i.setTitleString(i.mapDecoder().getTitleChars());

        i.mapDecoder().setStepped(loc.pos().x(), loc.pos().y());

        if (i.mapDecoder().isLit() || (Heap.get(Heap.LIGHT_RANGE).read() != 0)) {
            drawRoofTexture(i.mapDecoder().getSquare(loc.pos()).roofTexture());
            drawFloorTexture();
            drawWallTextures(); // also handles decor
        }

        i.drawViewportCorners();

        i.start(i.getIP().incr());
    }

    public void drawFloorTexture() {
        final PartyLocation loc = i.getPartyLocation();
        for (int index = 8; index >= 0; index--) {
            final int squareId = FLOOR_SQUARE_ORDER.get(index);
            final GridCoordinate rotated = loc.translate(squareId);
            final MapData.Square s = i.mapDecoder().getSquare(rotated.x(), rotated.y());
            final int segmentId = i.getSegmentForChunk(s.floorTextureChunk(), Frob.IN_USE);
            final Chunk textureChunk = i.memory().getSegment(segmentId);
            final int x0 = FLOOR_X_OFFSET.get(index);
            final int y0 = FLOOR_Y_OFFSET.get(index);
            final int textureOffset = FLOOR_TEXTURE_OFFSET.get(index);
//            System.out.format("decodeTexture(0x%02x, %d, %d, %d, %d, %d)\n",
//                    s.floorTextureChunk(), 0, textureOffset, x0, y0, 0);
            i.fg().drawTexture(textureChunk, textureOffset, x0, y0, 0x0, gameplayArea);
        }
    }

    private void drawRoofTexture(int textureId) {
        if (textureId == 1) { // 0x54f8
            final int segmentId = i.getSegmentForChunk(ChunkTable.SKY_TEXTURE, Frob.IN_USE);
            final Chunk textureChunk = i.memory().getSegment(segmentId);
            i.fg().drawTexture(textureChunk, 0x4, 0x0, 0x0, 0x0, gameplayArea);
        } else { // 0x5515
            // Draw a generic checkerboard roof
            // I don't bother reading the silly array of bytes from the executable (0x55ec)
            i.fg().drawRectangle(new PixelRectangle(gameplayArea.x0(), 0x30, gameplayArea.x1(), gameplayArea.y1()), (byte)0);
            for (int y = 0x08; y < 0x39; y++) {
                int x = gameplayArea.x0();
                while (x < gameplayArea.x1()) {
                    i.fg().drawGrid(x++, y, (byte)(y % 2 == 0 ? 0 : 4));
                    i.fg().drawGrid(x++, y, (byte)(y % 2 == 0 ? 4 : 0));
                }
            }
        }
    }

    public void drawWallTextures() {
        final PartyLocation loc = i.getPartyLocation();
        for (WallTexture data : WALL_SQUARE_ORDER) {
            final GridCoordinate farSquare = loc.translate(data.squareId());
            final Facing newFacing = data.facingDelta().apply(loc.facing());
            final int wallChunk = getWallTextureChunk(farSquare, newFacing);

            // make sure we're passing negative numbers correctly
            final int x0 = ALU.signExtend(WALL_X_OFFSET.get(data.listIndex()), 2);
            final int y0 = WALL_Y_OFFSET.get(data.listIndex());
            final int textureOffset = WALL_TEXTURE_OFFSET.get(data.listIndex());
            final int invert = WALL_INVERT.get(data.listIndex());

            // Wall texture
            if ((wallChunk >= 0x6e) && (wallChunk < 0x7f)) {
                final int segmentId = i.getSegmentForChunk(wallChunk, Frob.IN_USE);
                final Chunk textureChunk = i.memory().getSegment(segmentId);
                i.fg().drawTexture(textureChunk, textureOffset, x0, y0, invert, gameplayArea);
            }

            // 'Other' decor texture; try to only run each square once
            if (data.facingDelta() == Facing.Delta.NONE) {
                final MapData.Square sq = i.mapDecoder().getSquare(farSquare);
                if (sq.otherTextureChunk().isPresent()) {
                    final Integer otherChunkId = sq.otherTextureChunk().get();
                    final int segmentId = i.getSegmentForChunk(otherChunkId, Frob.IN_USE);
                    final Chunk textureChunk = i.memory().getSegment(segmentId);
                    i.fg().drawTexture(textureChunk, textureOffset, x0, y0, 0, gameplayArea);
                }
            }
        }
    }

    private int getWallTextureChunk(GridCoordinate position, Facing facing) {
        return switch (facing) {
            case NORTH -> i.mapDecoder().getSquare(position)
                    .northWallTextureChunk().orElse(0);
            case EAST -> i.mapDecoder().getSquare(ALU.addByte(position.x(), 1).value(), position.y())
                    .westWallTextureChunk().orElse(0);
            case SOUTH -> i.mapDecoder().getSquare(position.x(), ALU.subByte(position.y(), 1).value())
                    .northWallTextureChunk().orElse(0);
            case WEST -> i.mapDecoder().getSquare(position)
                    .westWallTextureChunk().orElse(0);
        };
    }

    private int getWallMetadata(GridCoordinate position, Facing facing) {
        return switch (facing) {
            case NORTH -> i.mapDecoder().getSquare(position)
                    .northWallTextureMetadata().orElse(0);
            case EAST -> i.mapDecoder().getSquare(ALU.addByte(position.x(), 1).value(), position.y())
                    .westWallTextureMetadata().orElse(0);
            case SOUTH -> i.mapDecoder().getSquare(position.x(), ALU.subByte(position.y(), 1).value())
                    .northWallTextureMetadata().orElse(0);
            case WEST -> i.mapDecoder().getSquare(position)
                    .westWallTextureMetadata().orElse(0);
        };
    }

    private static final List<Integer> FLOOR_X_OFFSET = List.of(
            0x10, 0x00, 0x80, 0x20, 0x00, 0x70, 0x30, 0x00, 0x60
    );

    private static final List<Integer> FLOOR_Y_OFFSET = List.of(
            0x78, 0x78, 0x78, 0x68, 0x68, 0x68, 0x58, 0x58, 0x58
    );

    private static final List<Integer> FLOOR_TEXTURE_OFFSET = List.of(
            0x12, 0x10, 0x14, 0x0c, 0x0a, 0x0e, 0x06, 0x04, 0x08
    );

    private static final List<Integer> FLOOR_SQUARE_ORDER = List.of(
            0xa, 0x9, 0xb, 0x7, 0x6, 0x8, 0x4, 0x3, 0x5
    );

    // Array index by wall location in viewport:
    //        |       |
    // -13,16-|-10,15-+-14,17-
    //       11       12
    // -0b,0e-|-08,0d-+-0c,0f-
    //       09       0a
    // -03,06-|-00,05-|-04,07-
    //       01   x   02
    //      party is here

    private static final List<Integer> WALL_X_OFFSET = List.of( // 0x536f
            0x0020, 0x0000, 0x0080, 0xffc0, 0x0080, 0x0020, 0xffc0, 0x0080,
            0x0030, 0x0020, 0x0070, 0xfff0, 0x0070, 0x0030, 0xfff0, 0x0070,
            0x0040, 0x0030, 0x0060, 0x0020, 0x0060, 0x0040, 0x0020, 0x0060
    );

    private static final List<Integer> WALL_Y_OFFSET = List.of( // 0x539f
            0x10, 0x00, 0x00, 0x10, 0x10, 0x10, 0x10, 0x10,
            0x20, 0x10, 0x10, 0x20, 0x20, 0x20, 0x20, 0x20,
            0x30, 0x20, 0x20, 0x30, 0x30, 0x30, 0x30, 0x30
    );

    // Wall texture sub-images:
    //   0x00: minimap north
    //   0x02: minimap west
    //   0x04: large front
    //   0x06: medium front
    //   0x08: small front
    //   0x0c: large side
    //   0x0e: medium side
    //   0x10: small side
    private static final List<Integer> WALL_TEXTURE_OFFSET = List.of( // 0x53ff
            0x04, 0x0c, 0x0c, 0x04, 0x04, 0x04, 0x04, 0x04,
            0x06, 0x0e, 0x0e, 0x06, 0x06, 0x06, 0x06, 0x06,
            0x08, 0x10, 0x10, 0x08, 0x08, 0x08, 0x08, 0x08
    );

    private static final List<Integer> WALL_INVERT = List.of( // 0x542f
            0x01, 0x00, 0x80, 0x01, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x80, 0x01, 0x01, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x80, 0x01, 0x01, 0x00, 0x00, 0x00
    );

    /*
        private static final List<Integer> WALL_SQUARE_ORDER = List.of( // 0x53cf
                0x16, 0x0a, 0x0b, 0x15, 0x17, 0x8a, 0x89, 0x8b,
                0x13, 0x07, 0x08, 0x12, 0x14, 0x87, 0x86, 0x88,
                0x10, 0x04, 0x05, 0x0f, 0x11, 0x84, 0x83, 0x85
        );
    */
    record WallTexture(int listIndex, int squareId, Facing.Delta facingDelta) { }

    // This ordering is my own invention: far to near, sides first then middle.
    private static final List<WallTexture> WALL_SQUARE_ORDER = List.of(
            new WallTexture(0x13, 0x03, Facing.Delta.NONE),
            new WallTexture(0x14, 0x05, Facing.Delta.NONE),
            new WallTexture(0x10, 0x04, Facing.Delta.NONE),
            new WallTexture(0x11, 0x04, Facing.Delta.LEFT),
            new WallTexture(0x12, 0x04, Facing.Delta.RIGHT),
            new WallTexture(0x0b, 0x06, Facing.Delta.NONE),
            new WallTexture(0x0c, 0x08, Facing.Delta.NONE),
            new WallTexture(0x08, 0x07, Facing.Delta.NONE),
            new WallTexture(0x09, 0x07, Facing.Delta.LEFT),
            new WallTexture(0x0a, 0x07, Facing.Delta.RIGHT),
            new WallTexture(0x03, 0x09, Facing.Delta.NONE),
            new WallTexture(0x04, 0x0b, Facing.Delta.NONE),
            new WallTexture(0x00, 0x0a, Facing.Delta.NONE),
            new WallTexture(0x01, 0x0a, Facing.Delta.LEFT),
            new WallTexture(0x02, 0x0a, Facing.Delta.RIGHT)
    );
}
