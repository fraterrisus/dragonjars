package com.hitchhikerprod.dragonjars.ui;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ParagraphsWindow {
    private static final ParagraphsWindow INSTANCE = new ParagraphsWindow();
    private static final String RESOURCE_NAME = "paragraphs.txt";

    public static ParagraphsWindow getInstance() { return INSTANCE; }

    record Paragraph(int index, String body) {}

    private final Stage stage;
    private final Scene scene;

    private ListView<Paragraph> listView;
    private TextArea textView;

    private ParagraphsWindow() {
        final Parent root = buildElements();
        this.scene = new Scene(root);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
        root.getStyleClass().add("paragraphs-window");

        this.stage = new Stage();
        this.stage.initModality(Modality.NONE);
        this.stage.initStyle(StageStyle.DECORATED);
        this.stage.setTitle("Paragraphs");
        this.stage.setResizable(true);
        this.stage.setScene(scene);

        this.stage.setMinWidth(600);
        this.stage.setMinHeight(300);

        populateParagraphList();

        stage.sizeToScene();
    }

    public void show() {
        this.stage.show();
    }

    public void show(int index) {
        this.stage.show();
        listView.getItems().stream()
            .filter(paragraph -> paragraph.index == index).findFirst()
            .ifPresent(p -> textView.setText(p.body()));
    }

    public void hide() {
        this.stage.hide();
    }

    private Parent buildElements() {
        final BorderPane root = new BorderPane();

        root.setLeft(buildListPane());

        final Node textPane = buildTextPane();
        root.setCenter(textPane);
        BorderPane.setAlignment(root, Pos.TOP_LEFT);

        return root;
    }

    private Node buildListPane() {
        listView = new ListView<>();

        listView.setCellFactory(ParagraphsWindow::paragraphCellFactory);

        final MultipleSelectionModel<Paragraph> selectionModel = listView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) textView.setText("");
            else textView.setText(nVal.body());
        });

        listView.setPrefWidth(96);
        listView.setMaxWidth(128);

        return listView;
    }

    private Node buildTextPane() {
        textView = new TextArea();
        textView.setEditable(false);
        textView.setWrapText(true);

        textView.setPrefWidth(640);
        textView.setPrefHeight(480);

        return textView;
    }

    private void populateParagraphList() {
        try (final InputStream textfile = this.getClass().getResourceAsStream(RESOURCE_NAME)) {
            final ObservableList<Paragraph> paragraphItems = listView.getItems();
            paragraphItems.clear();
            final String fileBody = new String(Objects.requireNonNull(textfile).readAllBytes(), StandardCharsets.UTF_8);
            int index = 1;
            for (String paragraph: fileBody.split("\\n-----\\n")) {
                paragraph = paragraph.replaceAll("\\n\\n", "##");
                paragraph = paragraph.replaceAll("\\n", " ");
                paragraph = paragraph.replaceAll("##", "\n\n");
                paragraphItems.add(new Paragraph(index++, paragraph));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ListCell<Paragraph> paragraphCellFactory(ListView<Paragraph> ignored) {
        final ListCell<Paragraph> cell = new ListCell<>() {
            @Override
            protected void updateItem(Paragraph item, boolean empty) {
                super.updateItem(item, empty);
                if (Objects.isNull(item) || empty) {
                    setText("");
                } else {
                    setText(String.valueOf(item.index()));
                }
            }
        };
        cell.getStyleClass().add("paragraph-cell");
        return cell;
    }
}
