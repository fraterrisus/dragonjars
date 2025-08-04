package com.hitchhikerprod.dragonjars.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class LoadingWindow {
    public static final LoadingWindow INSTANCE = new LoadingWindow();

    public static LoadingWindow getInstance() {
        return INSTANCE;
    }

    private final VBox pane;
    private final Label label;
    private final ProgressBar progressBar;

    private LoadingWindow() {
        progressBar = new ProgressBar(0.0);
        progressBar.setPrefWidth(300.0);

        label = new Label("Loading...");
        label.setAlignment(Pos.TOP_CENTER);

        pane = new VBox();
        final ObservableList<Node> children = pane.getChildren();
        children.add(label);
        children.add(progressBar);
    }

    public Node asNode() {
        return pane;
    }

    public Label getLabel() {
        return label;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
