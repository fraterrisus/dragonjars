package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.DragonWarsApp;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MenuBar {
    private static final MenuBar INSTANCE = new MenuBar();

    public static MenuBar getInstance() { return INSTANCE; }

    private final javafx.scene.control.MenuBar menuBar;

    private final Map<String, MenuItem> items;

    private MenuBar() {
        items = new HashMap<>();
        menuBar = new javafx.scene.control.MenuBar(makeFileMenu(), makeHelpMenu());
        VBox.setVgrow(menuBar, Priority.NEVER);
    }

    public void setStylesheets(URL cssUrl) {
        menuBar.getStylesheets().add(cssUrl.toExternalForm());
    }

    public void start(DragonWarsApp app) {
        activateFileMenu(app);
        activateHelpMenu(app);
    }

    private Menu makeFileMenu() {
        final Menu fileMenu = new Menu("File");

        final MenuItem prefsMI = new MenuItem("Preferences");
        items.put("file.prefs", prefsMI);

        final MenuItem quitMI = new MenuItem("Quit");
        items.put("file.quit", quitMI);

        fileMenu.getItems().setAll(prefsMI, new SeparatorMenuItem(), quitMI);
        return fileMenu;
    }

    private void activateFileMenu(final DragonWarsApp app) {
        items.get("file.prefs").setOnAction(ev -> app.openPreferencesDialog());
        items.get("file.quit").setOnAction(ev -> app.close());
    }

    private Menu makeHelpMenu() {
        final Menu helpMenu = new Menu("Help");

        final MenuItem paraMI = new MenuItem("Paragraphs");
        items.put("help.paragraphs", paraMI);

        final MenuItem spellsMI = new MenuItem("Spells");
        items.put("help.spells", spellsMI);

        final MenuItem mapMI = new MenuItem("Map");
        items.put("help.map", mapMI);

        final MenuItem partyMI = new MenuItem("Party State");
        items.put("help.party", partyMI);

        final MenuItem stateMI = new MenuItem("Game State");
        items.put("help.state", stateMI);

        final MenuItem aboutMI = new MenuItem("About DragonJars");
        items.put("help.about", aboutMI);

        helpMenu.getItems().setAll(paraMI, spellsMI,
                new SeparatorMenuItem(), mapMI, partyMI, stateMI,
                new SeparatorMenuItem(), aboutMI);
        return helpMenu;
    }

    private void activateHelpMenu(DragonWarsApp app) {
        items.get("help.about").setOnAction(ev -> app.openAboutDialog());
        items.get("help.map").setOnAction(ev -> app.openMapWindow());
        items.get("help.paragraphs").setOnAction(ev -> app.openParagraphsWindow());
        items.get("help.party").setOnAction(ev -> app.openPartyStateDialog());
        items.get("help.state").setOnAction(ev -> app.openGameStateWindow());
        items.get("help.spells").setOnAction(ev -> app.openSpellsWindow());
    }

    public Node asNode() { return menuBar; }
}
