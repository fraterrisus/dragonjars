package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PartyLocation;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.exec.VideoHelper;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Frob;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.VideoBuffer;
import javafx.scene.input.KeyCode;

public class DrawAutomap implements Instruction {
    private VideoHelper automap;
    private Interpreter i;
    private PixelRectangle automapRectangle;

    @Override
    public Address exec(Interpreter i) {
        this.i = i;
        i.setBackground(0x10);

        automap = new VideoHelper(i.memory().getCodeChunk());
        automap.setVideoBuffer(new VideoBuffer());

        // drawModal uses the BBox and then shrinks it by one to draw the interior rectangle
        // so we can capture the BBox afterwards to write that same space
        i.drawModal(VideoBuffer.AUTOMAP.toChar());
        final PixelRectangle r = i.getBBox().toPixel();
        // ... except we don't want to overwrite the footer
        automapRectangle = new PixelRectangle(r.x0(), r.y0(), r.x1(), r.y1() - 8);

        i.setBackground();
        i.printFooter(0);

        final PartyLocation loc = i.getPartyLocation();
        return displayAutomapPage(loc.pos().x(), loc.pos().y());
    }

    private Address displayAutomapPage(final int x0, final int y0) { // 0x16f0
        final Address ip = i.getIP();
        final Address nextIP = ip.incr(OPCODE);
        final PartyLocation loc = i.getPartyLocation();

        // Note that we iterate (boxx,boxy) over one extra square (10x8 instead of 9x7) so that we can use the square
        // 1E of the viewport to draw the easternmost walls (i.e. the west wall of square x+1), and similarly for the
        // southernmost walls (north wall of square y-1).

        // This is a BUGFIX: the original won't draw the easternmost walls (although it *does* draw north and west
        // walls for squares 1S of the map viewport)

        automap.clearBuffer((byte)0);

        // (x0,y0) = center of automap
        // Screen ('box') coordinates origin = top-left (x0-4, y0+3)
        // Map coordinates origin = bottom-left
        for (int boxy = 0; boxy <= 7; boxy++) { // top to bottom, plus one
            int mapy = y0 + 3 - boxy;
            for (int boxx = 0; boxx <= 9; boxx++) { // left to right, plus one
                int mapx = x0 - 4 + boxx;

                final int xOffset = 0x20 * boxx;
                final int yOffset = 0x18 * boxy;

                final MapData.Square square = i.mapDecoder().getSquare(mapx, mapy);

                // Floor (texture offset 0)
                if (square.touched()) {
                    drawChunk(square.floorTextureChunk(), 0, xOffset, yOffset);
                }

                // West wall (texture offset 2)
                final MapData.Square squareLeft = i.mapDecoder().getSquare(mapx - 1, mapy);
                if (square.touched() || squareLeft.touched()) {
                    square.westWallTextureChunk().ifPresent(id -> drawChunk(id, 2, xOffset-8, yOffset-8));
                }

                // North wall (texture offset 0)
                final MapData.Square squareUp = i.mapDecoder().getSquare(mapx, mapy + 1);
                if (square.touched() || squareUp.touched()) {
                    square.northWallTextureChunk().ifPresent(id -> drawChunk(id, 0, xOffset-8, yOffset-8));
                }

                // Party avatar, or Deco (texture offset 0)
                if (mapx == loc.pos().x() && mapy == loc.pos().y()) {
                    automap.drawTextureData(i.memory().getCodeChunk(), VideoHelper.LITTLE_MAN_TEXTURE_ADDRESS,
                            xOffset, yOffset, 0, automapRectangle);
                } else if (square.touched()) {
                    square.otherTextureChunk().ifPresent(id -> drawChunk(id, 0, xOffset, yOffset));
                }
            }
        }

        i.bitBlast(automap, automapRectangle);

        // The switch is at cs:1717, but it points to code segment addresses so we emulate it here
        i.app().setKeyHandler(event -> {
            switch(event.getCode()) {
                case KeyCode.ESCAPE -> {
                    i.disableMonsterAnimation();
                    i.resetUI();
                    i.start(nextIP);
                }
                case KeyCode.UP, KeyCode.I, KeyCode.A -> {
                    final int y1 = y0 + 1;
                    if (y1 < i.heap(Heap.BOARD_MAX_Y).read()) displayAutomapPage(x0, y1);
                }
                case KeyCode.DOWN, KeyCode.K, KeyCode.Z -> {
                    final int y1 = y0 - 1;
                    if (y1 >= 0) displayAutomapPage(x0, y1);
                }
                case KeyCode.RIGHT, KeyCode.L -> {
                    final int x1 = x0 + 1;
                    if (x1 < i.heap(Heap.BOARD_MAX_X).read()) displayAutomapPage(x1, y0);
                }
                case KeyCode.LEFT, KeyCode.J -> {
                    final int x1 = x0 - 1;
                    if (x1 >= 0) displayAutomapPage(x1, y0);
                }
            }
        });

        return null;
    }

    private void drawChunk(int chunkId, int chunkIndex, int x, int y) {
        final int segmentId = i.getSegmentForChunk(chunkId, Frob.IN_USE);
        final Chunk chunk = i.memory().getSegment(segmentId);
        automap.drawTexture(chunk, chunkIndex, x, y, 0, automapRectangle);
    }
}
