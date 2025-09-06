package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MenuBar {
    private static final MenuBar INSTANCE = new MenuBar();

    public static MenuBar getInstance() { return INSTANCE; }

    private final javafx.scene.control.MenuBar menuBar;

    private final Map<String, MenuItem> items;
    private final ToggleGroup videoScaleGroup;
    private final IntegerProperty videoScale;
    private final Slider volumeSlider;

    private MenuBar() {
        items = new HashMap<>();

        volumeSlider = new Slider(0, 100, 50);
        videoScaleGroup = new ToggleGroup();
        videoScaleGroup.selectedToggleProperty().addListener((obs, oVal, nVal) -> updateScaleProperty(nVal));
        videoScale = new SimpleIntegerProperty();
        videoScale.addListener((obs, oVal, nVal) -> updateScaleButtons(nVal));

        final Menu fileM = makeFileMenu();
        final Menu videoM = makeVideoMenu();
        final Menu audioM = makeAudioMenu();

        menuBar = new javafx.scene.control.MenuBar(fileM, videoM, audioM);
        VBox.setVgrow(menuBar, Priority.NEVER);
    }

    public void setStylesheets(URL cssUrl) {
        menuBar.getStylesheets().add(cssUrl.toExternalForm());
    }

    public void start(DragonWarsApp app) {
        videoScale.set(3);
        activateFileMenu(app);
    }

    private Menu makeFileMenu() {
        final Menu fileMenu = new Menu("File");

        final MenuItem quitMI = new MenuItem("Quit");
        items.put("file.quit", quitMI);

        fileMenu.getItems().setAll(quitMI);
        return fileMenu;
    }

    private void activateFileMenu(DragonWarsApp app) {
        items.get("file.quit").setOnAction(ev -> Platform.exit());
    }

    private Menu makeVideoMenu() {
        final Menu videoMenu = new Menu("Video");

        final Menu scaleMenu = new Menu("Scale");
        for (int i = 1; i <= 4; i++) {
            final RadioMenuItem scaleMI = new RadioMenuItem(i + "x");
            scaleMI.getStyleClass().add("dark");
            scaleMI.setUserData(i);
            scaleMI.setToggleGroup(videoScaleGroup);
            scaleMenu.getItems().add(scaleMI);
        }

        videoMenu.getItems().setAll(scaleMenu);
        return videoMenu;
    }

    private Menu makeAudioMenu() {
        final Menu audioMenu = new Menu("Audio");

        final Menu volumeMenu = new Menu("Volume");

        volumeSlider.setShowTickMarks(true);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.setBlockIncrement(5);
        final CustomMenuItem volumeMI = new CustomMenuItem(volumeSlider);
        volumeMI.setHideOnClick(false);

        volumeMenu.getItems().setAll(volumeMI);

        audioMenu.getItems().setAll(volumeMenu);
        return audioMenu;
    }

    private void updateScaleProperty(Toggle nVal) {
        if (Objects.nonNull(nVal) && nVal.getUserData() instanceof Integer newScale) {
            // try to avoid an infinite loop of binding
            if (videoScale.get() != newScale)
                videoScale.set(newScale);
        }
    }

    private void updateScaleButtons(Number nVal) {
        final int value = nVal.intValue();
        videoScaleGroup.getToggles().stream()
                .filter(t -> t.getUserData() instanceof Integer val && val == value)
                .findFirst()
                .ifPresent(videoScaleGroup::selectToggle);
    }

    public IntegerProperty videoScaleProperty() {
        return videoScale;
    }

    public DoubleProperty volumeProperty() {
        return volumeSlider.valueProperty();
    }

    public Node asNode() { return menuBar; }
}
