package com.hitchhikerprod.dragonjars.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
    private VBox logItems;
    private ScrollPane scroller;
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
        final ObservableList<Node> items = INSTANCE.logItems.getChildren();

        final Text newText = new Text(text);
        newText.getStyleClass().addAll(styleClass);

        final TextFlow nextFlow = new TextFlow(newText);
        nextFlow.setPrefWidth(Double.MAX_VALUE);

        final ObservableList<String> flowStyles = nextFlow.getStyleClass();
        flowStyles.add("log-cell");
        flowStyles.add((items.size() % 2 == 0) ? "dj-background" : "dj-background-alt");

        items.add(nextFlow);
    }

    public static void clear() {
        INSTANCE.logItems.getChildren().clear();
    }

    private Parent buildElements() {
        final BorderPane root = new BorderPane();

        logItems = new VBox();
        logItems.setPrefWidth(640);
        logItems.setPrefHeight(480);
        logItems.setAlignment(Pos.TOP_LEFT);
        logItems.setFillWidth(true);
        logItems.getStyleClass().addAll("log-cell-holder", "dj-background");

        scroller = new ScrollPane(logItems);
        // useless if we're using TextFlows
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroller.setFitToHeight(true);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("dj-background");

        scrollEnabled = new CheckBox("Auto-scroll on new log lines");
        scrollEnabled.setSelected(true);

        logItems.heightProperty().addListener((obs, oVal, nVal) -> {
            if (INSTANCE.scrollEnabled.isSelected()) {
                final double vTarget = INSTANCE.scroller.getVmax();
                INSTANCE.scroller.setVvalue(vTarget);
                final double hTarget = INSTANCE.scroller.getHmin();
                INSTANCE.scroller.setHvalue(hTarget);
            }
        });

        root.setCenter(scroller);
        root.setBottom(scrollEnabled);
        BorderPane.setAlignment(scrollEnabled, Pos.CENTER);
        return root;
    }
}
