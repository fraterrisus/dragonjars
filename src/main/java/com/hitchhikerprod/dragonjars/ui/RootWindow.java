package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class RootWindow {
    public static final RootWindow INSTANCE = new RootWindow();

    public static RootWindow getInstance() {
        return INSTANCE;
    }

    private final StackPane pane;

    private RootWindow() {
        pane = new StackPane();
    }

    public Parent asParent() {
        return pane;
    }

    public void start(DragonWarsApp app) {
        pane.getChildren().setAll(LoadingWindow.getInstance().asNode());
        app.loadDataFiles();
    }
}
