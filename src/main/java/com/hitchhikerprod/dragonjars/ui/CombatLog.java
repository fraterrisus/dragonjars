package com.hitchhikerprod.dragonjars.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.Objects;

public class CombatLog {
    private static final CombatLog INSTANCE = new CombatLog();

    public static CombatLog getInstance() {
        return INSTANCE;
    }

    private final Stage stage;
    private TextFlow textView;
    private ScrollPane scrollPane;
    private ListView<Text> listView;

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

    public static void append(String text, String... styleClass) {
        final Text newText = new Text(text);
        newText.getStyleClass().addAll(styleClass);
        INSTANCE.listView.getItems().add(newText);
        INSTANCE.listView.scrollTo(newText);
    }

    public static void clear() {
        INSTANCE.listView.getItems().clear();
    }

    private Parent buildElements() {
        listView = new ListView<>();
        listView.setPrefWidth(640);
        listView.setPrefHeight(480);

        listView.setCellFactory(view ->
            new ListCell<>() {
                private final TextFlow flow = new TextFlow();

                @Override
                protected void updateItem(Text item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(flow);
                    flow.getChildren().clear();
                    if (Objects.nonNull(item) && !empty) flow.getChildren().setAll(item);
                }
            }
        );

        return listView;
    }
}
