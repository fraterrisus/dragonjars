package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.tasks.PlayChunkSound;
import com.hitchhikerprod.dragonjars.tasks.PlaySimpleSound;
import com.hitchhikerprod.dragonjars.tasks.PlayTitleMusic;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MusicService {
    private static final float FREQUENCY = 44100 * 4;
    private static final AudioFormat FORMAT = new AudioFormat(FREQUENCY, 8, 1, true, false);

    private SourceDataLine sdl;

    private final AppPreferences prefs = AppPreferences.getInstance();
    private final BooleanProperty enabled = new SimpleBooleanProperty(false);
    private final IntegerProperty volume;

    private final List<Task<Void>> runningTasks = new ArrayList<>();

    public MusicService() {
        volume = prefs.volumeProperty();

        prefs.soundEnabledProperty().addListener((obs, oVal, nVal) -> {
            System.out.println("music service received update (" + nVal + ")");
            if (nVal) enable();
            else disable();
        });
        enabled.addListener((obs, oVal, nVal) -> {
            System.out.println("music service set enabled property (" + nVal + ")");
            prefs.soundEnabledProperty().set(nVal);
        });
    }

    private static SourceDataLine openLine() {
        try {
            final SourceDataLine sdl = AudioSystem.getSourceDataLine(FORMAT);
            sdl.open(FORMAT);
            return sdl;
        } catch (LineUnavailableException e) {
            System.err.println("Unable to open audio device.");
            return null;
        }
    }

    private void enable() {
        enabled.set(true);
        if (Objects.isNull(sdl)) sdl = openLine();
        if (Objects.isNull(sdl)) {
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to initialize sound system.");
            alert.showAndWait();
            // I don't love this, but if you move it earlier, the checkbox won't toggle off.
            enabled.set(false);
        } else {
            sdl.start();
        }
    }

    public void stop() {
        // Copying the list prevents reentry errors, because when we cancel a task it immediately
        // tries to remove itself from `runningTasks`.
        new ArrayList<>(runningTasks).forEach(Task::cancel);
    }

    private void disable() {
        enabled.set(false);
        if (Objects.nonNull(sdl)) {
            stop();
            sdl.stop();
            sdl.flush();
        }
    }

    public void close() {
        if (Objects.isNull(sdl)) return;
        disable();
        sdl.close();
    }

    public void playTitleMusic(Chunk codeChunk) {
        runTaskHelper(() -> new PlayTitleMusic(sdl, volume, codeChunk));
    }

    public void playSimpleSound(int soundId) {
        runTaskHelper(() -> new PlaySimpleSound(sdl, volume, soundId));
    }

    public void playChunkSound(Chunk soundChunk) {
        runTaskHelper(() -> new PlayChunkSound(sdl, volume, soundChunk));
    }

    private void runTaskHelper(Supplier<Task<Void>> taskGetter) {
        if (!prefs.soundEnabledProperty().get()) return;
        final Task<Void> task = taskGetter.get();
        task.setOnSucceeded(removeThisTaskHelper(task));
        task.setOnFailed(removeThisTaskHelper(task));
        task.setOnCancelled(removeThisTaskHelper(task));
        Thread.ofPlatform().daemon().start(task);
        runningTasks.add(task);
    }

    private EventHandler<WorkerStateEvent> removeThisTaskHelper(Task<Void> me) {
        return event -> runningTasks.remove(me);
    }
}
