package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.exec.Heap;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

public class GameStateDialog extends Dialog<Void> {
    private static final String RESOURCE_NAME = "game-flags.txt";

    public GameStateDialog(Window parent) {
        super();
        super.initOwner(parent);
        super.setResultConverter(buttonType -> null);
        super.setTitle("Game State");

        final ButtonType dismissButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        final DialogPane root = getDialogPane();
        root.getStyleClass().add("state-dialog");
        root.getButtonTypes().add(dismissButton);
        root.setPrefHeight(300);
        root.setContent(buildGrid());
        super.setResizable(true);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
    }

    private String[] getFlagNames() {
        try (final InputStream textfile = this.getClass().getResourceAsStream(RESOURCE_NAME)) {
            return new String(Objects.requireNonNull(textfile).readAllBytes(), StandardCharsets.UTF_8).split("\\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Parent buildGrid() {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("state-grid");

        final String[] flagNames = getFlagNames();
        int heapIndex = 0x99;
        int mask = 0x80;

        int rowIndex = 0;
        for (String flagName : flagNames) {
            final CheckBox cbox = new CheckBox();
            cbox.setSelected((Heap.get(heapIndex).read() & mask) > 0);
            cbox.selectedProperty().addListener(checkBoxListener(heapIndex, mask));
            grid.addRow(rowIndex++, cbox, new Label(flagName));

            mask = mask >> 1;
            if (mask == 0) {
                heapIndex++;
                mask = 0x80;
            }
        }

        final ScrollPane scroll = new ScrollPane(grid);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private ChangeListener<? super Boolean> checkBoxListener(int heapIndex, int mask) {
        return (obs, oVal, nVal) -> {
            final Function<Integer, Integer> fn;
            if (nVal) {
                fn = (x) -> x | mask;
            } else {
                fn = (x) -> x & ~mask;
            }
            Heap.get(heapIndex).modify(1, fn);
        };
    }
}
