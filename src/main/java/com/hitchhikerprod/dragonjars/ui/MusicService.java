package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.tasks.PlayChunkSound;
import com.hitchhikerprod.dragonjars.tasks.PlaySimpleSound;
import com.hitchhikerprod.dragonjars.tasks.PlayTitleMusic;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

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

    private static SourceDataLine sdl;

    private final SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<Integer> volume = new SimpleObjectProperty<>(50);

    private final List<Task<Void>> runningTasks = new ArrayList<>();

    public MusicService() {
        sdl = openLine();
        this.enabled.set(sdl != null);
        this.volume.set(50);
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

    // TODO make this configurable
    public SimpleObjectProperty<Integer> volumeProperty() {
        return volume;
    }

    public SimpleBooleanProperty enabledProperty() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void enable() {
        if (Objects.isNull(sdl)) sdl = openLine();
        if (Objects.isNull(sdl)) {
            this.enabled.set(false);
        } else {
            this.enabled.set(true);
            sdl.start();
        }
    }

    public void stop() {
        // Copying the list prevents reentry errors, because when we cancel a task it immediately
        // tries to remove itself from `runningTasks`.
        new ArrayList<>(runningTasks).forEach(Task::cancel);
    }

    public void disable() {
        if (Objects.nonNull(sdl)) {
            stop();
            sdl.stop();
            sdl.flush();
        }
        this.enabled.set(false);
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
        if (!isEnabled()) return;
        final Task<Void> task = taskGetter.get();
        task.setOnSucceeded(removeThisTaskHelper(task));
        task.setOnFailed(removeThisTaskHelper(task));
        task.setOnCancelled(removeThisTaskHelper(task));
        final Thread musicThread = new Thread(task);
        musicThread.setDaemon(true);
        musicThread.start();
        runningTasks.add(task);
    }

    private EventHandler<WorkerStateEvent> removeThisTaskHelper(Task<Void> me) {
        return event -> runningTasks.remove(me);
    }
}
