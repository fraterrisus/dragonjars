package com.hitchhikerprod.dragonjars.exec.instructions;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkImageDecoder;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.Rectangle;
import com.hitchhikerprod.dragonjars.exec.Address;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.SleepTask;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class YouWin implements Instruction {
    private Interpreter i;

    @Override
    public Address exec(Interpreter i) {
        this.i = i;
        i.app().musicService().enable();
        i.app().musicService().playTitleMusic(i.memory().getCodeChunk());
        handler(0);
        return null;
    }

    private void handler(int page) {
        final Chunk pageChunk = i.memory().copyDataChunk(ChunkTable.YOU_WIN + page);
        final Image pageImage = new ChunkImageDecoder(pageChunk).parse();
        switch (page) {
            case 1 -> pageOne(pageImage, 0);
            case 4 -> {
                i.app().setImage(pageImage);
                i.app().setKeyHandler(event -> {
                    if (event.getCode().isModifierKey()) return;
                    i.app().close();
                });
            }
            default -> {
                i.app().setImage(pageImage);
                i.app().setKeyHandler(event -> {
                    if (event.getCode().isModifierKey()) return;
                    handler(page + 1);
                });
            }
        }
    }

    private static final Rectangle BODY_1 = new Rectangle(28, 153, 12, 132);
    private static final Rectangle BODY_2 = new Rectangle(153, 230, 68, 140);
    private static final Rectangle BODY_3 = new Rectangle(232, 263, 117, 142);
    private static final Rectangle TEXT = new Rectangle(0, 320, 150, 200);

    private void pageOne(Image pageImage, int box) {
        final WritableImage wImage = new WritableImage(pageImage.getPixelReader(), (int)pageImage.getWidth(), (int)pageImage.getHeight());
        final PixelWriter writer = wImage.getPixelWriter();

        if (box != 0) blackOut(writer, BODY_1);
        if (box != 1) blackOut(writer, BODY_2);
        if (box != 2) blackOut(writer, BODY_3);
        if (box != 3) blackOut(writer, TEXT);
        i.app().setImage(wImage);

        if (box != 3) {
            i.app().setKeyHandler(null);
            final SleepTask sleepTask = new SleepTask(1000);
            sleepTask.setOnSucceeded(event -> pageOne(pageImage, box+1));
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

    private void blackOut(PixelWriter writer, Rectangle rect) {
        final int black = Images.convertColorIndex(0);
        for (int y = rect.y0(); y < rect.y1(); y++) {
            for (int x = rect.x0(); x < rect.x1(); x++) {
                writer.setArgb(x, y, black);
            }
        }
    }
}
