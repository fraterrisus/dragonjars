package com.hitchhikerprod.dragonjars;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.VideoHelper;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.VideoBuffer;
import com.hitchhikerprod.dragonjars.tasks.LoadDataTask;
import com.hitchhikerprod.dragonjars.ui.AppPreferences;
import com.hitchhikerprod.dragonjars.ui.LoadingWindow;
import com.hitchhikerprod.dragonjars.ui.MenuBar;
import com.hitchhikerprod.dragonjars.ui.MusicService;
import com.hitchhikerprod.dragonjars.ui.RootWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class DragonWarsApp extends Application {
    public static final int IMAGE_X = 320;
    public static final int IMAGE_Y = 200;

    private Stage stage;
    private Scene scene;

    private MusicService musicService;

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
        root.setStylesheets(cssUrl);

        this.scene = new Scene(root.asParent());

        this.stage.setTitle("DragonJars");
        this.stage.setScene(scene);
        this.stage.setResizable(false);
        this.stage.show();

        final MenuBar menuBar = MenuBar.getInstance();

        this.musicService = new MusicService();
        this.musicService.volumeProperty().bind(menuBar.volumeProperty());

        final AppPreferences prefs = AppPreferences.getInstance();
        menuBar.volumeProperty().bindBidirectional(prefs.volumeProperty());
        menuBar.videoScaleProperty().bindBidirectional(prefs.scaleProperty());
        menuBar.combatDelayProperty().bindBidirectional(prefs.combatDelayProperty());

        loadDataFiles();
    }

    public static void main(String[] args) {
        launch();
    }

    public void close() {
        musicService.close();
        Platform.exit();
    }

    public int getScaleFactor() {
        return MenuBar.getInstance().videoScaleProperty().get();
    }

    public Stage getStage() {
        return stage;
    }

    public void loadDataFiles() {
        final AppPreferences prefs = AppPreferences.getInstance();
        final String executablePath = prefs.executablePathProperty().get();
        final String data1Path = prefs.data1PathProperty().get();
        final String data2Path = prefs.data2PathProperty().get();

        if (Objects.isNull(executablePath) || Objects.isNull(data1Path) || Objects.isNull(data2Path)) return;

        musicService.stop();
        RootWindow.getInstance().setLoading();
        final LoadDataTask task = new LoadDataTask(executablePath, data1Path, data2Path);

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
            task.getException().printStackTrace();
            prefs.executablePathProperty().set(null);
            prefs.data1PathProperty().set(null);
            prefs.data2PathProperty().set(null);
            final Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to load data files. Your settings have been cleared.");
            alert.showAndWait();
        });

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public MusicService musicService() {
        return musicService;
    }

    public String runOpenFileDialog(String header) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open " + header);
        final File selected = fileChooser.showOpenDialog(this.stage);
        return (Objects.isNull(selected)) ? null : selected.getAbsolutePath();
    }

    public void setImage(Image image) {
        // TODO I don't get why this doesn't result in actual integer scaling.
        final int scale = AppPreferences.getInstance().scaleProperty().get();
        RootWindow.getInstance().setImage(image, scale);
        resize();
    }

    public void resize() {
        this.stage.sizeToScene();
    }

    public void setKeyHandler(EventHandler<KeyEvent> handler) {
        this.scene.setOnKeyPressed(handler);
    }

    private void showTitleScreen() {
        final VideoHelper draw = new VideoHelper(dataChunks.getLast());
        final VideoBuffer vb = new VideoBuffer();
        draw.setVideoBuffer(vb);
        final Chunk rawChunk = dataChunks.get(ChunkTable.TITLE_SCREEN);
        draw.chunkImage(rawChunk);
        final WritableImage titleScreenImage = Images.blankImage(IMAGE_X, IMAGE_Y);
        vb.writeTo(titleScreenImage.getPixelWriter(), VideoBuffer.WHOLE_IMAGE, false);
        setImage(titleScreenImage);
        setKeyHandler(this::titleScreenHandler);
        musicService.enable();
        musicService.playTitleMusic(dataChunks.getLast());
    }

    private void startInterpreter() {
        setImage(Images.blankImage(IMAGE_X, IMAGE_Y));
        new Interpreter(this, this.dataChunks).init().reenter(0, 0, () -> { close(); return null; });
    }

    private void stringHelper(VideoHelper draw, String s, int x, int y, boolean invert) {
        int fx = x * 8;
        int fy = y * 8;
        for (char ch : s.toCharArray()) {
            draw.character(ch, fx, fy, invert);
            fx += 8;
        }
    }

    private void testPattern() {
        setKeyHandler(null);
        final WritableImage wimage = Images.blankImage(IMAGE_X, IMAGE_Y);
        setImage(wimage);

        final VideoHelper draw = new VideoHelper(this.dataChunks.getLast());
        final VideoBuffer vb = new VideoBuffer(VideoBuffer.CHROMA_KEY);
        draw.setVideoBuffer(vb);

        stringHelper(draw, "Test Pattern", 14, 0, true);
        stringHelper(draw, "Press Q to exit", 13, 24, true);

        for (int x = 0; x < 16; x++) {
            final int fx = (x + 2) * 16;
            final int ch = (x < 10) ? 0x30 + x : 0x37 + x;
            draw.character(ch, fx, 18, true);
        }
        for (int y = 0; y < 2; y++) {
            final int fy = (y + 2) * 16;
            draw.character(0x30 + y, 12, fy, true);
            draw.character('x', 20, fy, true);
            for (int x = 0; x < 16; x++) {
                final int fx = (x + 2) * 16;
                final int ch = (16 * y) + x;
                draw.character(ch, fx, fy, false);
            }
        }

        for (int x = 0; x < 16; x++) {
            final int fx = (x + 4) * 8;
            final int ch = (x < 10) ? 0x30 + x : 0x37 + x;
            draw.character(ch, fx, 68, true);
        }
        for (int y = 2; y < 8; y++) {
            final int fy = (y + 8) * 8;
            draw.character(0x30 + y, 12, fy, true);
            draw.character('x', 20, fy, true);
            for (int x = 0; x < 16; x++) {
                final int fx = (x + 4) * 8;
                final int ch = (16 * y) + x;
                draw.character(ch, fx, fy, false);
            }
        }

        vb.writeTo(wimage.getPixelWriter(), VideoBuffer.WHOLE_IMAGE, true);

        setKeyHandler(event -> {
            if (event.getCode() == KeyCode.Q || event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });
    }

    private void titleScreenHandler(KeyEvent event) {
        if (event.getCode().isModifierKey()) return;
        switch (event.getCode()) {
            case S -> {
                if (event.isControlDown()) {
                    if (musicService.isEnabled()) {
                        musicService.disable();
                    } else {
                        musicService.enable();
                        musicService.playTitleMusic(dataChunks.getLast());
                    }
                } else {
                    musicService.stop();
                    startInterpreter();
                }
            }
            case Q -> close();
            case T -> {
                musicService.stop();
                testPattern();
            }
            default -> {
                musicService.stop();
                startInterpreter();
            }
        }
    }
}