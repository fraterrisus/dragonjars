package com.hitchhikerprod.dragonjars.ui;

import com.fasterxml.jackson.jr.ob.JSON;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SpellWindow {
    private static final SpellWindow INSTANCE = new SpellWindow();
    private static final String RESOURCE_NAME = "spells.json";

    public static SpellWindow getInstance() {
        return INSTANCE;
    }

    private final Stage stage;

    private final ObservableList<Spell> spells = FXCollections.observableArrayList();

    private SpellWindow() {
        final Parent root = buildTable();

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
        root.getStyleClass().add("spells-window");

        try (final InputStream textfile = SpellWindow.class.getResourceAsStream(RESOURCE_NAME)) {
            spells.addAll(JSON.std.listOfFrom(Spell.class, Objects.requireNonNull(textfile).readAllBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.stage = new Stage();
        this.stage.initModality(Modality.NONE);
        this.stage.initStyle(StageStyle.DECORATED);
        this.stage.setTitle("Spells");
        this.stage.setResizable(true);
        this.stage.setScene(new Scene(root));

        this.stage.setMinWidth(800);
        this.stage.setMinHeight(400);
        this.stage.sizeToScene();
    }

    public void show() {
        for (Spell spell : spells) System.out.println(spell);
        this.stage.show();
    }

    private Parent buildTable() {
        final TableView<Spell> table = new TableView<>();
        table.setItems(spells);

        final ObservableList<TableColumn<Spell, ?>> columns = table.getColumns();

        final TableColumn<Spell, String> schoolCol = new TableColumn<>("School");
        schoolCol.setCellValueFactory(p -> p.getValue().schoolProperty());
        schoolCol.setCellFactory(alignedCellFactory(Pos.CENTER));
        columns.add(schoolCol);

        final TableColumn<Spell, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> p.getValue().typeProperty());
        typeCol.setCellFactory(alignedCellFactory(Pos.CENTER));
        columns.add(typeCol);

        final TableColumn<Spell, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(p -> p.getValue().nameProperty());
        nameCol.setCellFactory(alignedCellFactory(Pos.TOP_LEFT));
        columns.add(nameCol);
/*
        TableColumn<Spell, String> aliasesCol = new TableColumn<>("Aliases");
        aliasesCol.setCellValueFactory(p -> p.getValue().aliasesProperty());
        columns.add(aliasesCol);
*/
        final TableColumn<Spell, String> powerCol = new TableColumn<>("Power");
        powerCol.setCellValueFactory(p -> p.getValue().powerProperty());
        powerCol.setCellFactory(alignedCellFactory(Pos.CENTER));
        powerCol.setComparator(powerComparator(powerCol));
        columns.add(powerCol);

        final TableColumn<Spell, String> targetsCol = new TableColumn<>("Targets");
        targetsCol.setCellValueFactory(p -> p.getValue().targetsProperty());
        targetsCol.setCellFactory(alignedCellFactory(Pos.CENTER));
        columns.add(targetsCol);

        final TableColumn<Spell, String> rangeCol = new TableColumn<>("Range");
        rangeCol.setCellValueFactory(p -> p.getValue().rangeProperty());
        rangeCol.setCellFactory(alignedCellFactory(Pos.CENTER));
        rangeCol.setComparator(rangeComparator());
        columns.add(rangeCol);

        final TableColumn<Spell, String> effectCol = new TableColumn<>("Effect");
        effectCol.setCellValueFactory(p -> p.getValue().effectProperty());
        effectCol.setCellFactory(alignedCellFactory(Pos.TOP_LEFT));
        effectCol.setSortable(false);
        columns.add(effectCol);

        final TableColumn<Spell, String> whereCol = new TableColumn<>("Where Found");
        whereCol.setCellValueFactory(p -> p.getValue().whereProperty());
        whereCol.setCellFactory(alignedCellFactory(Pos.TOP_LEFT));
        whereCol.setSortable(false);
        columns.add(whereCol);

        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(table);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }

    private static Comparator<String> powerComparator(TableColumn<Spell, String> col) {
        return (a, b) -> {
            final int bottom;
            final int top;
            switch (col.getSortType()) {
                case ASCENDING -> { bottom = -1; top = 1; }
                case DESCENDING -> { bottom = 1; top = -1; }
                default -> { bottom = 0; top = 0; }
            }
            if (a == null) return top;
            if (b == null) return bottom;
            if (a.equals("var")) return top;
            if (b.equals("var")) return bottom;
            final int ai = Integer.parseInt(a);
            final int bi = Integer.parseInt(b);
            return ai - bi;
        };
    }

    private static Comparator<String> rangeComparator() {
        return (a, b) -> {
            final int ai = (a.isEmpty()) ? 0 : Integer.parseInt(a.substring(0, a.length() - 2));
            final int bi = (b.isEmpty()) ? 0 : Integer.parseInt(b.substring(0, b.length() - 2));
            return Integer.compare(ai, bi);
        };
    }

    private Callback<TableColumn<Spell, String>, TableCell<Spell, String>> alignedCellFactory(Pos alignment) {
        return (col) -> {
            final TableCell<Spell, String> cell = new TableCell<>() {
                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setAlignment(alignment);
            cell.getStyleClass().add("spell-cell");
            return cell;
        };
    }

    private record Spell(String school, String name, List<String> aliases, String power, String targets, String range,
                         String type, String where, String effect) {

        public StringProperty schoolProperty() {
            return new ReadOnlyStringWrapper(school);
        }

        public StringProperty nameProperty() {
            return new ReadOnlyStringWrapper(name);
        }

        public StringProperty powerProperty() {
            return new ReadOnlyStringWrapper(power);
        }

        public StringProperty targetsProperty() {
            return new ReadOnlyStringWrapper(targets);
        }

        public StringProperty rangeProperty() {
            return new ReadOnlyStringWrapper(range);
        }

        public StringProperty typeProperty() {
            return new ReadOnlyStringWrapper(type);
        }

        public StringProperty whereProperty() {
            return new ReadOnlyStringWrapper(where);
        }

        public StringProperty effectProperty() {
            return new ReadOnlyStringWrapper(effect);
        }

        public StringProperty aliasesProperty() {
            final String aliasesValue;
            if (Objects.isNull(aliases)) aliasesValue = "";
            else aliasesValue = String.join(", ", aliases);
            return new ReadOnlyStringWrapper(aliasesValue);
        }
    }
}
