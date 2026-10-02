package com.hitchhikerprod.dragonjars.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleDoubleProperty;
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

/**
 * A class designed to allow threadsafe updates to the RootWindow and the base Image we use for displaying graphics.
 * Queue input comes from calls to #pushImage() with either a pre-populated `Image` object or a lambda that receives a
 * `PixelWriter` as a parameter and updates a bunch of pixels. Queue output is handled by `AnimationTimer#handle` which
 * is called once per animation pulse, reads the most recent image written to the queue, and writes it to the
 * RootWindow.
 * */
public class FrameQueue extends AnimationTimer {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Deque<Image> images = new LinkedList<>();

    private static final long ONE_SECOND = 1_000_000_000;
    private Long lastUpdate = 0L;
    private Long numFrames = 0L;
    public SimpleDoubleProperty fps = new SimpleDoubleProperty(0.0);

    /**
     * Read the most-recently written image on the queue, update the RootWindow, and clear the queue. The idea here is
     * that if multiple writes have occurred since the last time we read from the queue, we may skip some of those
     * intermediate frames because they don't matter.
     *
     * @param now The timestamp of the current frame given in nanoseconds. This is used to compute the display FPS.
     */
    @Override
    public void handle(long now) {
        numFrames++;
        if (lastUpdate < now - ONE_SECOND) {
            fps.set(1.0 * numFrames * ONE_SECOND / (now - lastUpdate));
            numFrames = 0L;
            lastUpdate = now;
        }

        if (images.isEmpty()) return;
        if (lock.isHeldByCurrentThread()) return;

        // Don't wait for anyone else.
        // In theory, this means that if there are multiple writers queued up to add new frames, none of them will
        // actually make it to the screen until they all finish writing. Although tryLock will also jump the fairness
        // queue, so maybe it sneaks in?
        if (!lock.tryLock()) return;

        Image newFrame = null;
        try {
            if (!images.isEmpty()) {
                newFrame = images.getLast();
                images.clear();
            }
        } finally {
            lock.unlock();
        }

        if (Objects.nonNull(newFrame)) RootWindow.getInstance().setImage(newFrame);
    }

    /**
     * Push an `Image` onto the queue. This method should be called when the caller has a completely new image to push.
     * If the caller desires to make changes to the previous frame, use #pushImage(Consumer&lt;PixelWriter&gt;) instead.
     *
     * @param newImage The Image to push
     */
    public void pushImage(Image newImage) {
        lock.lock();
        try {
            images.add(newImage);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Used when the caller desires to make changes to the current frame. A copy of the most recent frame will be made,
     * and a `PixelWriter` pointing to that copy passed back to the generator lambda so that the caller can make
     * modifications to it.
     *
     * @param generator A lambda that receives a PixelWriter.
     */
    public void pushImage(Consumer<PixelWriter> generator) {
        lock.lock();
        try {
            final Image inputImage;
            if (images.isEmpty()) {
                inputImage = RootWindow.getInstance().getImage();
            } else {
                inputImage = images.getLast();
            }
            final WritableImage wImage = new WritableImage(inputImage.getPixelReader(), IMAGE_X, IMAGE_Y);
            generator.accept(wImage.getPixelWriter());
            images.add(wImage);
        } finally {
            lock.unlock();
        }
    }
}
