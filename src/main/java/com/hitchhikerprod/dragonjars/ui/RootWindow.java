package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    }

    public void setImage(Image image, double scale) {
        final ImageView imageView = new ImageView(image);
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);
        pane.getChildren().setAll(imageView);
    }
}
