package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import com.hitchhikerprod.dragonjars.data.Images;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_X;
import static com.hitchhikerprod.dragonjars.DragonWarsApp.IMAGE_Y;

public class RootWindow {
    public static final RootWindow INSTANCE = new RootWindow();

    public static RootWindow getInstance() {
        return INSTANCE;
    }

    private static final String RESOURCE_NAME = "welcome.txt";

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
        final Label welcomeText = new Label(getWelcomeText());
        welcomeText.getStyleClass().add("welcome-text");
        this.pane.getChildren().setAll(welcomeText);

        final PreferencesWindow prefsWindow = PreferencesWindow.getInstance();
        prefsWindow.start(app);

        final AppPreferences prefs = AppPreferences.getInstance();
        prefs.scaleProperty().addListener((obs, oVal, nVal) -> app.resize());
    }

    private String getWelcomeText() {
        try (final InputStream textfile = this.getClass().getResourceAsStream(RESOURCE_NAME)) {
            return new String(Objects.requireNonNull(textfile).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLoading() {
        this.pane.getChildren().setAll(LoadingWindow.getInstance().asNode());
    }

    public void setStylesheets(URL cssUrl) {
        pane.getStylesheets().add(cssUrl.toExternalForm());
        menuBar.setStylesheets(cssUrl);
    }

    public Image getImage() {
        final Node node = pane.getChildren().getFirst();
        if (node instanceof ImageView imageView) {
            return imageView.getImage();
        } else {
            final WritableImage image = Images.blankImage(IMAGE_X, IMAGE_Y);
            setImage(image);
            return image;
        }
    }

    public void setImage(Image image) {
        final ImageView imageView = new ImageView(image);
        final AppPreferences prefs = AppPreferences.getInstance();
        imageView.setPreserveRatio(true);
        imageView.scaleXProperty().bind(prefs.scaleProperty());
        imageView.scaleYProperty().bind(prefs.scaleProperty());
        pane.prefWidthProperty().unbind();
        pane.prefWidthProperty().bind(
                prefs.scaleProperty().multiply(image.widthProperty())
        );
        pane.prefHeightProperty().unbind();
        pane.prefHeightProperty().bind(
                prefs.scaleProperty().multiply(image.heightProperty())
        );
        pane.getChildren().setAll(imageView);
        app.resize();
    }
}
