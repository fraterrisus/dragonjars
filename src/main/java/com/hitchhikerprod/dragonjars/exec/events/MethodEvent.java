package com.hitchhikerprod.dragonjars.exec.events;

import com.hitchhikerprod.dragonjars.exec.Interpreter;

import java.util.function.Consumer;

public record MethodEvent (Consumer<Interpreter> callback) implements InterpreterEvent {
    public void run(Interpreter i) {
        this.callback.accept(i);
    }
}
