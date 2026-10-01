package com.hitchhikerprod.dragonjars.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_X;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_Y;

public class FrameQueue extends AnimationTimer {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Deque<Image> images = new LinkedList<>();

    @Override
    public void handle(long now) {
        if (images.isEmpty()) return;
        if (lock.isHeldByCurrentThread()) return;

        // Don't wait for anyone else.
        // In theory, this means that if there are multiple writers queued up to add new frames, none of them will
        // actually make it to the screen until they all finish writing. Although tryLock will also jump the fairness
        // queue, so maybe it sneaks in?
        if (!lock.tryLock()) return;
        Image newFrame = null;
        if (!images.isEmpty()) {
            newFrame = images.getLast();
            images.clear();
        }
        lock.unlock();

        if (Objects.nonNull(newFrame)) RootWindow.getInstance().setImage(newFrame);
    }

    public void pushImage(Image newImage) {
        lock.lock();
        images.add(newImage);
        lock.unlock();
    }

    public void pushImage(Consumer<PixelWriter> generator) {
        lock.lock();
        final Image inputImage;
        if (images.isEmpty()) {
            inputImage = RootWindow.getInstance().getImage();
        } else {
            inputImage = images.pollLast();
        }
        final WritableImage wImage = new WritableImage(inputImage.getPixelReader(), IMAGE_X, IMAGE_Y);
        generator.accept(wImage.getPixelWriter());
        images.add(wImage);
        lock.unlock();
    }
}
