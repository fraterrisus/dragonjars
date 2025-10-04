package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.CharRectangle;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.GridCoordinate;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.instructions.*;
import com.hitchhikerprod.dragonjars.tasks.EyeAnimationTask;
import com.hitchhikerprod.dragonjars.tasks.MonsterAnimationTask;
import com.hitchhikerprod.dragonjars.tasks.SpellDecayTask;
import com.hitchhikerprod.dragonjars.tasks.TorchAnimationTask;
import com.hitchhikerprod.dragonjars.ui.AppPreferences;
import com.hitchhikerprod.dragonjars.ui.RootWindow;
import javafx.beans.property.BooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Interpreter {
    private static final int MASK_LOW = 0x000000ff;
    private static final int MASK_HIGH = 0x0000ff00;
    private static final int MASK_WORD = 0x0000ffff;

    public static final int PARTY_SEGMENT = 1;

    private final DragonWarsApp app;

    /* Utility classes */

    private final StringDecoder stringDecoder;
    private final VideoHelper videoHelper;
    private MapData mapDecoder;

    /* Memory space */

    private final Memory memory;

    private final Deque<Byte> stack = new ArrayDeque<>(); // one-byte values

    // Write the entire HUD to this (empty title, no spell icons, no corners). It shouldn't ever change!
    private final VideoBuffer videoBackground = new VideoBuffer();
    // Write everything else
    private final VideoBuffer videoForeground = new VideoBuffer();

    private MonsterAnimationTask monsterAnimationTask;
    private EyeAnimationTask eyeAnimationTask;
    private TorchAnimationTask torchAnimationTask;
    private SpellDecayTask spellDecayTask;

//    private int mul_result; // 0x1166:4
//    private int div_result; // 0x116a:4

    private int draw_borders; // 0x253e
    private int bbox_x0; // 0x2547 (ch)
    private int bbox_y0; // 0x2549 (px)
    private int bbox_x1; // 0x254a (ch)
    private int bbox_y1; // 0x254c (px)

    private final List<Integer> stringBuffer = new ArrayList<>(); // 0x313e
    private List<Integer> titleString = List.of(); // 0x273a (len) 0x273b:16 (string)

    public int x_31ed; // default for draw_char
    public int y_31ef; // default for draw_char

    private int mem_342f = 0x00;
    private int mem_3430 = 0x00;
    private int bg_color_3431 = 0xffff;

    // 0x4a80(), called from mfn8a(RunMonster) sets this to 3
    // Any call to setFrob02For4d33 (0x4bc2) sets this to 0
    //   erase_video_buffer (0x3608)
    //     start
    //     automap_key_exit
    //     fcn.4a80
    //     draw_current_viewport
    //   fcn.4a80
    //   draw_current_viewport (0x4f90)
    private final ReentrantLock monsterAnimationLock = new ReentrantLock();
    private boolean animationEnabled_4d4e = false;
    private int animationMonsterId_4d32 = 0xff;

    /* Architectural registers */

    // In assembly, metaregister CS contains the address of the segment to which the current code segment
    // has been loaded. Here I store the structure index instead.
    private int cs = -1;
    private int ds = -1;

    private int ip = -1;

    private boolean width;
    private int ax;
    private int bx;
    private boolean flagCarry; // 0x0001
    private boolean flagZero;  // 0x0040
    private boolean flagSign;  // 0x0080

    // debugging information
    private boolean gameIsPaused = false;

    private int instructionsExecuted = 0;
    private final Deque<Supplier<Address>> executionStack = new LinkedList<>();

    public Interpreter(DragonWarsApp app, List<Chunk> dataChunks) {
        this.app = app;
        this.memory = new Memory(
                dataChunks.getLast(),
                dataChunks.subList(0, dataChunks.size() - 1)
        );

        this.width = false;
        this.ax = 0;
        this.bx = 0;
        this.flagCarry = false;
        this.flagZero = false;
        this.flagSign = false;

        this.stringDecoder = new StringDecoder(this.memory().getCodeChunk());
        this.videoHelper = new VideoHelper(this.memory().getCodeChunk());
    }

    public Interpreter init() {
        final boolean testMode = Objects.isNull(app());

        if (!testMode) {
            videoBackground.reset((byte)0x00);
            videoForeground.reset(VideoBuffer.CHROMA_KEY);

            videoHelper.setVideoBuffer(videoBackground);
            for (int i = 0; i < 10; i++) videoHelper.drawRomImage(i); // most HUD sections
            for (int i = 0; i < 16; i++) videoHelper.drawRomImage(0x1b + i); // HUD title bar
//            videoBackground.writeTo("video-background.png", AppPreferences.getInstance().scaleProperty().get());

            videoHelper.setVideoBuffer(videoForeground);
            final PixelRectangle mask = videoHelper.getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel();
            videoHelper.drawRectangle(mask, (byte)0);
//            videoForeground.writeTo("video-foreground.png", AppPreferences.getInstance().scaleProperty().get());

            startSpellDecayTask();
        }

        // cs:0150  ax <- 0x0000
        // cs:0155  heap[0x00..0x7f] <- 0x00
        // cs:0157  di <- 0xb6a2 [chunks]
        // cs:015a  ax <- 0xffff
        // cs:015b  cx <- 0x0180
        // cs:015e  chunk_map[0x0000..0x017f] <- 0x00

        // create segments 0 and 1, which have the same segment address, to store party data [0x0e00 bytes]
        final ModifiableChunk partyChunk = new ModifiableChunk(new byte[0x0e00]);
        memory().addSegment(partyChunk, 0xffff, 0x0001, Frob.FROZEN);
        memory().addSegment(partyChunk, 0xffff, 0x0e00, Frob.FROZEN);

        // build "x50" multiplication table
        // which sets ax to 0x2a..
        // cs:0166  al <- 0xff
        // cs:0168  frob.4d32 <- 0xff
        Heap.get(Heap.BOARD_1_SEGIDX).write(0xffff, 2);
        Heap.get(Heap.BOARD_2_SEGIDX).write(0xffff, 2);
        // cs:0177  struct_idx.4d33 <- 0xff
        Heap.get(0x08).write(0xff);
        // cs:017d  inc ax  (ax <- 0x2b00)
        Heap.get(0xdc).write(0x00);
        // cd:017e  0x377c <- 0x00
        setBackground(0x00);
        // run_opening_titles, which we already did
        // eraseSmallVideoBuffer(); // we do this above when we reset the foreground layer
        //   set the frob for segment_idx[4d33] to 0x02
        //     that was init'd to 0xffff, but now is 0xb9c0, i'm just not sure HOW
        //     also this doesn't have any effect because there's no segment loaded?
        //   but it also includes:
        if (!testMode) drawViewportCorners();

        setBBox(VideoBuffer.DEFAULT_RECT);
        if (!testMode) {
            draw_borders = 0xff;
            resetUI();
        }

        this.instructionsExecuted = 0;
        // [width] <- 0x00
        // [3923] <- 0x00

        return this; // for call chaining
    }

    /**
     * Start the interpreter from the provided chunk ID (NOT segment) and address.
     */
    public void reenter(int chunk, int addr, Supplier<Address> after) {
        this.executionStack.push(after);
        final int startingSegment = getSegmentForChunk(chunk, Frob.IN_USE);
        final Address nextIP = new Address(startingSegment, addr);
        setDS(startingSegment);
        mainLoop(nextIP);
    }

    public void start(int chunk, int addr) {
        if (Objects.nonNull(app())) app().setKeyHandler(null);
        final int startingSegment = getSegmentForChunk(chunk, Frob.IN_USE);
        final Address nextIP = new Address(startingSegment, addr);
        mainLoop(nextIP);
    }

    /**
     * Start the interpreter from the provided Address, which contains a segment/address pair.
     */
    public void reenter(Address startPoint, Supplier<Address> after) {
        this.executionStack.push(after);
        setDS(startPoint.segment());
        mainLoop(startPoint);
    }

    public void start(Address startPoint) {
        if (Objects.nonNull(app())) app().setKeyHandler(null);
        mainLoop(startPoint);
    }

    public Address finish() {
        this.width = false;
        return this.executionStack.pop().get();
    }

    private static final int BREAKPOINT_CHUNK = 0x000;
    private static final int BREAKPOINT_ADR = 0x00000;

    private void mainLoop(Address startPoint) {
        Address nextIP = startPoint;
        while (Objects.nonNull(nextIP)) {
            this.cs = nextIP.segment();
            if (this.ds == -1) this.ds = this.cs;
            this.ip = nextIP.offset();
            final int opcode = memory().read(nextIP, 1);
            final int csChunk = memory().getSegmentChunk(cs);
            if (memory().getSegmentFrob(cs) != Frob.IN_USE) {
                System.err.println("instruction read from segment " + cs + " (chunk " + csChunk + ") with frob " +
                        memory().getSegmentFrob(cs));
            }
//            System.out.format("%02x%s%08x %02x\n", csChunk, isWide() ? ":" : " ", ip, opcode);
            if (csChunk == BREAKPOINT_CHUNK && ip == BREAKPOINT_ADR) {
                System.out.println("breakpoint");
            }
            runPatches(csChunk, ip);
            final Instruction ins = decodeOpcode(opcode);
            try {
                nextIP = ins.exec(this);
            } catch (Exception e) {
                System.err.format("Caught exception at [%03x:%06x]\n", csChunk, this.ip);
                throw(e);
            }
            this.instructionsExecuted++;
        }
    }

    public Optional<CombatData> combatData() {
        return Optional.ofNullable(combatData);
    }

    private record Patch(int chunkId, int ip, Consumer<Interpreter> fn) {}

    private static final List<Patch> PATCHES = List.of(
            new Patch(0x008, 0x02f1, (i) -> i.openParagraph(i.getAX())),
            new Patch(0x046 + 0x08, 0x03aa, (i) -> i.openParagraph(30)),
            new Patch(0x046 + 0x12, 0x10ff, (i) -> i.openParagraph(138)),
            new Patch(0x046 + 0x18, 0x06c5, (i) -> i.openParagraph(80)),
            new Patch(0x046 + 0x1e, 0x065b, (i) -> i.openParagraph(91)),
            new Patch(0x046 + 0x24, 0x041b, (i) -> i.openParagraph(42)),

            new Patch(0x003, 0x0905, Interpreter::bugfixCastActionAvDvMod),

            new Patch(0x003, 0x0000, (i) -> i.combatData = new CombatData(i)),
            new Patch(0x003, 0x006c, (i) -> i.combatData().ifPresent(c -> c.decodeInitiative())),
            new Patch(0x012, 0x0097, (i) -> i.combatData().ifPresent(c -> c.getCombatants())),
            new Patch(0x003, 0x00e1, (i) -> i.combatData = null),

            new Patch(0x003, 0x0b00, (i) -> i.combatData().ifPresent(c -> c.partyTurn())),
            new Patch(0x003, 0x0b98, (i) -> i.combatData().ifPresent(c -> c.partyEquip())),
            new Patch(0x003, 0x0c06, (i) -> i.combatData().ifPresent(c -> c.partyMove(i.getAL()))),
            new Patch(0x003, 0x0c2e, (i) -> i.combatData().ifPresent(c -> c.partySpell())),
            new Patch(0x003, 0x0cdc, (i) -> i.combatData().ifPresent(c -> c.partyAttackTarget())),
            new Patch(0x003, 0x0e66, (i) -> i.combatData().ifPresent(c -> c.partyAttackBlocked())),
            new Patch(0x003, 0x0d3d, (i) -> i.combatData().ifPresent(c -> Heap.get(0x7b).write(0xff))),
            new Patch(0x003, 0x0d41, (i) -> i.combatData().ifPresent(c -> c.partyAttackHits())),
            new Patch(0x003, 0x0d81, (i) -> i.combatData().ifPresent(c -> c.partyDamageBonus(i.getAL()))),
            new Patch(0x003, 0x0d94, (i) -> i.combatData().ifPresent(c -> c.partyDamageMighty(i.getAL()))),
            new Patch(0x003, 0x0dd5, (i) -> i.combatData().ifPresent(c -> c.partyDamage())),
            new Patch(0x003, 0x0fb8, (i) -> i.combatData().ifPresent(c -> c.monsterTurn())),
            new Patch(0x003, 0x0ffc, (i) -> i.combatData().ifPresent(c -> c.monsterConfidence(i.getAL()))),
            new Patch(0x003, 0x1023, (i) -> i.combatData().ifPresent(c -> c.monsterBravery(i.getBL()))),

            new Patch(0x003, 0x108f, (i) -> i.combatData().ifPresent(c -> c.monsterAction(i.getAL()))),
            new Patch(0x003, 0x10e2, (i) -> i.combatData().ifPresent(c -> c.monsterFlees(true))),
            new Patch(0x003, 0x10f2, (i) -> i.combatData().ifPresent(c -> c.monsterFlees(false))),
            new Patch(0x003, 0x13d6, (i) -> i.combatData().ifPresent(c -> c.monsterRearms())),
            new Patch(0x003, 0x1113, (i) -> i.combatData().ifPresent(c -> c.monsterBlocks())),
            new Patch(0x003, 0x1196, (i) -> i.combatData().ifPresent(c -> c.monsterCalls(true))),
            new Patch(0x003, 0x11a3, (i) -> i.combatData().ifPresent(c -> c.monsterCalls(false))),
            new Patch(0x003, 0x11f8, (i) -> i.combatData().ifPresent(c -> c.monsterAttackHits())),
            new Patch(0x003, 0x127f, (i) -> i.combatData().ifPresent(c -> c.monsterDamage())),
            new Patch(0x003, 0x1305, (i) -> i.combatData().ifPresent(c -> c.monsterAttackBlocked())),
            new Patch(0x003, 0x1326, (i) -> i.combatData().ifPresent(c -> c.monsterAttackTarget())),
            new Patch(0x003, 0x1379, (i) -> i.combatData().ifPresent(c -> Heap.get(0x7b).write(0xff))),
            new Patch(0x003, 0x142e, (i) -> i.combatData().ifPresent(c -> c.monsterAdvances(true))),
            new Patch(0x003, 0x1453, (i) -> i.combatData().ifPresent(c -> c.monsterAdvances(false))),
            new Patch(0x003, 0x14c3, (i) -> i.combatData().ifPresent(c -> c.monsterSpell())),
            new Patch(0x003, 0x1508, (i) -> i.combatData().ifPresent(c -> c.monsterBreath())),

            new Patch(0x006, 0x03e4, (i) -> i.combatData().ifPresent(c -> c.partyHeal())),
            new Patch(0x006, 0x0631, (i) -> i.combatData().ifPresent(c -> c.partySpellDamage())),
            new Patch(0x006, 0x0678, (i) -> i.combatData().ifPresent(c -> Heap.get(0x7b).write(0xff))),
            new Patch(0x006, 0x067d, (i) -> i.combatData().ifPresent(c -> c.partySpellTarget())),
            new Patch(0x006, 0x0795, (i) -> i.combatData().ifPresent(c -> c.monsterSpellDamage())),
            new Patch(0x006, 0x07ca, (i) -> i.combatData().ifPresent(c -> c.monsterSpellHits()))
    );

    private void runPatches(int chunkId, int ip) {
        for (Patch patch : PATCHES) {
            if (chunkId == patch.chunkId() && ip == patch.ip())
                patch.fn().accept(this);
        }
    }

    private void openParagraph(int id) {
        final AppPreferences prefs = AppPreferences.getInstance();
        if (prefs.autoOpenParagraphsProperty().get()) app.openParagraphsWindow(id);
    }

    private void bugfixCastActionAvDvMod() {
        // BUGFIX
        // When calculating temporary AV/DV/AC values in a combat round, the game inexplicably doesn't check for
        // spell-casting actions (0x80 | spellId) before referencing the action-to-AV array at [0966], so it pulls
        // an AV mod from way off in the middle of the code. Confirmed this in an emulator. Action 0x0f is a no-op
        // with no AV or DV mods, so it seems a safe replacement.
        final AppPreferences prefs = AppPreferences.getInstance();
        if (!prefs.casterAVBugfixProperty().get()) return;

        final int action = getAL();
        if ((action & 0x80) > 0) setAL(0x0f);
    }

    public int instructionsExecuted() {
        return this.instructionsExecuted;
    }

    public Memory memory() {
        return this.memory;
    }

    public VideoHelper fg() {
        return this.videoHelper;
    }

    public StringDecoder stringDecoder() {
        return this.stringDecoder;
    }

    public MapData mapDecoder() {
        return this.mapDecoder;
    }

    private CombatData combatData = null;

    private void decodePartyAttack() {
        final int combatSegmentId = getSegmentForChunk(0x03, Frob.IN_USE);
        final int targetMonsterId = Heap.get(0x84).read(1);

        final int weaponType = Heap.get(0x66).read(1);
        final boolean weaponIsMelee = (weaponType < 0x08);

        final int targetStatus = memory().read(combatSegmentId, 0x030e + targetMonsterId, 1);
        final boolean monsterIsBlocking = (targetStatus & 0x40) > 0;

        if (weaponIsMelee & monsterIsBlocking) return;
    }

    public void decodeMap(int mapId) {
        // See [cs/0384] load_dirty_map_state() and [cs/5544] load_map_chunks()
        // This code reads clean primary map data (chunk 0x46 + mapID) and dirty map data (chunk 0x10) into memory and
        // then copies the dirty data into the "clean" segment. So here we point the map decoder at the segment for
        // formerly-clean primary map data instead of 0x10.
        final Heap.Access boardId = Heap.get(Heap.DECODED_BOARD_ID);
        if (boardId.read() != mapId) {
            if (Objects.nonNull(mapDecoder)) {
                packAutomapData(boardId.read());
                mapDecoder.chunkIds().forEach(this::freeSegmentForChunk);
            }

            boardId.write(mapId);

            this.mapDecoder = new MapData(stringDecoder());

            final int primarySegment = getSegmentForChunk(mapId + 0x46, Frob.IN_USE);
            Heap.get(Heap.BOARD_1_SEGIDX).write(primarySegment, 1);
            final ModifiableChunk primaryData = memory().getSegment(primarySegment);

            final int secondarySegment = getSegmentForChunk(mapId + 0x1e, Frob.IN_USE);
            Heap.get(Heap.BOARD_2_SEGIDX).write(secondarySegment, 1);
            final ModifiableChunk secondaryData = memory().getSegment(secondarySegment);

            mapDecoder().parse(mapId, primaryData, secondaryData);
            unpackAutomapData(mapId);

            // erase board state flags on every load (see cs:5589)
            Heap.get(Heap.GAME_STATE_BOARD).write(0, 4);
        }
    }

    private void packAutomapData(int mapId) { // 0x4e00 and 0x4e5a
        final ModifiableChunk automap = memory().automapChunk();
        final int xMax = mapDecoder().xMax();
        final int yMax = mapDecoder().yMax();
        int flagMask = automap.getWord(2 * mapId);
        int address = 0x050 + (flagMask >> 3);
        int mask = 0x80 >> (flagMask & 0x7);
        int value = automap.getByte(address);
        for (int y = 0; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                value = value & ~mask;
                if ((mapDecoder().getSquare(x, y).rawData() & 0x000800) > 0) {
                    value = value | mask;
                }
                mask = mask >> 1;
                if (mask == 0) {
                    automap.write(address, 1, value);
                    mask = 0x80;
                    value = automap.getByte(++address);
                }
            }
        }
        automap.write(address, 1, value);
    }

    private void unpackAutomapData(int mapId) { // 0x4e00 and 0x4e6b
        final ModifiableChunk automap = memory().automapChunk();
        final int xMax = mapDecoder().xMax();
        final int yMax = mapDecoder().yMax();
        int flagMask = automap.getWord(2 * mapId);
        int address = 0x050 + (flagMask >> 3);
        int mask = 0x80 >> (flagMask & 0x7);
        int value = automap.getByte(address);
        for (int y = 0; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                if ((value & mask) > 0) {
                    mapDecoder().setStepped(new GridCoordinate(x, y));
                }
                mask = mask >> 1;
                if (mask == 0) {
                    mask = 0x80;
                    value = automap.getByte(++address);
                }
            }
        }
    }

    /**
     * Examines the segment table to translate a chunk ID into a segment ID. If the chunk doesn't exist in the segment
     * table, a new segment will be allocated for it; if it does exist, its frob will be overwritten with the new value.
     * @param chunkId The desired chunk ID
     * @param frob The desired new value of the segment's frob
     * @return The segment ID of the (new) segment
     */
    public int getSegmentForChunk(int chunkId, Frob frob) {
        int segmentId = memory().lookupChunkId(chunkId);
        if (segmentId == -1 || (chunkId & 0x8000) > 0) {
            segmentId = memory().getFreeSegmentId();
            final ModifiableChunk newChunk = memory().copyDataChunk(chunkId);
            memory().setSegment(segmentId, newChunk, chunkId, newChunk.getSize(), frob);
            // System.out.format("getSegmentForChunk(0x%03x), %s", chunkId, memory());
        }
        // should there be a "don't overwrite frob 0xff" guard here?
        memory().setSegmentFrob(segmentId, frob);
        return segmentId;
    }

    /**
     * Marks the segment related to a chunk as available to be unloaded.
     * This method is much safer than #freeSegment, because it looks up the chunk-to-segment relationship first.
     * @param chunkId
     */
    public void freeSegmentForChunk(int chunkId) {
        final int segmentId = memory().lookupChunkId(chunkId);
        if (segmentId != -1) freeSegment(segmentId);
    }

    /**
     * Marks a segment as available to be unloaded, but doesn't actually remove it. If a subsequent call to
     * getSegmentForChunk() occurs for the same chunk, there's a chance that the existing segment will be revived
     * (rather than unloading and reloading it).
     * Note that this method is somewhat dangerous, in that it requires the caller to pass a correct segment ID with
     * the knowledge that it refers to the correct segment (chunk).
     * @param segmentId
     */
    public void freeSegment(int segmentId) {
        if (segmentId == 0xff) return;
        if (memory().getSegmentFrob(segmentId) != Frob.FROZEN) {
            memory().setSegmentFrob(segmentId, Frob.FREE);
        }
        // System.out.format("freeSegment(%d), %s", segmentId, memory());
    }

    public DragonWarsApp app() {
        return app;
    }

    public boolean isPaused() {
        return gameIsPaused;
    }

    public void pause() {
//        if (!gameIsPaused) System.out.println("pause <- true");
        gameIsPaused = true;
    }

    public void unpause() {
//        if (gameIsPaused) System.out.println("pause <- false");
        gameIsPaused = false;
    }

    public CharRectangle getBBox() {
        return new CharRectangle(bbox_x0, bbox_y0, bbox_x1, bbox_y1);
    }

    public void setBBox(CharRectangle bbox) {
        bbox_x0 = bbox.x0();
        bbox_y0 = bbox.y0();
        bbox_x1 = bbox.x1();
        bbox_y1 = bbox.y1();
    }

    public void expandBBox() {
        bbox_y1 = bbox_y1 + 8;
        bbox_y0 = bbox_y0 - 8;
        bbox_x1 = bbox_x1 + 1;
        bbox_x0 = bbox_x0 - 1;
    }

    public void shrinkBBox() {
        bbox_y1 = bbox_y1 - 8;
        bbox_y0 = bbox_y0 + 8;
        bbox_x1 = bbox_x1 - 1;
        bbox_x0 = bbox_x0 + 1;
    }

