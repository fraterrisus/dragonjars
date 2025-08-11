package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

import java.net.URL;

public class RootWindow {
    public static final RootWindow INSTANCE = new RootWindow();

    public static RootWindow getInstance() {
        return INSTANCE;
    }

    private DragonWarsApp app;
    private final StackPane pane;

    private RootWindow() {
        pane = new StackPane();
    }

    public Parent asParent() {
        return pane;
    }

    public void start(DragonWarsApp app) {
        this.app = app;
        pane.getChildren().setAll(LoadingWindow.getInstance().asNode());
    }

    public void setStyleSheets(URL cssUrl) {
        pane.getStylesheets().add(cssUrl.toExternalForm());
    }

    public Image getImage() {
        final Node node = pane.getChildren().getFirst();
        if (node instanceof ImageView imageView) {
            return imageView.getImage();
        }
        throw new RuntimeException("Image not found");
    }

    public void setImage(Image image, double scale) {
        final ImageView imageView = new ImageView(image);
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);
        pane.setPrefWidth(imageView.getImage().getWidth() * scale);
        pane.setPrefHeight(imageView.getImage().getHeight() * scale);
        pane.getChildren().setAll(imageView);
    }

}
