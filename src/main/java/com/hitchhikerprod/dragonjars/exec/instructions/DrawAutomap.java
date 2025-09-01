package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ImageDecoder;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.scene.input.KeyCode;

import java.util.Optional;

public class DrawAutomap implements Instruction {
    public static final int AUTOMAP_FRAME_WIDTH = 0x90;

    // window = +/- 4 X squares and 3 Y squares
    @Override
    public Address exec(Interpreter i) {
        i.setBackground(0x10);
        i.drawModal(i.loadFromCodeSegment(0x1779, 0, 4)
                .stream().map(Interpreter::byteToInt).toList());
        i.setBackground();
        i.printFooter(0);

        final PartyLocation loc = i.getPartyLocation();
        return displayAutomapPage(i, loc.pos().x(), loc.pos().y());
    }

    // TODO 0x16f0
    public static Address displayAutomapPage(Interpreter i, int x0, int y0) {
        final ImageDecoder decoder = i.imageDecoder();
        final Address ip = i.getIP();
        final Address nextIP = ip.incr(OPCODE);

        for (int y = y0 + 3; y >= y0 - 3; y--) {
            for (int x = x0 - 4; x <= x0 + 4; x++) {
                final MapData.Square square = i.mapDecoder().getSquare(x, y);
                if (square.touched()) {
                    // floor texture offset 0
                    final int floorSegmentId = i.getSegmentForChunk(square.floorTextureChunk(), Frob.CLEAN);
                    final Chunk floorChunk = i.memory().getSegment(floorSegmentId);

                    // FIXME: videoMemory is set up to _just_ hold the gameplay area.
                    //   in order for the automap to work it's gotta be full-screen.
                    // MULT_FACTOR is really the "width of the area we're trying to fill",
                    //   which is 0x50 for the gameplay and 0x90 for the automap. It doesn't
                    //   change the size of the tile, it just determines how to manage
                    //   skipping lines.
                    decoder.decodeTexture(floorChunk, 0, 8, 8, 0, AUTOMAP_FRAME_WIDTH);
                    i.bitBlastViewport(); // should be bitBlast() with bbox coordinates

                    // deco texture offset 0
                    final Optional<Integer> decoChunkId = square.otherTextureChunk();
                }
                final MapData.Square squareLeft = i.mapDecoder().getSquare(x - 1, y);
                if (square.touched() || squareLeft.touched()) {
                    // wall texture offset 2: west wall
                    final Optional<Integer> wallChunkId = square.westWallTextureChunk();
                }
                final MapData.Square squareUp = i.mapDecoder().getSquare(x, y + 1);
                if (square.touched() || squareUp.touched()) {
                    // wall texture offset 0: north wall
                    final Optional<Integer> wallChunkId = square.northWallTextureChunk();
                }
            }
            final MapData.Square square = i.mapDecoder().getSquare(x0 + 5, y);
            final MapData.Square squareLeft = i.mapDecoder().getSquare(x0 + 4, y);
            if (square.touched() || squareLeft.touched()) {
                final Optional<Integer> wallChunkId = square.westWallTextureChunk();
            }
        }
        for (int x = x0 - 4; x <= x0 + 4; x++) {
            final MapData.Square square = i.mapDecoder().getSquare(x, y0 - 4);
            final MapData.Square squareUp = i.mapDecoder().getSquare(x, y0 - 4);
            if (square.touched() || squareUp.touched()) {
                final Optional<Integer> wallChunkId = square.northWallTextureChunk();
            }
        }

        // The switch is at cs:1717, but it looks weird
        i.app().setKeyHandler(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                i.drawHud();
                i.start(nextIP);
            }
        });

        return null;
    }
}
