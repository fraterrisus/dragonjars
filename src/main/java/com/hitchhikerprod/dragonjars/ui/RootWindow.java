package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;

public class RootWindow {
    public static final RootWindow INSTANCE = new RootWindow();

    public static RootWindow getInstance() {
        return INSTANCE;
    }

    private DragonWarsApp app;
    private final VBox root;
    private final MenuBar menuBar;
    private final StackPane pane;

    private RootWindow() {
        menuBar = MenuBar.getInstance();
        pane = new StackPane();
        root = new VBox(menuBar.asNode(), pane);
    }

    public Parent asParent() {
        return root;
    }

    public void start(DragonWarsApp app) {
        this.app = app;
        this.menuBar.start(app);
        this.pane.getChildren().setAll(LoadingWindow.getInstance().asNode());

        this.menuBar.videoScaleProperty().addListener((obs, oVal, nVal) -> resetScale(nVal));
    }

    public void setStylesheets(URL cssUrl) {
        pane.getStylesheets().add(cssUrl.toExternalForm());
        menuBar.setStylesheets(cssUrl);
    }

    public DoubleProperty volumeProperty() {
        return this.menuBar.volumeProperty();
    }

    public IntegerProperty videoScaleProperty() {
        return this.menuBar.videoScaleProperty();
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
        imageView.setPreserveRatio(true);
        pane.setPrefWidth(imageView.getImage().getWidth() * scale);
        pane.setPrefHeight(imageView.getImage().getHeight() * scale);
        pane.getChildren().setAll(imageView);
    }

    private void resetScale(Number factor) {
        double scale = factor.doubleValue();
        final Node node = pane.getChildren().getFirst();
        if (node instanceof ImageView imageView) {
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            pane.setPrefWidth(imageView.getImage().getWidth() * scale);
            pane.setPrefHeight(imageView.getImage().getHeight() * scale);
            // root.requestLayout();
            app.resize();
        }
    }
}
