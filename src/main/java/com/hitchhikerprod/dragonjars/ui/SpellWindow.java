package com.hitchhikerprod.dragonjars.ui;

import com.fasterxml.jackson.jr.ob.JSON;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class SpellWindow {
    private static final SpellWindow INSTANCE = new SpellWindow();
    private static final String RESOURCE_NAME = "spells.json";

    public static SpellWindow getInstance() {
        return INSTANCE;
    }

    private final Stage stage;

    private final ObservableList<Spell> spells;

    private SpellWindow() {
        try (final InputStream textfile = SpellWindow.class.getResourceAsStream(RESOURCE_NAME)) {
            spells = FXCollections.observableList(
                JSON.std.listOfFrom(Spell.class, Objects.requireNonNull(textfile).readAllBytes())
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final Parent root = buildTable();
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

        TableColumn<Spell, String> schoolCol = new TableColumn<>("School");
        schoolCol.setCellValueFactory(p -> p.getValue().schoolProperty());
        columns.add(schoolCol);
        TableColumn<Spell, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> p.getValue().typeProperty());
        columns.add(typeCol);
        TableColumn<Spell, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(p -> p.getValue().nameProperty());
        columns.add(nameCol);
/*
        TableColumn<Spell, String> aliasesCol = new TableColumn<>("Aliases");
        aliasesCol.setCellValueFactory(p -> p.getValue().aliasesProperty());
        columns.add(aliasesCol);
*/
        TableColumn<Spell, String> powerCol = new TableColumn<>("Power");
        powerCol.setCellValueFactory(p -> p.getValue().powerProperty());
        columns.add(powerCol);
        TableColumn<Spell, String> targetsCol = new TableColumn<>("Targets");
        targetsCol.setCellValueFactory(p -> p.getValue().targetsProperty());
        columns.add(targetsCol);
        TableColumn<Spell, String> rangeCol = new TableColumn<>("Range");
        rangeCol.setCellValueFactory(p -> p.getValue().rangeProperty());
        columns.add(rangeCol);
        TableColumn<Spell, String> effectCol = new TableColumn<>("Effect");
        effectCol.setCellValueFactory(p -> p.getValue().effectProperty());
        columns.add(effectCol);
        TableColumn<Spell, String> whereCol = new TableColumn<>("Where Found");
        whereCol.setCellValueFactory(p -> p.getValue().whereProperty());
        columns.add(whereCol);

        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(table);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
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
