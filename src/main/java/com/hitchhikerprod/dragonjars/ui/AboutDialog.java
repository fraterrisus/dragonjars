package com.hitchhikerprod.dragonjars.ui;

import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

public class AboutDialog extends Dialog<Void> {
    private static final String ABOUT_TEXT = "/com/hitchhikerprod/dragonjars/ui/about.txt";
    private static final String BUILD_PROPERTIES = "/com/hitchhikerprod/dragonjars/build.properties";

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
        final Properties buildProps = new Properties();
        try (InputStream istream = this.getClass().getResourceAsStream(BUILD_PROPERTIES)) {
            if (Objects.isNull(istream)) {
                throw new IOException("Null stream");
            }
            buildProps.load(istream);
        } catch (IOException e) {
            return "DragonJars\nby Ben Cordes";
        }

        return "DragonJars v" + buildProps.get("app.version") + "\nby Ben Cordes";
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
