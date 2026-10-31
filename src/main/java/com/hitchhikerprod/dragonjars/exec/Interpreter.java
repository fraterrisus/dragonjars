package com.hitchhikerprod.dragonjars.exec;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.CharRectangle;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ExecutableLayout;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.events.InterpreterEvent;
import com.hitchhikerprod.dragonjars.exec.events.KeyPressEvent;
import com.hitchhikerprod.dragonjars.exec.events.MethodEvent;
import com.hitchhikerprod.dragonjars.exec.instructions.Instruction;
import com.hitchhikerprod.dragonjars.exec.instructions.Instructions;
import com.hitchhikerprod.dragonjars.tasks.EyeAnimationTask;
import com.hitchhikerprod.dragonjars.tasks.MonsterAnimationTask;
import com.hitchhikerprod.dragonjars.tasks.SpellDecayTask;
import com.hitchhikerprod.dragonjars.tasks.TorchAnimationTask;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.KeyEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Interpreter implements Runnable {
    private static final int MASK_LOW = 0x000000ff;
    private static final int MASK_HIGH = 0x0000ff00;
    private static final int MASK_WORD = 0x0000ffff;

    public static int byteToInt(byte b) {
        return MASK_LOW & ((int) b);
    }

    public static byte intToByte(int i) {
        return (byte)(i & MASK_LOW);
    }

    public static final int PARTY_SEGMENT = 1;

    private final DragonWarsApp app;
    private boolean shutdownRequested = false;

    /* Utility classes */

    private final StringDecoder stringDecoder;
    private final VideoHelper videoHelper;
    private MapData mapDecoder;

    /* UI Thread Interaction */

    private final Deque<ExecutionContext> executionStack = new LinkedList<>();
    private final LinkedBlockingQueue<InterpreterEvent> eventQueue = new LinkedBlockingQueue<>();
    private EventHandler<KeyEvent> keyCallback;

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

    /* Debugging information */

    private int instructionsExecuted = 0;
    private CombatData combatData = null;

    // *******************************************************************************
    // *** Constructor
    // At this point this is a mishmash of code taken from the startup routine in the engine's executable and things
    // that I need to do for the sake of initializing a Java object. Which is which? Good luck, future engineer.

    public Interpreter(DragonWarsApp app, List<Chunk> dataChunks) {
        this.app = app;
        final boolean testMode = Objects.isNull(app);

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

        // Allocate a garbage segment (0x0) and the party segment (0x1). We handle this in the Memory constructor
        // rather than asking the Interpreter to do it.

        // build "x50" multiplication table
        // which sets ax to 0x2a
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
    }

    // *******************************************************************************
    // *** Getters and Setters for internal objects.

    public DragonWarsApp app() {
        return app;
    }

    public int instructionsExecuted() {
        return this.instructionsExecuted;
    }

    public Optional<CombatData> combatData() {
        return Optional.ofNullable(combatData);
    }

    public void setCombatData(CombatData newData) {
        this.combatData = newData;
    }

    public synchronized void requestShutdown() {
        this.shutdownRequested = true;
    }

    public synchronized boolean isShutdownRequested() {
        return this.shutdownRequested;
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

    public StringDecoder newStringDecoder() {
        return new StringDecoder(this.memory().getCodeChunk());
    }

    public MapData mapDecoder() {
        return this.mapDecoder;
    }

    public void setMapDecoder(MapData newDecoder) {
        this.mapDecoder = newDecoder;
    }

    public LinkedBlockingQueue<InterpreterEvent> eventQueue() {
        return this.eventQueue;
    }

    public void setKeyHandler(EventHandler<KeyEvent> callback) {
        this.keyCallback = callback;
    }

    public void clearKeyHandler() {
        this.keyCallback = null;
    }

    // *******************************************************************************
    // *** Getters and Setters for architectural registers (AX, BX, various ALU flags, the 'width' register, CS, DS,
    // and the architectural stack)

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

    // *******************************************************************************
    // *** Execution Frame Management (startup, shutdown, reenter, etc.)
    // The Interpreter runs on its own thread, which has to be managed by the DragonWarsApp (i.e. the JavaFX main
    // thread). The thread's main loop runs instructions while it has instructions to run, watches an event queue to
    // receive requests from the main thread, and takes shutdown requests via interrupt.

    /**
     * Start the interpreter thread.
     * Sets the initial keyboard handler, allocates the initial code segment (chunk 0x0), pushes the root state onto
     * the execution stack, and starts the main execution loop.
     */
    @Override
    public void run() {
        clearKeyHandler();
        final int startingSegmentId = memory.allocateSegment(0x0, Frob.IN_USE).id();
        this.executionStack.push(new ExecutionContext(this::shutdown, startingSegmentId, 0));
        eventLoop();
        System.err.println("Exiting interpreter thread.");
    }

    /**
     * Start the interpreter from the provided chunk ID (NOT segment) and address.
     * Forcing a chunk ID helps avoid the situation where the caller passes in a segment ID, but that segment gets
     * deallocated between the lookup and the time it's used.
     */
    public void reenter(int chunk, int addr, Runnable after) {
        final int segmentId = memory.allocateSegment(chunk, Frob.IN_USE).id();
        this.executionStack.push(new ExecutionContext(after, segmentId, addr));
        setDS(-1);
        Thread.currentThread().interrupt();
    }

    /**
     * Restart the interpreter at a new instruction pointer. Used after an asynchronous event, such as a timer or
     * keypress, and as such the current execution context should have a null IP.
     * @param chunk
     * @param addr
     */
    public void restart(int chunk, int addr) {
        clearKeyHandler();
        final int segmentId = memory.allocateSegment(chunk, Frob.IN_USE).id();
        final Address nextIP = new Address(segmentId, addr);

        // This method shouldn't get called if the current context is trying to execute instructions.
        final ExecutionContext context = this.executionStack.peek();
        assert(Objects.isNull(context.nextIP()));
        context.nextIP(nextIP);

        app().interruptInterpreterThread();
    }

    /**
     * Marks the end of an execution context stack frame.
     * */
    public Address finish() {
        this.width = false;
        this.executionStack.pop().after();
        if (Objects.nonNull(app())) app().interruptInterpreterThread();
        // reenter() callers must push the nextIP by returning it from the method that calls reenter
        return null;
    }

    /**
     * Shuts down the interpreter thread and the JavaFX main thread.
     */
    public void shutdown() {
        requestShutdown();
        if (Objects.nonNull(app())) {
            Platform.runLater(() -> this.app().close());
        }
    }

    /**
     * Add an event to the event queue that will run some arbitrary code at the next opportunity.
     * @param callback A lambda that receives a handle to the Interpreter.
     */
    public void runLater(Consumer<Interpreter> callback) {
        eventQueue().offer(new MethodEvent(callback));
    }

    private void eventLoop() {
        while (!isShutdownRequested()) {
            final Address nextIP = Objects.requireNonNull(this.executionStack.peek()).nextIP();
            if (Objects.nonNull(nextIP)) {
                // While the current execution frame has a nextIP, we should run instructions.
                executeInstruction(nextIP);
            } else {
                // If there aren't any instructions, wait for an event in our inbound queue.
                // This queue receives user interaction events (i.e. keypresses) from the JavaFX main thread
                // and various other requests (i.e. graphic updates) from child Tasks via runLater().
                InterpreterEvent ev = null;
                try {
                    ev = eventQueue.take();
                } catch (InterruptedException e) { }

                if (Objects.isNull(ev)) continue;

                switch (ev) {
                    case KeyPressEvent kev -> {
                        if (Objects.nonNull(keyCallback)) {
                            keyCallback.handle(kev.event());
                        }
                    }
                    case MethodEvent mev -> mev.run(this);
                    default -> throw new RuntimeException("Unrecognized event type " + ev.getClass().getName());
                }
            }
        }
    }

    private final int breakpointChunk = 0x03;
    private final int breakpointAddress = 0x06b5;

    private void executeInstruction(Address ip) {
        final ExecutionContext context = Objects.requireNonNull(this.executionStack.peek());

        // Manage the CS, DS, and IP "architectural registers".
        this.cs = ip.segment();
        final Segment codeSegment = memory().unsafeGetSegment(cs);
        if (this.ds == -1) this.ds = this.cs;
        this.ip = ip.offset();

        // A bit of debugging.
        if (codeSegment.frob() != Frob.IN_USE) {
            System.err.println("instruction read from " + codeSegment);
        }
        if (codeSegment.chunkId() == breakpointChunk && this.ip == breakpointAddress) {
//            System.out.println("breakpoint");
        }

        Patch.runPatches(this, codeSegment.chunkId(), this.ip);

        // Read the actual opcode from memory, figure out which Instruction we're going to run, and run it.
        // Note that each Instruction is responsible for returning the next value of IP after they execute; this is
        // because the size of an instruction in this engine is both state- and data-dependent.
        final int opcode = memory().read(ip, 1);
        final Instruction ins = Instructions.decodeOpcode(this, opcode);
        try {
            final Address nextIP = ins.exec(this);
            // Note that executing an instruction may change the execution stack, so it's important that we change the
            // nextIP field on the *current* context rather than whatever context happens to be on top at this point.
            context.nextIP(nextIP);
        } catch (Exception e) {
            System.err.format("Caught exception at [%03x:%06x]\n", codeSegment.chunkId(), this.ip);
            throw(e);
        }
        this.instructionsExecuted++;
    }

    // *******************************************************************************
    // *** Pause system.
    // The interpreter needs to be "paused" whenever a dialog box is popped. Mostly this just pauses the animation tasks
    // so they stop animating things like the spell icons, but it also stops spell decay and anything else that's
    // keeping a long timer.

    private final AtomicBoolean gameIsPaused = new AtomicBoolean(false);

    public boolean isPaused() {
        return gameIsPaused.get();
    }

    public void pause() {
        gameIsPaused.set(true);
    }

    public void unpause() {
        gameIsPaused.set(false);
    }

    // *******************************************************************************
    // *** Bounding Box handlers.
    // The engine keeps track of a single bounding box at [0257] that it uses to do things like draw dialog boxes, but
    // also uses as a mask for other draw operations. expand() and shrink() are utility methods that grow and shrink the
    // box by one character box (8 pixels in each dimension).

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

    // *******************************************************************************
    // *** Graphics "Primitives"
    // These are lower-level helper methods that perform smaller functions. Many of these are implemented very close to
    // the functions in the original engine, which is why their member variables often have hexadecimal suffixes (the
    // memory location in the x86 binary).

    public void getImageWriter(Consumer<PixelWriter> fn) {
        app().frameQueue().pushImage(fn);
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

    // *******************************************************************************
    // *** Graphics Helpers
    // Higher-level helper methods that aggregate larger sets of functionality.

    public void drawViewportCorners() {
        for (int i = 0; i < 4; i++) fg().drawCorner(i);
        bitBlast(fg(), fg().getHudRegionArea(VideoHelper.HUD_GAMEPLAY).toPixel());
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
        final int statusBitmaskLutAddress = ExecutableLayout.getInstance().getStatusBitmaskLutAddress();

        final int wordAddress = memory().getCodeChunk().read(statusBitmaskLutAddress + 8 + (2 * i), 2) - 0x100;
        this.stringDecoder.decodeString(memory().getCodeChunk(), wordAddress);

        final List<Integer> chars = new ArrayList<>();
        chars.add(0xe9); // 'i'
        chars.add(0xf3); // 's'
        chars.add(0xa0); // ' '
        chars.addAll(stringDecoder.getDecodedChars());

        int x = memory().getCodeChunk().getUnsignedByte(statusBitmaskLutAddress + 4 + i);
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
            final int offset = ExecutableLayout.getInstance().getStatusBitmaskLutAddress() + i;
            final int mask = memory().getCodeChunk().getUnsignedByte(offset);
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

    // *******************************************************************************
    // *** Animation Task / Thread Management.
    // Animations, especially the dancing monsters and the spell icons, are be handled with background threads that, in
    // turn, submit events to our queue to update the graphics.

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
        Thread.ofVirtual().start(spellDecayTask);
    }

    public void startTorchAnimation() {
        torchAnimationTask = new TorchAnimationTask(this);
        torchAnimationTask.setOnSucceeded(ev -> torchAnimationTask = null);
        torchAnimationTask.setOnFailed(ev -> torchAnimationTask = null);
        torchAnimationTask.setOnCancelled(ev -> torchAnimationTask = null);
        Thread.ofVirtual().start(torchAnimationTask);
    }
    
    public void startEyeAnimation() {
        eyeAnimationTask = new EyeAnimationTask(this);
        eyeAnimationTask.setOnSucceeded(ev -> eyeAnimationTask = null);
        eyeAnimationTask.setOnFailed(ev -> eyeAnimationTask = null);
        eyeAnimationTask.setOnCancelled(ev -> eyeAnimationTask = null);
        Thread.ofVirtual().start(eyeAnimationTask);
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
        Thread.ofVirtual().start(monsterAnimationTask);
    }

    public void stopMonsterAnimation() {
        if (Objects.nonNull(monsterAnimationTask)) {
            monsterAnimationTask.cancel();
            monsterAnimationTask = null;
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

    // *******************************************************************************

    public void autoBandage() {
        // This basically emulates the Bandage handler at [06/0171]
        Heap.lock();
        try {
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
        } finally {
            Heap.unlock();
        }
        drawPartyInfoArea();
    }
}
