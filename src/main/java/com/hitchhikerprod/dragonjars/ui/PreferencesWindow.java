package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public class PreferencesWindow {
    private static final PreferencesWindow INSTANCE = new PreferencesWindow();

    public static PreferencesWindow getInstance() {
        return INSTANCE;
    }

    private final Stage stage;
    private final Scene scene;

    private final Button execButton = new Button();
    private final Button data1Button = new Button();
    private final Button data2Button = new Button();
    private CheckBox backRowThrown;

    private PreferencesWindow() {
        final Parent root = buildGrid();
        this.scene = new Scene(root);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
        root.getStyleClass().add("prefs-window");

        this.stage = new Stage();
        this.stage.setTitle("Preferences");
        this.stage.setScene(scene);
        this.stage.setResizable(true);
    }

    public void start(DragonWarsApp app) {
        final AppPreferences prefs = AppPreferences.getInstance();

        execButton.setOnAction(ev -> {
            prefs.executablePathProperty().set(app.runOpenFileDialog("DRAGON.COM"));
            app.loadDataFiles();
        });
        data1Button.setOnAction(ev -> {
            prefs.data1PathProperty().set(app.runOpenFileDialog("DATA1"));
            app.loadDataFiles();
        });
        data2Button.setOnAction(ev -> {
            prefs.data2PathProperty().set(app.runOpenFileDialog("DATA2"));
            app.loadDataFiles();
        });
        Objects.requireNonNull(backRowThrown).setOnAction(ev -> {
            app.loadDataFiles();
        });
    }

    public void show() {
        this.stage.show();
        this.stage.sizeToScene();
    }

    public void hide() {
        this.stage.hide();
    }

    private Parent buildGrid() {
        final Slider volumeSlider;
        final Slider delaySlider;
        final Slider videoScaleSlider;
        final CheckBox soundEnabled;
        final CheckBox autoOpenParagraphs;
        final Label execLabel;
        final Label data1Label;
        final Label data2Label;

        final AppPreferences appPreferences = AppPreferences.getInstance();

        final GridPane grid = new GridPane();
        grid.getStyleClass().addAll("prefs-grid");
        int rowCounter = 0;

        final InputStream folderImageData = Objects.requireNonNull(getClass().getResourceAsStream("open-folder.png"));
        final Image folderImage = new Image(folderImageData, 16, 16, true, true);

        final Label dataFilesLabel = new Label("Data Files");
        dataFilesLabel.getStyleClass().add("text-header");
        grid.addRow(rowCounter++, dataFilesLabel);

        execButton.setGraphic(new ImageView(folderImage));
        execLabel = new Label("not loaded");
        execLabel.textProperty().bind(appPreferences.executablePathProperty());
        final HBox execBox = new HBox(execButton, execLabel);
        execBox.getStyleClass().add("hbox");
        grid.addRow(rowCounter++, new Label("DRAGON.COM"), execBox);

        data1Button.setGraphic(new ImageView(folderImage));
        data1Label = new Label("not loaded");
        data1Label.textProperty().bind(appPreferences.data1PathProperty());
        final HBox data1Box = new HBox(data1Button, data1Label);
        data1Box.getStyleClass().add("hbox");
        grid.addRow(rowCounter++, new Label("DATA1"), data1Box);

        data2Button.setGraphic(new ImageView(folderImage));
        data2Label = new Label("not loaded");
        data2Label.textProperty().bind(appPreferences.data2PathProperty());
        final HBox data2Box = new HBox(data2Button, data2Label);
        data2Box.getStyleClass().add("hbox");
        grid.addRow(rowCounter++, new Label("DATA2"), data2Box);

        final HBox sep1Box = new HBox();
        sep1Box.getStyleClass().add("separator");
        grid.addRow(rowCounter++, sep1Box);
        GridPane.setHgrow(sep1Box, Priority.ALWAYS);
        GridPane.setColumnSpan(sep1Box, 2);

        final Label bugFixesLabel = new Label("Bug Fixes");
        final Label restartLabel = new Label("Changes require restart");
        bugFixesLabel.getStyleClass().add("text-header");
        grid.addRow(rowCounter++, bugFixesLabel, restartLabel);

        backRowThrown = new CheckBox();
        backRowThrown.selectedProperty().bindBidirectional(appPreferences.backRowThrownProperty());
        grid.addRow(rowCounter++, new Label("Allow Thrown Weapons from\nthe back rank (5-7)"), backRowThrown);

        final HBox sep2Box = new HBox();
        sep2Box.getStyleClass().add("separator");
        grid.addRow(rowCounter++, sep2Box);
        GridPane.setHgrow(sep2Box, Priority.ALWAYS);
        GridPane.setColumnSpan(sep2Box, 2);

        soundEnabled = new CheckBox();
        soundEnabled.selectedProperty().bindBidirectional(appPreferences.soundEnabledProperty());
        grid.addRow(rowCounter++, new Label("Sound Enabled"), soundEnabled);

        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.setMinorTickCount(0);
        volumeSlider.setBlockIncrement(5);
        volumeSlider.setPrefWidth(250);
        volumeSlider.setMaxWidth(Region.USE_PREF_SIZE);
        volumeSlider.valueProperty().bindBidirectional(appPreferences.volumeProperty());
        grid.addRow(rowCounter++, new Label("Volume"), volumeSlider);

        final HBox sep3Box = new HBox();
        sep3Box.getStyleClass().add("separator");
        grid.addRow(rowCounter++, sep3Box);
        GridPane.setHgrow(sep3Box, Priority.ALWAYS);
        GridPane.setColumnSpan(sep3Box, 2);

        delaySlider = new Slider(0, 10, 5);
        delaySlider.setShowTickMarks(true);
        delaySlider.setShowTickLabels(true);
        delaySlider.setSnapToTicks(true);
        delaySlider.setMajorTickUnit(1);
        delaySlider.setMinorTickCount(1);
        delaySlider.setBlockIncrement(.5);
        delaySlider.setPrefWidth(250);
        delaySlider.setMaxWidth(Region.USE_PREF_SIZE);
        delaySlider.valueProperty().bindBidirectional(appPreferences.combatDelayProperty());
        grid.addRow(rowCounter++, new Label("Combat Delay"), delaySlider);

        videoScaleSlider = new Slider(1, 5, 3);
        videoScaleSlider.setShowTickMarks(true);
        videoScaleSlider.setShowTickLabels(true);
        videoScaleSlider.setSnapToTicks(true);
        videoScaleSlider.setMajorTickUnit(1);
        videoScaleSlider.setMinorTickCount(0);
        videoScaleSlider.setBlockIncrement(1);
        videoScaleSlider.setPrefWidth(250);
        videoScaleSlider.setMaxWidth(Region.USE_PREF_SIZE);
        videoScaleSlider.valueProperty().bindBidirectional(appPreferences.scaleProperty());
        grid.addRow(rowCounter++, new Label("Video Scale"), videoScaleSlider);

        autoOpenParagraphs = new CheckBox();
        autoOpenParagraphs.selectedProperty().bindBidirectional(appPreferences.autoOpenParagraphsProperty());
        grid.addRow(rowCounter++, new Label("Auto-open Paragraphs"), autoOpenParagraphs);

        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        final ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(col1, col2);

        return grid;
    }
}
