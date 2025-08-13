package com.hitchhikerprod.dragonjars;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkImageDecoder;
import com.hitchhikerprod.dragonjars.data.ChunkTable;
import com.hitchhikerprod.dragonjars.data.Images;
import com.hitchhikerprod.dragonjars.data.RomImageDecoder;
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
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
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

    /**
     * Draws an 8x8 bitmask on the screen. The bitmask should be an array of bytes, one byte per vertical line.
     * @param bitmask A `byte[8]`, each byte (8 bits) corresponding to 8 pixels in the X dimension
     * @param x Screen coordinate (in pixels) of the left-hand side
     * @param y Screen coordinate (in pixels) of the top side
     * @param invert Draw black-on-white if false, or white-on-black if true.
     */
    public void drawBitmask(byte[] bitmask, int x, int y, boolean invert) {
        final Image image = RootWindow.getInstance().getImage();
        final int black = Images.convertColorIndex(0);
        final int white = Images.convertColorIndex(15);
        if (image instanceof WritableImage wimage) {
            final PixelWriter writer = wimage.getPixelWriter();
            for (int dy = 0; dy < 8; dy++) {
                final int b = bitmask[dy];
                final int mask = 0x80;
                for (int dx = 0; dx < 8; dx++) {
                    final boolean draw = (b & (mask >> dx)) > 0;
                    writer.setArgb(x + dx, y + dy, (draw ^ invert) ? black : white);
                }
            }
        } else {
            throw new RuntimeException("Can't write the image");
        }
    }

    public void drawRectangle(int color, int x0, int y0, int x1, int y1) {
        final Image image = RootWindow.getInstance().getImage();
        final int colorValue = Images.convertColorIndex(color);
        if (image instanceof WritableImage wimage) {
            final PixelWriter writer = wimage.getPixelWriter();
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    writer.setArgb(x, y, colorValue);
                }
            }
        } else {
            throw new RuntimeException("Can't write the image");
        }
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
        final Chunk titleScreenChunk = dataChunks.get(ChunkTable.TITLE_SCREEN);
        final Image titleScreenImage = new ChunkImageDecoder(titleScreenChunk).parse();
        RootWindow.getInstance().setImage(titleScreenImage, SCALE_FACTOR);
        this.stage.sizeToScene();
        setKeyHandler(this::titleScreenHandler);
    }

    private void startInterpreter() {
        setKeyHandler(null);
        RootWindow.getInstance().setImage(Images.blankImage(320, 200), SCALE_FACTOR);
        this.stage.sizeToScene();
        final Interpreter interp = new Interpreter(this, this.dataChunks, 0, 0);
        interp.start();
        Platform.exit();
    }

    private void testPattern() {
        setKeyHandler(null);
        RootWindow.getInstance().setImage(Images.blankImage(320, 200), SCALE_FACTOR);
        this.stage.sizeToScene();
        final Interpreter interp = new Interpreter(this, this.dataChunks, 0, 0);

        interp.drawString("Test Pattern", 14, 0, true);
        interp.drawString("Press Q to exit", 13, 24, true);

        for (int x = 0; x < 16; x++) {
            final int fx = (x + 2) * 16;
            final int ch = (x < 10) ? 0x30 + x : 0x37 + x;
            interp.drawChar(ch, fx, 18, true);
        }
        for (int y = 0; y < 2; y++) {
            final int fy = (y + 2) * 16;
            interp.drawChar(0x30 + y, 12, fy, true);
            interp.drawChar('x', 20, fy, true);
            for (int x = 0; x < 16; x++) {
                final int fx = (x + 2) * 16;
                final int ch = (16 * y) + x;
                interp.drawChar(ch, fx, fy, false);
            }
        }

        for (int x = 0; x < 16; x++) {
            final int fx = (x + 4) * 8;
            final int ch = (x < 10) ? 0x30 + x : 0x37 + x;
            interp.drawChar(ch, fx, 68, true);
        }
        for (int y = 2; y < 8; y++) {
            final int fy = (y + 8) * 8;
            interp.drawChar(0x30 + y, 12, fy, true);
            interp.drawChar('x', 20, fy, true);
            for (int x = 0; x < 16; x++) {
                final int fx = (x + 4) * 8;
                final int ch = (16 * y) + x;
                interp.drawChar(ch, fx, fy, false);
            }
        }

        // Test case: drawModal(0x16,0x00,0x28,0x98) is the combat dialog
        // which is 16 characters wide plus border
        // interp.drawModal(8 * 0x16, 0x00, 8 * 0x28, 0x98);
        // interp.drawString("You still face 4", 0x17, 0x01, false);

        setKeyHandler(event -> {
            if (event.getCode() == KeyCode.Q || event.getCode() == KeyCode.ESCAPE) {
                Platform.exit();
            }
        });
    }

    private void titleScreenHandler(KeyEvent event) {
        System.out.println(event.getCode().name());
        switch (event.getCode()) {
            case Q -> Platform.exit();
            case T -> testPattern();
            case ENTER, ESCAPE, SPACE -> startInterpreter();
        }
    }

}