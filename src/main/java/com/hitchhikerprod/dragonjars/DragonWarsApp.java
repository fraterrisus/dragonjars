package com.hitchhikerprod.dragonjars;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkImageDecoder;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.tasks.LoadDataTask;
import com.hitchhikerprod.dragonjars.ui.LoadingWindow;
import com.hitchhikerprod.dragonjars.ui.RootWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DragonWarsApp extends Application {
    private static final double SCALE_FACTOR = 3.0;

    private Stage stage;
    private Scene scene;

    private List<Chunk> dataChunks;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        final URL cssUrl = getClass().getResource("style.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }

        final RootWindow root = RootWindow.getInstance();
        root.start(this);
        root.setStyleSheets(cssUrl);

        this.scene = new Scene(root.asParent());

        this.stage.setTitle("DragonJars");
        this.stage.setScene(scene);
        this.stage.setResizable(false);
        this.stage.show();

        loadDataFiles();
    }

    public static void main(String[] args) {
        launch();
    }

    public void setKeyHandler(EventHandler<KeyEvent> handler) {
        this.scene.setOnKeyReleased(handler);
    }

    public void exit() {
        Platform.exit();
    }

    private void loadDataFiles() {
        final LoadDataTask task = new LoadDataTask();

        final StringProperty label = LoadingWindow.getInstance().getLabel().textProperty();
        final DoubleProperty progress = LoadingWindow.getInstance().getProgressBar().progressProperty();
        label.bind(task.messageProperty());
        progress.bind(task.progressProperty());

        task.setOnSucceeded(event -> {
            label.unbind();
            label.setValue("Complete.");
            progress.unbind();
            progress.setValue(1.0);
            try {
                this.dataChunks = task.get();
                showTitleScreen();
            } catch (InterruptedException | ExecutionException e) {
                label.setValue("Failed.");
                dataChunks = null;
            }
        });

        task.setOnFailed(event -> {
            label.unbind();
            label.setValue("Failed.");
            progress.unbind();
            progress.setValue(0.0);
            final Alert alert = new Alert(Alert.AlertType.ERROR, task.getException().getMessage());
            alert.showAndWait();
            Platform.exit();
        });

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showTitleScreen() {
        this.stage.setWidth(320 * SCALE_FACTOR);
        this.stage.setHeight(200 * SCALE_FACTOR);
        final Chunk titleScreenChunk = dataChunks.get(ChunkTable.TITLE_SCREEN);
        final Image titleScreenImage = new ChunkImageDecoder(titleScreenChunk).parse();
        RootWindow.getInstance().setImage(titleScreenImage, SCALE_FACTOR);
        setKeyHandler(this::titleScreenHandler);
    }

    private void startInterpreter() {
        setKeyHandler(null);
        final Interpreter interp = new Interpreter(this.dataChunks, 0, 0);
        interp.start();
    }

    private void titleScreenHandler(KeyEvent event) {
        System.out.println(event.getCode().name());
        switch (event.getCode()) {
            case Q -> Platform.exit();
            case ENTER, ESCAPE, SPACE -> startInterpreter();
        }
    }
}