/*    public int getMulResult() {
        return mul_result;
    }

    public void setMulResult(int mulResult) {
        this.mul_result = mulResult;
    }

    public int getDivResult() {
        return div_result;
    }

    public void setDivResult(int divResult) {
        this.div_result = divResult;
    }*/

    public boolean getCarryFlag() {
        return flagCarry;
    }

    public void setCarryFlag(boolean flag) {
        this.flagCarry = flag;
    }

    public boolean getZeroFlag() {
        return flagZero;
    }

    public void setZeroFlag(boolean flag) {
        this.flagZero = flag;
    }

    public boolean getSignFlag() {
        return flagSign;
    }

    public void setSignFlag(boolean flag) {
        this.flagSign = flag;
    }

    public Address getIP() {
        return new Address(this.cs, this.ip);
    }

    public int getAL() {
        return this.ax & MASK_LOW;
    }

    public int getAX(boolean forceWide) {
        return (forceWide || width) ? this.ax & MASK_WORD : this.ax & MASK_LOW;
    }

    public int getAX() {
        return getAX(false);
    }

    public void setAL(int val) {
        this.ax = (this.ax & MASK_HIGH) | (val & MASK_LOW);
    }

    public void setAH(int val) {
        this.ax = ((val & MASK_LOW) << 8) | (this.ax & MASK_LOW);
    }

    public void setAX(int val, boolean forceWide) {
        if (forceWide || width) {
            this.ax = val & MASK_WORD;
        } else {
            setAL(val);
        }
    }

    /** Writes the value to the AX register. In WIDE mode this method writes both AL and AH; in NARROW mode it only
     * writes AL, and AH is untouched. */
    public void setAX(int val) {
        setAX(val, false);
    }

    public int getBL() {
        return this.bx & MASK_LOW;
    }

    public int getBX(boolean forceWide) {
        return (forceWide || width) ? this.bx & MASK_WORD : this.bx & MASK_LOW;
    }

    public int getBX() {
        return getBX(false);
    }

    public void setBL(int val) {
        this.bx = (this.bx & MASK_HIGH) | (val & MASK_LOW);
    }

    public void setBH(int val) {
        this.bx = ((val & 0x000000ff) << 8) | (this.bx & MASK_LOW);
    }

    public void setBX(int val) {
        if (width) {
            this.bx = val & MASK_WORD;
        } else {
            setBL(val);
        }
    }

    public int getCS() {
        return this.cs;
    }

    public int getDS() {
        return this.ds;
    }

    public void setDS(int val) {
        this.ds = val;
    }

    public void pushByte(int val) {
        this.stack.push(intToByte(val));
    }

    public void pushWord(int val) {
        this.stack.push(intToByte(val >> 8));
        this.stack.push(intToByte(val));
    }

    public int popByte() {
        return byteToInt(this.stack.pop());
    }

    public int popWord() {
        final int lo = byteToInt(this.stack.pop());
        final int hi = byteToInt(this.stack.pop());
        return hi << 8 | lo;
    }

    public int width() {
        return isWide() ? 2 : 1;
    }

    public boolean isWide() {
        return width;
    }

    public void setWidth(boolean width) {
        this.width = width;
    }

    public void resetUI() {
        unpause();
        drawStringBuffer();
        if (draw_borders != 0x00) drawHud();
        draw_borders = 0x00;
        final CharRectangle messageArea = fg().getHudRegionArea(VideoHelper.HUD_MESSAGE_AREA);
        setBBox(messageArea);
        x_31ed = messageArea.x0();
        y_31ef = messageArea.y0();
    }

    public void addToStringBuffer(List<Integer> st) {
        stringBuffer.addAll(st);
    }

    public void drawStringBuffer() {
        if (stringBuffer.isEmpty()) return;

        int i0 = 0;
        // skip leading spaces
//        while (stringBuffer.get(i0) == 0xa0) i0++;

        drawString(stringBuffer.subList(i0, stringBuffer.size()));
        stringBuffer.clear();
    }

    public void fillRectangle() {
        getImageWriter(this::fillRectangle);
    }

    private void fillRectangle(PixelWriter w) {
        fg().drawRectangle(getBBox().toPixel(), (byte)(bg_color_3431 & 0xf), w);

        x_31ed = bbox_x0; // 0x32a8
        y_31ef = bbox_y0;
        stringBuffer.clear();
    }

    /**
     * Sets the background color of forthcoming text to WHITE, unless the input value has bit 0x10 set, in which
     * case the background color is set to BLACK.
     */
    public void setBackground(int al) {
        // confirm: input 0x10 -> al 0x02 -> bx 0x0000
        this.bg_color_3431 = ((al & 0x10) > 0) ? 0x0000 : 0xffff;
        this.mem_342f = this.mem_3430 & 0xff;
        this.mem_3430 = al & 0xff;
    }

    /**
     * Sets the background color to the "previous" color, as stored in 0x342f.
     */
    public void setBackground() {
        setBackground(mem_342f);
    }

    public void backSpace() {
        x_31ed -= 1;
        getImageWriter(w -> fg().drawCharacter(0xa0, x_31ed * 8, y_31ef, bg_color_3431 == 0, w));
    }

    public boolean roomToDrawChar() {
        return x_31ed < bbox_x1 - 1;
    }

    public void drawChar(int ch) {
        getImageWriter(w -> drawChar(ch, w));
    }

    private void drawChar(int ch, PixelWriter w) {
        fg().drawCharacter(ch, x_31ed * 8, y_31ef, bg_color_3431 == 0, w);
        x_31ed += 1;
    }

    public void drawString(List<Integer> chars) {
        getImageWriter(w -> drawString(chars, w));
    }

    private void drawString(List<Integer> chars, PixelWriter pixelWriter) {
        int x = x_31ed;
        int y = y_31ef;

        int p0 = 0;
        int p1;
        while (p0 < chars.size()) {
            p1 = p0;
            int ch = chars.get(p1);
            while (ch != 0xa0 && ch != 0x8d) {
                p1++;
                if (p1 == chars.size()) {
                    ch = -1;
                    break;
                }
                ch = chars.get(p1);
            }
            if ((x + p1 - p0) > bbox_x1) {
                x = bbox_x0;
                y += 8;
            }
            for (int i = p0; i < p1; i++) {
                fg().drawCharacter(chars.get(i), x * 8, y, bg_color_3431 == 0, pixelWriter);
                x++;
            }
            p0 = p1;
            if (ch == 0x8d) { x = bbox_x0; y += 8; p0++; }
            if (ch == 0xa0) {
                if (x < bbox_x1 - 1) {
                    fg().drawCharacter(0xa0, x * 8, y, bg_color_3431 == 0, pixelWriter);
                    x++;
                } else {
                    x = bbox_x0;
                    y += 8;
                }
                p0++;
            }
        }

        x_31ed = x;
        y_31ef = y;
    }

    public void drawViewportCorners() {
        for (int i = 0; i < 4; i++) fg().drawCorner(i);
        bitBlast(fg(), fg().getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel());
    }

    public void stopAllThreads() {
        pause();
        stopMonsterAnimation();
        spellDecayTask.cancel();
        torchAnimationTask.cancel();
        eyeAnimationTask.cancel();
        while (Objects.nonNull(monsterAnimationTask) ||
                Objects.nonNull(spellDecayTask) ||
                Objects.nonNull(torchAnimationTask) ||
                Objects.nonNull(eyeAnimationTask)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}
        }
        videoHelper.setVideoBuffer(null);
    }

    private void startSpellDecayTask() {
        spellDecayTask = new SpellDecayTask(this);
        spellDecayTask.setOnSucceeded(ev -> spellDecayTask = null);
        spellDecayTask.setOnFailed(ev -> spellDecayTask = null);
        spellDecayTask.setOnCancelled(ev -> spellDecayTask = null);
        Thread.ofPlatform().daemon().start(spellDecayTask);
    }

    public void startTorchAnimation() {
        torchAnimationTask = new TorchAnimationTask(this);
        torchAnimationTask.setOnSucceeded(ev -> torchAnimationTask = null);
        torchAnimationTask.setOnFailed(ev -> torchAnimationTask = null);
        torchAnimationTask.setOnCancelled(ev -> torchAnimationTask = null);
        Thread.ofPlatform().daemon().start(torchAnimationTask);
    }
    
    public void startEyeAnimation() {
        eyeAnimationTask = new EyeAnimationTask(this);
        eyeAnimationTask.setOnSucceeded(ev -> eyeAnimationTask = null);
        eyeAnimationTask.setOnFailed(ev -> eyeAnimationTask = null);
        eyeAnimationTask.setOnCancelled(ev -> eyeAnimationTask = null);
        Thread.ofPlatform().daemon().start(eyeAnimationTask);
    }

    public boolean isMonsterAnimationEnabled() {
        monsterAnimationLock.lock();
        final boolean enabled = animationEnabled_4d4e;
        monsterAnimationLock.unlock();
        return enabled;
    }

    public int activeMonster() {
        monsterAnimationLock.lock();
        final int monsterId = animationMonsterId_4d32;
        monsterAnimationLock.unlock();
        return monsterId;
    }

    public void enableMonsterAnimation(int monsterId) {
        monsterAnimationLock.lock();
        animationEnabled_4d4e = true;
        animationMonsterId_4d32 = monsterId;
        monsterAnimationLock.unlock();
    }

    public void disableMonsterAnimation() {
        monsterAnimationLock.lock();
        stopMonsterAnimation();
        animationEnabled_4d4e = false;
        animationMonsterId_4d32 = 0xff;
        monsterAnimationLock.unlock();
    }

    public void startMonsterAnimation(MonsterAnimationTask task) {
        stopMonsterAnimation();
        monsterAnimationTask = task;
        Thread.ofPlatform().daemon().start(monsterAnimationTask);
    }

    public void stopMonsterAnimation() {
        if (Objects.nonNull(monsterAnimationTask)) {
            monsterAnimationTask.cancel();
            monsterAnimationTask = null;
        }
    }

    private boolean regionOverlapsBBox(int regionId, boolean force) {
        if (!force && draw_borders == 0x00) return false;

        expandBBox();

        final CharRectangle region = fg().getHudRegionArea(regionId);

        if ((region.x0() >= bbox_x1) || (region.y0() >= bbox_y1) || (bbox_x0 >= region.x1()) || (bbox_y0 >= region.y1())) {
            shrinkBBox();
            return false;
        } else {
            shrinkBBox();
            return true;
        }
    }

    private int eyePhase = -1;
    private int torchPhase = -1;

    public void setTorchPhase(int phase) {
        this.torchPhase = phase;
        if (!isPaused()) drawTorchHelper();
    }

    public void setEyePhase(int phase) {
        this.eyePhase = phase;
        if (!isPaused()) drawEyeHelper();
    }

    private void drawEyeHelper() {
        final PixelRectangle mask = fg().getRomImageArea(VideoHelper.EYE_CLOSED);
        if (eyePhase < 0) {
            bitBlast(videoBackground, mask);
        } else {
            fg().drawRomImage(VideoHelper.EYE_CLOSED + eyePhase);
            bitBlast(videoForeground, mask);
        }
    }

    private void drawTorchHelper() {
        final PixelRectangle mask = fg().getRomImageArea(VideoHelper.TORCH_1);
        if (torchPhase < 0) {
            bitBlast(videoBackground, mask);
        } else {
            fg().drawRomImage(VideoHelper.TORCH_1 + torchPhase);
            bitBlast(videoForeground, mask);
        }
    }

    private void drawCompassHelper() {
        final PixelRectangle mask = fg().getRomImageArea(VideoHelper.COMPASS_N);
        final boolean naturalLight = Objects.nonNull(mapDecoder) && mapDecoder().isLit();
        final int compass = Heap.get(Heap.COMPASS_ENABLED).read();
        if (compass <= 0 && !naturalLight) {
            bitBlast(videoBackground, mask);
        } else {
            final int facing = Heap.get(Heap.PARTY_FACING).read();
            fg().drawRomImage(VideoHelper.COMPASS_N + facing);
            bitBlast(videoForeground, mask);
        }
    }

    private void drawShieldHelper() {
        final PixelRectangle mask = fg().getRomImageArea(VideoHelper.SHIELD);
        final int shield = Heap.get(Heap.SHIELD_POWER).read();
        if (shield <= 0) {
            bitBlast(videoBackground, mask);
        } else {
            fg().drawRomImage(VideoHelper.SHIELD);
            bitBlast(videoForeground, mask);
        }
    }

    public void drawSpellIcons(boolean force) {
        if (!force && isPaused()) return;

        final int trap = Heap.get(Heap.DETECT_TRAPS_RANGE).read();
        if (trap > 0) {
            if (Objects.isNull(eyeAnimationTask)) startEyeAnimation();
        } else {
            eyePhase = -1;
        }

        final int torch = Heap.get(Heap.LIGHT_RANGE).read();
        if (torch > 0) {
            if (Objects.isNull(torchAnimationTask)) startTorchAnimation();
        } else {
            torchPhase = -1;
        }

        drawCompassHelper();
        drawShieldHelper();
        drawEyeHelper();
        drawTorchHelper();

        // ax <- 0x0000
        // [4a71] <- ax
        // [4a73] <- ax
        // CF <- 1
    }

    public void drawHud() {
        draw_borders = 0x00;

        for (int regionId = 0; regionId < 14; regionId++) {
            if (regionOverlapsBBox(regionId, true)) {
                final CharRectangle regionRect = fg().getHudRegionArea(regionId);
                switch(regionId) {
                    case VideoHelper.HUD_BOTTOM -> {
                        // Weirdly, the ROM region contains the bottom row, but the HUD region *doesn't*.
                        // We could probably fix this another way, because that row never gets drawn over,
                        // but this is relatively simple.
                        final PixelRectangle myRect = fg().getRomImageArea(regionId);
                        bitBlast(videoBackground, myRect);
                    }
                    case VideoHelper.HUD_PILLAR -> {
                        bitBlast(videoBackground, regionRect.toPixel());
                        drawSpellIcons(true);
                    }
                    case VideoHelper.HUD_GAMEPLAY -> bitBlast(videoForeground, regionRect.toPixel());
                    case VideoHelper.HUD_PARTY_AREA -> {
                        Heap.get(Heap.PC_DIRTY).write(0x00, 7); // heap[18:1e] <- 0x00
                        drawPartyInfoArea();
                    }
                    case VideoHelper.HUD_TITLE_BAR -> drawMapTitle();
                    case VideoHelper.HUD_MESSAGE_AREA -> {
                        setBBox(regionRect);
                        fillRectangle();
                    }
                    // Just copy this from the background video buffer, it's already been drawn
                    // (and we want a faithful copy)
                    default -> bitBlast(videoBackground, regionRect.toPixel(), false);
                }
            }
        }
    }

    public void bitBlast(VideoHelper helper, PixelRectangle mask, boolean respectChroma) {
        getImageWriter(w -> helper.writeTo(w, mask, respectChroma));
    }

    public void bitBlast(VideoHelper helper, PixelRectangle mask) {
        bitBlast(helper, mask, true);
    }

    public void bitBlast(VideoBuffer buffer, PixelRectangle mask, boolean respectChroma) {
        getImageWriter(w -> buffer.writeTo(w, mask, respectChroma));
    }

    public void bitBlast(VideoBuffer buffer, PixelRectangle mask) {
        bitBlast(buffer, mask, true);
    }

    public void drawPartyInfoArea() { // 0x1a12
        final int save_31ed = x_31ed;
        final int save_31ef = y_31ef;
        final int save_heap_06 = Heap.get(Heap.SELECTED_PC).read();

        // set the indirect function to draw_char()

        for (int charId = 0; charId < 7; charId++) {
            // Assembly loops in the other direction, but our bar-drawing code needs to go this way
            // to avoid mishaps with the black pixels between bars.
            final int heapIndex = Heap.PC_DIRTY + charId;
            int bg = Heap.get(heapIndex).read();
            // In theory this is a "don't bother, nothing's changed" check,
            // but I'm probably missing some updates (see 0x2bb1)
            if ((bg & 0x80) > 0) continue;
            Heap.get(Heap.SELECTED_PC).write(charId);
            bg = ((bg & 0x02) > 0) ? 0x01 : 0x10;
            drawCharacterInfo(charId, bg);
            Heap.get(heapIndex).write(0xff);
        }

        // copy everything we just did to the screen
        final PixelRectangle mask = fg().getHudRegionArea(VideoHelper.HUD_PARTY_AREA).toPixel();
        bitBlast(videoForeground, mask);

        // set the indirect function back to 0x30c1

        Heap.get(Heap.SELECTED_PC).write(save_heap_06);
        x_31ed = save_31ed;
        y_31ef = save_31ef;
    }

    private void drawCharacterInfo(int charId, int bg) {
        // input flag is either 0x10 or 0x01
        setBackground(bg);

        final PixelRectangle partyRegion = fg().getHudRegionArea(VideoHelper.HUD_PARTY_AREA).toPixel();
        final int pcY = partyRegion.y0() + (charId << 4);
        final PixelRectangle nameRegion = new PixelRectangle(partyRegion.x0(), pcY, partyRegion.x1(), pcY + 0x8);
        final PixelRectangle statusRegion = new PixelRectangle(partyRegion.x0(), pcY + 0x8, partyRegion.x1(), pcY + 0x10);

        fg().drawRectangle(nameRegion, (byte)(bg_color_3431 == 0 ? 0x0 : 0xf));
        fg().drawRectangle(statusRegion, (byte)(bg_color_3431 == 0 ? 0x0 : 0xf));

        final int charsInParty = Heap.get(Heap.PARTY_SIZE).read();
        if (charId >= charsInParty) {
            // black out non-existing characters
            fg().drawRectangle(nameRegion, (byte)0);
            fg().drawRectangle(statusRegion, (byte)0);
            setBackground();
            return;
        }

        final int charBaseAddress = Heap.get(Heap.MARCHING_ORDER + charId).read() << 8;

        final Address namePointer = new Address(PARTY_SEGMENT, charBaseAddress);
        final List<Integer> nameCh = Instructions.getStringFromMemory(this, namePointer);

        int x = 0x1b + ((0x0d - nameCh.size()) >> 1); // character address
        for (int ch : nameCh) {
            fg().drawCharacter(ch, x * 8, nameRegion.y0(), bg_color_3431 == 0);
            x++;
        }

        final int statuses = memory().read(PARTY_SEGMENT, charBaseAddress + Memory.PC_STATUS, 1);
        for (int i = 3; i >= 0; i--) {
            final int mask = memory().getCodeChunk().getUnsignedByte(VideoHelper.PC_STATUS_BITMASKS + i);
            if ((statuses & mask) > 0) {
                drawStatusHelper(i, statusRegion);
                setBackground();
                return;
            }
        }

        drawBarHelper(statusRegion, 0x00, charBaseAddress + Memory.PC_HEALTH_CURRENT, 12);
        drawBarHelper(statusRegion, 0x03, charBaseAddress + Memory.PC_STUN_CURRENT, 10);
        drawBarHelper(statusRegion, 0x06, charBaseAddress + Memory.PC_POWER_CURRENT, 9);

        setBackground();
    }

    public void indentFromBBox(int dx) {
        getImageWriter(w -> indentTo(bbox_x0 + dx, w));
    }

    public void indentToBBox() {
        getImageWriter(w -> indentTo(bbox_x1, w));
    }

    private void indentTo(int limit, PixelWriter w) {
        // drawChar skips the line-wrap check
        while (x_31ed < limit) drawChar(0xa0, w);
    }

    private void drawStatusHelper(int i, PixelRectangle statusRegion) {
        final int wordAddress = memory().getCodeChunk().read(VideoHelper.PC_STATUS_STRINGS + (2 * i), 2) - 0x100;
        this.stringDecoder.decodeString(memory().getCodeChunk(), wordAddress);

        final List<Integer> chars = new ArrayList<>();
        chars.add(0xe9); // 'i'
        chars.add(0xf3); // 's'
        chars.add(0xa0); // ' '
        chars.addAll(stringDecoder.getDecodedChars());

        int x = memory().getCodeChunk().getUnsignedByte(VideoHelper.PC_STATUS_OFFSETS + i);
        for (int ch : chars) {
            fg().drawCharacter(ch, x * 8, statusRegion.y0(), bg_color_3431 == 0);
            x++;
        }
    }

    private void drawBarHelper(PixelRectangle region, int y, int attributeAddr, int colorIndex) {
        final int cur = memory().read(PARTY_SEGMENT, attributeAddr, 2);
        final int max = memory().read(PARTY_SEGMENT, attributeAddr + 2, 2);
        // avoid divide-by-zero for null stats (usually Power)
        if (cur == 0 || max == 0) return;
        final int barWidth = 0x60 * cur / max;
        for (int dx = 0; dx < 0x60; dx++) {
            final byte color = (byte)((dx <= barWidth) ? colorIndex : 0);
            fg().drawGrid(region.x0() + dx, region.y0() + y, color);
            fg().drawGrid(region.x0() + dx, region.y0() + y + 1, color);
        }
    }

    public void setTitleString(List<Integer> chars) {
        if (chars.size() > 16) {
            this.titleString = List.copyOf(chars.subList(0, 16));
        } else {
            this.titleString = List.copyOf(chars);
        }
        drawMapTitle();
    }

    private void drawMapTitle() { // 0x2cd4
        // x_31ed and y_31ef are preserved across this call, but I don't use them
        // in this implementation.
        final PixelRectangle mask = fg().getHudRegionArea(VideoHelper.HUD_TITLE_BAR).toPixel();

        setBackground(0x10);
        fg().drawRectangle(mask, VideoBuffer.CHROMA_KEY);
        int x = 0x04 + ((16 - this.titleString.size()) / 2);
        for (int ch : this.titleString) {
            fg().drawCharacter(ch, x * 8, 0, bg_color_3431 == 0);
            x += 1;
        }
        setBackground();

        bitBlast(videoBackground, mask);
        bitBlast(videoForeground, mask);
    }

    public void drawModal(CharRectangle r) {
        pause();
        drawStringBuffer();

        boolean invert = bg_color_3431 == 0;

        // four immediates: 16 00 28 98 (combat window)
        // written as words to 0x253f/tmp
        if (draw_borders != 0) { //           <- come back to this
            //   expand bounding box()
            //   do some bounds checking?...   ???
            //   shrink bounding box()
            drawHud();
        }
        // copy 0x253f/tmp to 0x2547/bbox
        setBBox(r);

        // copy 0x2547/bbox/x0,y0 to 0x31ed/x0,y0
        x_31ed = r.x0();
        y_31ef = r.y0();

        getImageWriter(w -> {
            int x;
            int y = r.y0();
            // draw top border
            for (x = r.x0(); x < r.x1(); x += 1) {
                fg().drawCharacter(0x01, x * 8, y, invert, w);
            }
            fg().drawCharacter(0x00, r.x0() * 8, y, invert, w);
            fg().drawCharacter(0x02, (r.x1() - 1) * 8, y, invert, w);
            // draw vertical edges
            y += 8;
            while (y < r.y1() - 8) {
                fg().drawCharacter(0x03, r.x0() * 8, y, invert, w);
                fg().drawCharacter(0x04, (r.x1() - 1) * 8, y, invert, w);
                y += 8;
            }
            // draw bottom border
            for (x = r.x0(); x < r.x1(); x += 1) {
                fg().drawCharacter(0x06, x * 8, y, invert, w);
            }
            fg().drawCharacter(0x05, r.x0() * 8, y, invert, w);
            fg().drawCharacter(0x07, (r.x1() - 1) * 8, y, invert, w);

            shrinkBBox();
            fillRectangle(w);
        });
        draw_borders = 0xff;
    }

    private void withHeapLock(Runnable fn) {
        Heap.lock();
        try { fn.run(); }
        finally { Heap.unlock(); }
    }

    private void autoBandage() {
        // This basically emulates the Bandage handler at [06/0171]
        withHeapLock(() -> {
            // This should only work in travel mode
            if (isPaused()) return;
            if (Heap.get(Heap.COMBAT_MODE).lockedRead() != 0) return;

            final int partySize = Heap.get(Heap.PARTY_SIZE).lockedRead();

            // Find the best bandager in the party
            int bandageAbility = 0;
            for (int charId = 0; charId < partySize; charId++) {
                final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + charId).lockedRead() << 8;
                final int pcStatus = memory().read(PARTY_SEGMENT, pcBaseAddress + Memory.PC_STATUS, 1);
                if ((pcStatus & Memory.PC_STATUS_DEAD) > 0) continue;

                final int bandageRanks = memory().read(PARTY_SEGMENT, pcBaseAddress + Memory.PC_SKILL_BANDAGE, 1);
                bandageAbility = Integer.max(bandageAbility, bandageRanks);
            }
            if (bandageAbility == 0) return;

            bandageAbility += 10;
            for (int charId = 0; charId < partySize; charId++) {
                final int pcBaseAddress = Heap.get(Heap.MARCHING_ORDER + charId).lockedRead() << 8;
                final int pcStatus = memory().read(PARTY_SEGMENT, pcBaseAddress + Memory.PC_STATUS, 1);
                if ((pcStatus & Memory.PC_STATUS_DEAD) > 0) continue;

                for (Integer offset : List.of(Memory.PC_HEALTH_CURRENT, Memory.PC_STUN_CURRENT)) {
                    final int cur = memory().read(PARTY_SEGMENT, pcBaseAddress + offset, 2);
                    final int max = memory().read(PARTY_SEGMENT, pcBaseAddress + offset + 2, 2);
                    if (cur < bandageAbility) {
                        final int newHealth = Integer.min(max, bandageAbility);
                        memory().write(PARTY_SEGMENT, pcBaseAddress + offset, 2, newHealth);
                        Heap.get(Heap.PC_DIRTY + charId).lockedWrite(0);
                    }
                }
            }
        });
        drawPartyInfoArea();
    }

    public void setPrompt(List<ReadKeySwitch.KeyAction> prompts) {
        final EventHandler<KeyEvent> keyHandler = event -> {
            if (event.getCode().isModifierKey()) return;
            if (event.getCode() == KeyCode.B && event.isControlDown()) {
                autoBandage();
                return;
            }
            if (event.getCode() == KeyCode.S && event.isControlDown()) {
                final BooleanProperty soundEnabled = AppPreferences.getInstance().soundEnabledProperty();
                soundEnabled.set(!soundEnabled.get());
                return;
            }
            for (ReadKeySwitch.KeyAction prompt : prompts) {
                if (prompt.function().match(event)) {
                    if (event.getCode().isDigitKey()) {
                        Heap.get(Heap.SELECTED_PC).write(event.getCode().getCode() - (int)'1');
                    }
                    setAX(ReadKeySwitch.scanCode(event.getCode(), event.isShiftDown(), event.isControlDown()));
                    start(prompt.destination());
                    break;
                }
            }
        };
        app().setKeyHandler(keyHandler);
    }

    private static final List<Integer> FOOTER_OFFSETS = List.of(0x0c, 0x0f, 0x09, 0x0e);

    private static final List<String> FOOTERS = List.of(
            "ESC to exit",     // len=0x0b  ARGH
            "ESC to continue", // len=0x0f
            "Press ESC",       // len=0x09
            "ESC to go back"   // len=0x0e
    );

    public void printFooter(int index) { // 0x288b
        if (index > 3) throw new IllegalArgumentException("index can't be greater than 3");
        y_31ef = bbox_y1 - 8;
        // x_3166 = 0; // not sure why we do this
        final int x0 = Integer.max(0, bbox_x1 - bbox_x0 - FOOTER_OFFSETS.get(index)) >> 1;
        x_31ed = bbox_x0;
        getImageWriter(w -> {
            indentTo(bbox_x0 + x0, w); // this might not be welcome always?
            // x_31ed = bbox_x0 + x0;
            final List<Integer> ch = FOOTERS.get(index).chars().map(c -> c | 0x80).boxed().toList();
            drawString(ch, w);
            indentTo(bbox_x1, w);
        });

        // 0x28c6
//        final int bufferOffset = (y_31ef - bbox_y0) >> 3;
//        buf_2a47[bufferOffset] = 0xff;
//        buf_2a60[bufferOffset] = 0x9b;
    }

    public static int byteToInt(byte b) {
        return MASK_LOW & ((int) b);
    }

    public static byte intToByte(int i) {
        return (byte)(i & MASK_LOW);
    }

    public void getImageWriter(Consumer<PixelWriter> fn) {
        final Image image = RootWindow.getInstance().getImage();
        if (image instanceof WritableImage wimage) {
            final PixelWriter writer = wimage.getPixelWriter();
            fn.accept(writer);
        }
    }

    private Instruction decodeOpcode(int opcode) {
        return switch (opcode) {
            case 0x00 -> Instructions.SET_WIDE;
            case 0x01 -> Instructions.SET_NARROW;
            case 0x02 -> Instructions.PUSH_DS;
            case 0x03 -> Instructions.POP_DS;
            case 0x04 -> Instructions.PUSH_CS;
            case 0x05 -> new LoadBLHeap();
            case 0x06 -> new LoadBLImm();
            case 0x07 -> new LoadBLZero();
            case 0x08 -> new StoreBLHeap();
            case 0x09 -> new LoadAXImm();
            case 0x0a -> new LoadAXHeap();
            case 0x0b -> new LoadAXHeapOffset();
            case 0x0c -> new LoadAX();
            case 0x0d -> new LoadAXOffset();
            case 0x0e -> new LoadAXIndirect();
            case 0x0f -> new LoadAXLongPtr();
            case 0x10 -> new LoadAXIndirectImm();
            case 0x11 -> new StoreZeroHeap();
            case 0x12 -> new StoreAXHeap();
            case 0x13 -> new StoreAXHeapOffset();
            case 0x14 -> new StoreAX();
            case 0x15 -> new StoreAXOffset();
            case 0x16 -> new StoreAXIndirect();
            case 0x17 -> new StoreAXLongPtr();
            case 0x18 -> new StoreAXIndirectImm();
            case 0x19 -> new MoveHeap();
            case 0x1a -> new StoreImmHeap();
            case 0x1b -> new MoveData();
            case 0x1c -> new StoreImm();
            case 0x1d -> new CopyAutomapBuffer();
            case 0x1e -> Instructions.HARD_EXIT; // "kill executable" aka "you lost"
            case 0x1f -> Instructions.NOOP; // "read chunk table", which we don't need to do
            //   0x20 sends the (real) IP to 0x0000, which is probably a segfault
            case 0x21 -> new MoveALBL();
            case 0x22 -> new MoveBXAX();
            case 0x23 -> new IncHeap();
            case 0x24 -> new IncAX();
            case 0x25 -> new IncBL();
            case 0x26 -> new DecHeap();
            case 0x27 -> new DecAX();
            case 0x28 -> new DecBL();
            case 0x29 -> new LeftShiftHeap();
            case 0x2a -> new LeftShiftAX();
            case 0x2b -> new LeftShiftBL();
            case 0x2c -> new RightShiftHeap();
            case 0x2d -> new RightShiftAX();
            case 0x2e -> new RightShiftBL();
            case 0x2f -> new AddAXHeap();
            case 0x30 -> new AddAXImm();
            case 0x31 -> new SubAXHeap();
            case 0x32 -> new SubAXImm();
            case 0x33 -> new MulAXHeap();
            case 0x34 -> new MulAXImm();
            case 0x35 -> new DivAXHeap();
            case 0x36 -> new DivAXImm();
            case 0x37 -> new AndAXHeap();
            case 0x38 -> new AndAXImm();
            case 0x39 -> new OrAXHeap();
            case 0x3a -> new OrAXImm();
            case 0x3b -> new XorAXHeap();
            case 0x3c -> new XorAXImm();
            case 0x3d -> new CmpAXHeap();
            case 0x3e -> new CmpAXImm();
            case 0x3f -> new CmpBLHeap();
            case 0x40 -> new CmpBLImm();
            // The SUB and CMP instructions flip the carry bit before writing, which makes
            // JC and JNC behave in the opposite manner. But ADD doesn't flip carry.
            case 0x41 -> JumpIf.NOT_CARRY;
            case 0x42 -> JumpIf.CARRY;
            case 0x43 -> JumpIf.ABOVE;
            case 0x44 -> JumpIf.EQUAL;
            case 0x45 -> JumpIf.NOT_EQUAL;
            case 0x46 -> JumpIf.SIGN;
            case 0x47 -> JumpIf.NOT_SIGN;
            case 0x48 -> new TestAndSetHeapSign();
            case 0x49 -> new LoopBX();
            case 0x4a -> new LoopBXLimit();
            case 0x4b -> Instructions.SET_CARRY;
            case 0x4c -> Instructions.CLEAR_CARRY;
            case 0x4d -> new RandomAX();
            case 0x4e -> new FlagSetAL();
            case 0x4f -> new FlagClearAL();
            case 0x50 -> new FlagTestAL();
            case 0x51 -> new ArrayMax();
            case 0x52 -> JumpIf.ALWAYS;
            case 0x53 -> new Call();
            case 0x54 -> new Return();
            case 0x55 -> new PopAX();
            case 0x56 -> new PushAX();
            case 0x57 -> new LongJump();
            case 0x58 -> new LongCall();
            case 0x59 -> new LongReturn();
            case 0x5a -> Instructions.SOFT_EXIT; // "stop executing instruction stream"
            case 0x5b -> new EraseSquareSpecial();
            case 0x5c -> new RecurseOverParty();
            case 0x5d -> new LoadAXPartyAttribute();
            case 0x5e -> new StoreAXPartyAttribute();
            case 0x5f -> new SetPartyFlag();
            case 0x60 -> new ClearPartyFlag();
            case 0x61 -> new TestPartyFlag();
            case 0x62 -> new SearchPartySkill();
            case 0x63 -> new RecurseOverInventory();
            case 0x64 -> new PickUpItem();
            case 0x65 -> new SearchSpecialItem();
            case 0x66 -> new TestHeap();
            case 0x67 -> new DropItem();
            case 0x68 -> new ReadInventoryWord();
            case 0x69 -> new WriteInventoryWord();
            case 0x6a -> new IsPartyInBox();
            case 0x6b -> new TakeOneStep(true);
            case 0x6c -> new TakeOneStep(false);
            case 0x6d -> new DrawAutomap();
            case 0x6e -> new DrawCompass();
            case 0x6f -> new RotateMapView();
            case 0x70 -> new UnrotateMapView();
            case 0x71 -> new RunBoardEvent();
            case 0x72 -> new FindBoardAction();
            case 0x73 -> Instructions.COPY_HEAP_3F_3E;
            case 0x74 -> new DrawModal();
            case 0x75 -> (i) -> { i.resetUI(); return i.getIP().incr(); };
            case 0x76 -> Instructions.FILL_BBOX;
            case 0x77 -> DecodeStringFrom.CS_WITH_FILL;
            case 0x78 -> DecodeStringFrom.CS;
            case 0x79 -> DecodeStringFrom.DS_WITH_FILL;
            case 0x7a -> DecodeStringFrom.DS;
            case 0x7b -> DecodeStringFrom.CS_TO_TITLE;
            case 0x7c -> DecodeStringFrom.DS_TO_TITLE;
            case 0x7d -> new IndirectCharName();
            case 0x7e -> new IndirectCharItem();
            case 0x7f -> new IndirectString();
            case 0x80 -> new IndentAX();
            case 0x81 -> new Print4DigitNumber();
            case 0x82 -> new Print9DigitNumber();
            case 0x83 -> new IndirectChar();
            case 0x84 -> new AllocateTempSegment();
            case 0x85 -> new FreeSegmentAL();
            case 0x86 -> new LoadChunkAX();
            case 0x87 -> new PersistChunk();
            case 0x88 -> new WaitForEscapeKey();
            case 0x89 -> new ReadKeySwitch();
            case 0x8a -> new ShowMonsterImage();
            case 0x8b -> new DrawCurrentViewport(this);
            case 0x8c -> new RunYesNoModal();
            case 0x8d -> new ReadInputString();
            case 0x8e -> Instructions.NOOP;
            case 0x8f -> new StrToInt();
            case 0x90 -> new PlaySoundEffect();
            case 0x91 -> new DrawPartyInfoArea();
            case 0x92 -> new PauseUntilKeyOrTime(this);
            case 0x93 -> new PushBL();
            case 0x94 -> new PopBL();
            case 0x95 -> new SetCursor();
            case 0x96 -> new IndentToBBox();
            case 0x97 -> new ReadPCField();
            case 0x98 -> new WritePCField();
            case 0x99 -> new TestAX();
            case 0x9a -> new LoadHeapOnes();
            case 0x9b -> new FlagSetImm();
            case 0x9c -> new FlagClearImm();
            case 0x9d -> new FlagTestImm();
            case 0x9e -> new GetSegmentSize();
            case 0x9f -> new YouWin();
            default -> throw new IllegalArgumentException(String.format("Unimplemented opcode %02x", opcode));
        };
    }
}
