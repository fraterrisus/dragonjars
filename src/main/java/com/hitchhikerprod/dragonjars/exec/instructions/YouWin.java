package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.VideoBuffer;
import com.hitchhikerprod.dragonjars.exec.VideoHelper;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;

public class YouWin implements Instruction {
    private VideoHelper bg, fg;
    private Interpreter i;

    @Override
    public Address exec(Interpreter i) {
        this.i = i;
        final Chunk codeChunk = i.memory().getCodeChunk();

        bg = new VideoHelper(codeChunk);
        bg.setVideoBuffer(new VideoBuffer());
        fg = new VideoHelper(codeChunk);
        fg.setVideoBuffer(new VideoBuffer());

        i.stopAllThreads();

        i.app().musicService().playTitleMusic(codeChunk);

        handler(0);
        return null;
    }

    private void handler(int page) {
        if (page > 4) {
            i.app().close();
            return;
        }

        final Chunk pageChunk = i.memory().copyDataChunk(ChunkTable.YOU_WIN + page);
        bg.drawChunkImage(pageChunk);

        if (page == 1) {
            pageOne(0);
            return;
        }

        i.bitBlast(bg, VideoBuffer.WHOLE_IMAGE, false);
        i.app().setKeyHandler(nextPage(page + 1));
    }

    private static final PixelRectangle BODY_1 = new PixelRectangle(28, 12, 153, 132);
    private static final PixelRectangle BODY_2 = new PixelRectangle(153, 68, 230, 140);
    private static final PixelRectangle BODY_3 = new PixelRectangle(232, 117, 263, 142);
    private static final PixelRectangle TEXT = new PixelRectangle(0, 150, 320, 200);

    private void pageOne(int box) {
        i.bitBlast(bg, VideoBuffer.WHOLE_IMAGE, false);

        fg.clearBuffer(VideoBuffer.CHROMA_KEY);
        if (box != 0) fg.drawRectangle(BODY_1, (byte)0);
        if (box != 1) fg.drawRectangle(BODY_2, (byte)0);
        if (box != 2) fg.drawRectangle(BODY_3, (byte)0);
        if (box != 3) fg.drawRectangle(TEXT, (byte)0);

        i.bitBlast(fg, VideoBuffer.WHOLE_IMAGE, true);

        if (box != 3) {
            i.app().setKeyHandler(null);
            final SleepTask sleepTask = new SleepTask(1500);
            sleepTask.setOnSucceeded(event -> pageOne(box + 1));
            Thread.ofPlatform().daemon().start(sleepTask);
        } else {
            i.app().setKeyHandler(nextPage(2));
        }
    }

    private EventHandler<KeyEvent> nextPage(int page) {
        return (event) -> {
            if (event.getCode().isModifierKey()) return;
            handler(page);
        };
    }
}
