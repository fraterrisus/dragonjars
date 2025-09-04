package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.PixelRectangle;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.VideoBuffer;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class YouWin implements Instruction {
    private final WritableImage image = Images.blankImage(DragonWarsApp.IMAGE_X, DragonWarsApp.IMAGE_Y);
    private final VideoBuffer vb = new VideoBuffer();
    private Interpreter i;

    @Override
    public Address exec(Interpreter i) {
        this.i = i;
        i.app().musicService().enable();
        i.app().musicService().playTitleMusic(i.memory().getCodeChunk());
        i.imageDecoder().setVideoBuffer(vb);
        handler(0);
        i.app().setImage(image);
        return null;
    }

    private void handler(int page) {
        final Chunk pageChunk = i.memory().copyDataChunk(ChunkTable.YOU_WIN + page);
        i.imageDecoder().decodeChunkImage(pageChunk);
        vb.writeTo(image.getPixelWriter(), VideoBuffer.WHOLE_IMAGE, false);

        switch (page) {
            case 1 -> pageOne(0);
            case 4 -> i.app().setKeyHandler(event -> {
                if (event.getCode().isModifierKey()) return;
                i.app().close();
            });
            default -> i.app().setKeyHandler(event -> {
                if (event.getCode().isModifierKey()) return;
                handler(page + 1);
            });
        }
    }

    private static final PixelRectangle BODY_1 = new PixelRectangle(28, 153, 12, 132);
    private static final PixelRectangle BODY_2 = new PixelRectangle(153, 230, 68, 140);
    private static final PixelRectangle BODY_3 = new PixelRectangle(232, 263, 117, 142);
    private static final PixelRectangle TEXT = new PixelRectangle(0, 320, 150, 200);

    private void pageOne(int box) {
        final PixelWriter writer = image.getPixelWriter();

        if (box != 0) blackOut(writer, BODY_1);
        if (box != 1) blackOut(writer, BODY_2);
        if (box != 2) blackOut(writer, BODY_3);
        if (box != 3) blackOut(writer, TEXT);

        if (box != 3) {
            i.app().setKeyHandler(null);
            final SleepTask sleepTask = new SleepTask(1000);
            sleepTask.setOnSucceeded(event -> pageOne(box+1));
            final Thread thread = new Thread(sleepTask);
            thread.setDaemon(true);
            thread.start();
        } else {
            i.app().setKeyHandler(event -> {
                if (event.getCode().isModifierKey()) return;
                handler(2);
            });
        }
    }

    private void blackOut(PixelWriter writer, PixelRectangle rect) {
        final int black = Images.convertColorIndex(0);
        for (int y = rect.y0(); y < rect.y1(); y++) {
            for (int x = rect.x0(); x < rect.x1(); x++) {
                writer.setArgb(x, y, black);
            }
        }
    }
}
