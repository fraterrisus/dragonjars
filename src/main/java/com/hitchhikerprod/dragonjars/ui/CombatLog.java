package com.hitchhikerprod.dragonjars.ui;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class CombatLog {
    private static final CombatLog INSTANCE = new CombatLog();

    public static CombatLog getInstance() {
        return INSTANCE;
    }

    private final Stage stage;
    private TextFlow textView;
    private ScrollPane scrollPane;

    private CombatLog() {
        final Parent root = buildElements();
        final Scene scene = new Scene(root);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
        root.getStyleClass().add("combat-log");

        this.stage = new Stage();
        this.stage.initModality(Modality.NONE);
        this.stage.initStyle(StageStyle.DECORATED);
        this.stage.setTitle("Combat Log");
        this.stage.setResizable(true);
        this.stage.setScene(scene);

        this.stage.setMinWidth(600);
        this.stage.setMinHeight(300);

        stage.sizeToScene();
    }

    public void show() {
        this.stage.show();
    }

    public static void append(String text) {
        // TODO: allow rich text
        INSTANCE.textView.getChildren().add(new Text(text + "\n"));
        INSTANCE.scrollPane.setVvalue(1.0);
    }

    private Parent buildElements() {
        textView = new TextFlow();
        textView.setPrefWidth(640);
        textView.setPrefHeight(480);

        // TODO: "scroll to bottom" checkbox

        scrollPane = new ScrollPane(textView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        return scrollPane;
    }
}
