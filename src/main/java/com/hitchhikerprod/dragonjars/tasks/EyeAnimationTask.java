package com.hitchhikerprod.dragonjars.tasks;

import com.hitchhikerprod.dragonjars.exec.Interpreter;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;

public class EyeAnimationTask extends Task<Void> {
    private static final List<Integer> phases = List.of(
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 5, 5, 4, 4,
            3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 5, 5, 5, 5, 4, 4,
            3, 3, 3, 3, 3, 3, 2, 2, 1, 1, 2, 2
    );

    private final Interpreter interpreter;
    private int phaseIndex = 10;


    public EyeAnimationTask(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    protected Void call() throws Exception {
        while (true) {
            // doesn't run while the game is paused, huh
            Platform.runLater(() -> {
                final int imageId = 0x0e + phases.get(phaseIndex);
                interpreter.getImageWriter(writer -> interpreter.imageDecoder().decodeRomImage(imageId, writer));
            });

            sleepHelper(500);

            if (isCancelled() || interpreter.heap(0xbf).read() == 0) {
                return null;
            }

            phaseIndex = (phaseIndex + 1) % phases.size();
        }
    }

    private void sleepHelper(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
