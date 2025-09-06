package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

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
    private final Slider delaySlider;

    private MenuBar() {
        items = new HashMap<>();

        volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setShowTickMarks(true);
        volumeSlider.setShowTickLabels(true);
        volumeSlider.setMajorTickUnit(25);
        volumeSlider.setMinorTickCount(0);
        volumeSlider.setBlockIncrement(5);

        delaySlider = new Slider(0, 10, 5);
        delaySlider.setShowTickMarks(true);
        delaySlider.setShowTickLabels(true);
        delaySlider.setSnapToTicks(true);
        delaySlider.setMajorTickUnit(1);
        delaySlider.setMinorTickCount(1);
        delaySlider.setBlockIncrement(.5);

        videoScaleGroup = new ToggleGroup();
        videoScaleGroup.selectedToggleProperty().addListener((obs, oVal, nVal) -> updateScaleProperty(nVal));
        videoScale = new SimpleIntegerProperty();
        videoScale.addListener((obs, oVal, nVal) -> updateScaleButtons(nVal));

        menuBar = new javafx.scene.control.MenuBar(
                makeFileMenu(),
                makeSettingsMenu(),
                makeHelpMenu()
        );
        VBox.setVgrow(menuBar, Priority.NEVER);
    }

    public void setStylesheets(URL cssUrl) {
        menuBar.getStylesheets().add(cssUrl.toExternalForm());
    }

    public void start(DragonWarsApp app) {
        videoScale.set(3);
        activateFileMenu(app);
        activateHelpMenu(app);
    }

    private Menu makeFileMenu() {
        final Menu fileMenu = new Menu("File");

        final Menu execSubMenu = new Menu("Executable");
        final Circle execLight = new Circle(8);
        execLight.setFill(Color.GREY);
        execSubMenu.setGraphic(execLight);
        final MenuItem execMI = new MenuItem();
        items.put("file.exec", execMI);
        execSubMenu.getItems().setAll(execMI);

        final Menu data1SubMenu = new Menu("Data 1");
        final Circle data1Light = new Circle(8);
        data1Light.setFill(Color.GREY);
        data1SubMenu.setGraphic(data1Light);
        final MenuItem data1MI = new MenuItem();
        items.put("file.data1", data1MI);
        data1SubMenu.getItems().setAll(data1MI);

        final Menu data2SubMenu = new Menu("Data 2");
        final Circle data2Light = new Circle(8);
        data2Light.setFill(Color.GREY);
        data2SubMenu.setGraphic(data2Light);
        final MenuItem data2MI = new MenuItem();
        items.put("file.data2", data2MI);
        data2SubMenu.getItems().setAll(data2MI);

        final MenuItem quitMI = new MenuItem("Quit");
        items.put("file.quit", quitMI);

        fileMenu.getItems().setAll(execSubMenu, data1SubMenu, data2SubMenu,
                new SeparatorMenuItem(), quitMI);
        return fileMenu;
    }

    private void activateFileMenu(final DragonWarsApp app) {
        final AppPreferences prefs = AppPreferences.getInstance();

        items.get("file.quit").setOnAction(ev -> app.close());

        final StringProperty executablePathProperty = prefs.executablePathProperty();
        manageDiskFile("exec", executablePathProperty.get());
        executablePathProperty.addListener((obs, oVal, nVal) -> manageDiskFile("exec", nVal));
        items.get("file.exec").setOnAction(ev -> {
            executablePathProperty.set(app.runOpenFileDialog("DRAGON.COM"));
            app.loadDataFiles();
        });

        final StringProperty data1PathProperty = prefs.data1PathProperty();
        manageDiskFile("data1", data1PathProperty.get());
        data1PathProperty.addListener((obs, oVal, nVal) -> manageDiskFile("data1", nVal));
        items.get("file.data1").setOnAction(ev -> {
            data1PathProperty.set(app.runOpenFileDialog("DATA1"));
            app.loadDataFiles();
        });

        final StringProperty data2PathProperty = prefs.data2PathProperty();
        manageDiskFile("data2", data2PathProperty.get());
        data2PathProperty.addListener((obs, oVal, nVal) -> manageDiskFile("data2", nVal));
        items.get("file.data2").setOnAction(ev -> {
            data2PathProperty.set(app.runOpenFileDialog("DATA2"));
            app.loadDataFiles();
        });
    }

    private void manageDiskFile(String menuItem, String path) {
        final MenuItem fileMI = items.get("file." + menuItem);
        final Menu fileMenu = fileMI.getParentMenu();
        fileMI.getStyleClass().removeAll("label-italic", "label-roman");
        if (Objects.isNull(path)) {
            fileMI.setText("not set");
            fileMI.getStyleClass().add("label-italic");
            if (fileMenu.getGraphic() instanceof Circle c) c.setFill(Color.RED);
        } else {
            fileMI.setText(path);
            fileMI.getStyleClass().add("label-roman");
            if (fileMenu.getGraphic() instanceof Circle c) c.setFill(Color.GREEN);
        }
    }

    private Menu makeSettingsMenu() {
        final Menu settings = new Menu("Settings");

        final Menu volumeMenu = new Menu("Volume");
        final CustomMenuItem volumeMI = new CustomMenuItem(volumeSlider);
        volumeMI.setHideOnClick(false);
        volumeMenu.getItems().setAll(volumeMI);

        final Menu scaleMenu = new Menu("Video Scale");
        for (int i = 1; i <= 4; i++) {
            final RadioMenuItem scaleMI = new RadioMenuItem(i + "x");
            scaleMI.getStyleClass().add("dark");
            scaleMI.setUserData(i);
            scaleMI.setToggleGroup(videoScaleGroup);
            scaleMenu.getItems().add(scaleMI);
        }

        final Menu delayMenu = new Menu("Combat Delay");
        final CustomMenuItem delayMI = new CustomMenuItem(delaySlider);
        delaySlider.setPrefWidth(300);
        delayMI.setHideOnClick(false);
        delayMenu.getItems().add(delayMI);

        settings.getItems().setAll(volumeMenu, scaleMenu, delayMenu);
        return settings;
    }

    private Menu makeHelpMenu() {
        final Menu helpMenu = new Menu("Help");

        final MenuItem aboutMI = new MenuItem("About");
        items.put("help.about", aboutMI);

        helpMenu.getItems().setAll(aboutMI);
        return helpMenu;
    }

    private void activateHelpMenu(DragonWarsApp app) {
        items.get("help.about").setOnAction(ev -> {
            new AboutDialog(app.getStage()).showAndWait();
        });
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

    public DoubleProperty combatDelayProperty() { return delaySlider.valueProperty(); }

    public IntegerProperty videoScaleProperty() {
        return videoScale;
    }

    public DoubleProperty volumeProperty() {
        return volumeSlider.valueProperty();
    }

    public Node asNode() { return menuBar; }
}
