package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Lists;
import com.hitchhikerprod.dragonjars.exec.Heap;
import javafx.beans.value.ChangeListener;
import javafx.scene.Parent;
import javafx.scene.control.*;
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

    private enum FlagType { IGNORE, GLOBAL, BOARD }

    private Parent buildGrid() {
        final GridPane grid = new GridPane();
        grid.getStyleClass().add("state-grid");

        final String thisBoard = Lists.MAP_NAMES[Heap.get(Heap.BOARD_ID).read()];

        FlagType flagType = FlagType.IGNORE;
        final String[] flagNames = getFlagNames();
        int heapIndex = 0x99;
        int mask = 0x80;

        int rowIndex = 0;
        for (String flagName : flagNames) {
            if (flagName.startsWith("-- ")) {
                final String thatBoard = flagName.substring(3);
                if (thatBoard.equalsIgnoreCase(thisBoard)) {
                    flagType = FlagType.BOARD;
                    heapIndex = 0xb9;
                    mask = 0x80;

                    final Label headerLabel = new Label(thisBoard + " (transient)");
                    headerLabel.getStyleClass().add("text-header");
                    grid.addRow(rowIndex++, headerLabel);
                    GridPane.setColumnSpan(headerLabel, 2);

                    continue;
                } else if (thatBoard.equalsIgnoreCase("Global")) {
                    flagType = FlagType.GLOBAL;
                    heapIndex = 0x99;
                    mask = 0x80;

                    final Label headerLabel = new Label("Global (permanent)");
                    headerLabel.getStyleClass().add("text-header");
                    grid.addRow(rowIndex++, headerLabel);
                    GridPane.setColumnSpan(headerLabel, 2);

                    continue;
                } else {
                    flagType = FlagType.IGNORE;
                    continue;
                }
            }

            if (flagType == FlagType.IGNORE) continue;

            if (!flagName.equalsIgnoreCase("unused")) {
                final CheckBox cbox = new CheckBox();
                cbox.setSelected((Heap.get(heapIndex).read() & mask) > 0);
                cbox.selectedProperty().addListener(checkBoxListener(heapIndex, mask));
                grid.addRow(rowIndex++, cbox, new Label(flagName));
            }

            mask = mask >> 1;
            if (mask == 0) {
                heapIndex++;
                mask = 0x80;
            }
        }

        final ScrollPane scroll = new ScrollPane(grid);
        scroll.getStyleClass().add("state-scroll");
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // applies to contents
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
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
