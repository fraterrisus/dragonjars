package com.hitchhikerprod.dragonjars.ui;

import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AboutDialog extends Dialog<Void> {
    private static final String ABOUT_TEXT = "/com/hitchhikerprod/dragonjars/ui/about.txt";

    public AboutDialog(Window parent) {
        super();
        super.initOwner(parent);
        super.setResultConverter(buttonType -> null);
        super.setTitle("About DragonJars");
        super.setHeaderText(getHeader());
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

    private String getHeader() {
        return "DragonJars " + AppPreferences.getInstance().getAppVersion() + "\nⓒ Ben Cordes 2024–2026";
    }

    private String getText() {
        try (final InputStream textFile = this.getClass().getResourceAsStream(ABOUT_TEXT)) {
            if (Objects.isNull(textFile)) {
                throw new IOException("Null stream");
            }
            return new String(textFile.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
