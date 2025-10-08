package com.hitchhikerprod.dragonjars.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
    private ListView<Text> listView;
    private CheckBox scrollEnabled;

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
        if (INSTANCE.scrollEnabled.isSelected()) INSTANCE.listView.scrollTo(newText);
    }

    public static void clear() {
        INSTANCE.listView.getItems().clear();
    }

    private Parent buildElements() {
        final BorderPane root = new BorderPane();

        listView = new ListView<>();
        listView.setPrefWidth(640);
        listView.setPrefHeight(480);

        listView.setCellFactory(view -> {
            final ListCell<Text> cell = new ListCell<>() {
                private final TextFlow flow = new TextFlow();

                @Override
                protected void updateItem(Text item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(flow);
                    flow.getChildren().clear();
                    if (Objects.nonNull(item) && !empty) flow.getChildren().setAll(item);
                }
            };
            cell.getStyleClass().add("log-cell");
            return cell;
        });

        scrollEnabled = new CheckBox("Auto-scroll on new log lines");
        scrollEnabled.setSelected(true);

        root.setCenter(listView);
        root.setBottom(scrollEnabled);
        BorderPane.setAlignment(scrollEnabled, Pos.CENTER);
        return root;
    }
}
