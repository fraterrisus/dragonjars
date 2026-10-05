package com.hitchhikerprod.dragonjars.exec.events;

import javafx.scene.input.KeyEvent;

public record KeyPressEvent(KeyEvent event) implements InterpreterEvent { }
