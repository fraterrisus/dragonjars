package com.hitchhikerprod.dragonjars.ui;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AboutDialog extends Dialog<Void> {
    private static final String RESOURCE_NAME = "about.txt";

    public AboutDialog(Window parent) {
        super();
        super.initOwner(parent);
        super.setResultConverter(buttonType -> null);
        super.setTitle("About DragonJars");
        super.setHeaderText("DragonJars v0.9-dev\nby Ben Cordes");
        super.setContentText(getText());

        final ButtonType dismissButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        final DialogPane root = getDialogPane();
        root.getStyleClass().add("about-dialog");
        root.getButtonTypes().add(dismissButton);
        root.setMinHeight(Region.USE_PREF_SIZE);
        super.setResizable(false);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
    }

    private String getText() {
        try (final InputStream textfile = this.getClass().getResourceAsStream(RESOURCE_NAME)) {
            return new String(Objects.requireNonNull(textfile).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
