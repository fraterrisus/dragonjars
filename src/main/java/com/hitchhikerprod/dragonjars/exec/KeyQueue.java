package com.hitchhikerprod.dragonjars.exec;

import javafx.scene.input.KeyCode;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A synchronized queue for passing keypresses from the JavaFX UI thread to the Interpreter thread.
 */
public class KeyQueue {
    private final Deque<KeyCode> keycodes = new LinkedList<>();

    public synchronized void send(KeyCode kc) {
        keycodes.add(kc);
        notifyAll();
    }

    public synchronized KeyCode receive() {
        while (keycodes.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return keycodes.removeFirst();
    }
}
