package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.GridCoordinate;
import com.hitchhikerprod.dragonjars.data.MapData;
import com.hitchhikerprod.dragonjars.exec.Heap;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MapWindow {
    private static final MapWindow INSTANCE = new MapWindow();

    public static MapWindow getInstance() {
        return INSTANCE;
    }

    private static final String AVATAR_RESOURCE = "avatar.png";

    private final Stage stage;
    private final Scene scene;

    private final java.awt.Image avatarIcon;

    private final ImageView imageView;

    private MapWindow() {
        imageView = new ImageView();
        imageView.setImage(new WritableImage(100,100));
        final ScrollPane root = new ScrollPane(imageView);
        this.scene = new Scene(root);

        try {
            avatarIcon = ImageIO.read(Objects.requireNonNull(getClass().getResource(AVATAR_RESOURCE)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
        root.getStyleClass().add("paragraphs-window");

        this.stage = new Stage();
        this.stage.initModality(Modality.NONE);
        this.stage.initStyle(StageStyle.DECORATED);
        this.stage.setTitle("Map");
        this.stage.setResizable(true);
        this.stage.setScene(scene);
    }

    public void show() {
        this.stage.show();
    }

    public void setTitle(String title) {
        stage.setTitle(title);
    }

    public void setMap(MapData mapData) {
        imageView.setImage(buildMap(mapData));
        this.stage.sizeToScene();
    }

    private static final int GRID_SCALE = 3;
    private static final int GRID_SIZE = GRID_SCALE * 10;

    private static final Color FENCE = new Color(133, 63, 36);
    private static final Color FIRE = new Color(196, 75, 0);
    private static final Color GRID = new Color(148, 186, 207);
    private static final Color OCEAN = new Color(89, 137, 155);
    private static final Color RED_FLOOR = new Color(255, 234, 234);
    private static final Color ROCK = new Color(153, 153, 153);
    private static final Color STATUE = new Color(104, 52, 104);
    private static final Color STONE_FLOOR = new Color(225, 225, 225);
    private static final Color TREE = new Color(36, 110, 36);
    private static final Color WALL = Color.DARK_GRAY;
    private static final Color WATER = new Color(0x80, 0xa0, 0xff);
    private static final Color FOG = new Color(0, 0, 0, 51);

    private int xMax;
    private int yMax;

    private Image buildMap(MapData mapData) {
        xMax = mapData.getMaxX();
        yMax = mapData.getMaxY();
        final int xDim = GRID_SIZE * (xMax + 2);
        final int yDim = GRID_SIZE * (yMax + 2);

        final BufferedImage image = new BufferedImage(xDim, yDim, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gfx = image.createGraphics();
        gfx.getTransform().setToIdentity();

        gfx.setFont(new Font("Arial", Font.PLAIN, 3 * GRID_SCALE));

        gfx.setColor(Color.WHITE);
        gfx.fill(new Rectangle(0, 0, xDim, yDim));

        drawMarkers(gfx);
        drawGrid(gfx);
        drawFloors(gfx, mapData);
        drawWalls(gfx, mapData);
        drawFog(gfx, mapData);
        drawAvatar(gfx, mapData);

        final WritableImage map = new WritableImage(xDim, yDim);
        final PixelWriter out = map.getPixelWriter();
        for (int y = 0; y < yDim; y++) {
            for (int x = 0; x < xDim; x++) {
                out.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return map;
    }

    private void drawMarkers(Graphics2D gfx) {
        gfx.setColor(GRID);
        int stringX, stringY;
        stringX = (GRID_SIZE / 2) - GRID_SCALE;
        stringY = 3 * (GRID_SIZE + GRID_SCALE) / 2;
        for (int y = yMax - 1; y >= 0; y--) {
            gfx.drawString(String.valueOf(y), (y > 9) ? stringX - (GRID_SCALE) : stringX, stringY);
            stringY += GRID_SIZE;
        }
        for (int x = 0; x < xMax; x++) {
            stringX += GRID_SIZE;
            gfx.drawString(String.valueOf(x), (x > 9) ? stringX - (GRID_SCALE) : stringX, stringY);
        }
    }

    private void drawGrid(Graphics2D gfx) {
        gfx.setColor(GRID);
        for (int y = 0; y < yMax + 1; y++) {
            gfx.fill(new Rectangle(GRID_SIZE - 1, ((y + 1) * GRID_SIZE) - 1, (xMax * GRID_SIZE) + 2, 3));
        }
        for (int x = 0; x < xMax + 1; x++) {
            gfx.fill(new Rectangle(((x + 1) * GRID_SIZE) - 1, GRID_SIZE - 1, 3, (yMax * GRID_SIZE) + 2));
        }
    }

    private void drawWalls(Graphics2D gfx, MapData mapData) {
        for (int y = 0; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                final MapData.Square sq = mapData.getSquare(x, y);
                final Point topLeft = new Point(
                        ((x + 1) * GRID_SIZE) - 1,
                        ((yMax - y) * GRID_SIZE) - 1
                );
                final Rectangle northDoor = new Rectangle(
                        ((x + 1) * GRID_SIZE) + (GRID_SIZE / 2) - (2 * GRID_SCALE),
                        ((yMax - y) * GRID_SIZE) - GRID_SCALE,
                        4 * GRID_SCALE,
                        2 * GRID_SCALE
                );
                final Rectangle westDoor = new Rectangle(
                        ((x + 1) * GRID_SIZE) - GRID_SCALE,
                        ((yMax - y) * GRID_SIZE) + (GRID_SIZE / 2) - (2 * GRID_SCALE),
                        2 * GRID_SCALE,
                        4 * GRID_SCALE
                );
                sq.northWallTextureChunk().ifPresent(texture -> {
                    final Rectangle r = new Rectangle(topLeft, new Dimension(GRID_SIZE + 3, 3));
                    drawWall(gfx, r, texture);
                    sq.northWallTextureMetadata().ifPresent(metadata -> drawDoor(gfx, northDoor, texture, metadata));
                });
                sq.westWallTextureChunk().ifPresent(texture -> {
                    final Rectangle r = new Rectangle(topLeft, new Dimension(3, GRID_SIZE + 3));
                    drawWall(gfx, r, texture);
                    sq.westWallTextureMetadata().ifPresent(metadata -> drawDoor(gfx, westDoor, texture, metadata));
                });
            }
        }

        if (mapData.isWrapping()) {
            for (int y = 0; y < yMax; y++) {
                final MapData.Square sq = mapData.getSquare(0, y);
                final Point topLeft = new Point(
                        ((xMax + 1) * GRID_SIZE) - 1,
                        ((yMax - y) * GRID_SIZE) - 1
                );
                final Rectangle westDoor = new Rectangle(
                        ((xMax + 1) * GRID_SIZE) - GRID_SCALE,
                        ((yMax - y) * GRID_SIZE) + (GRID_SIZE / 2) - (2 * GRID_SCALE),
                        2 * GRID_SCALE,
                        4 * GRID_SCALE
                );
                sq.westWallTextureChunk().ifPresent(texture -> {
                    final Rectangle r = new Rectangle(topLeft, new Dimension(3, GRID_SIZE + 3));
                    drawWall(gfx, r, texture);
                    sq.westWallTextureMetadata().ifPresent(metadata -> drawDoor(gfx, westDoor, texture, metadata));
                });
            }

            for (int x = 0; x < xMax; x++) {
                final MapData.Square sq = mapData.getSquare(x, 0);
                final Point topLeft = new Point(
                        ((x + 1) * GRID_SIZE) - 1,
                        (yMax * GRID_SIZE) - 1
                );
                final Rectangle northDoor = new Rectangle(
                        ((x + 1) * GRID_SIZE) + (GRID_SIZE / 2) - (2 * GRID_SCALE),
                        (yMax * GRID_SIZE) - GRID_SCALE,
                        4 * GRID_SCALE,
                        2 * GRID_SCALE
                );
                sq.northWallTextureChunk().ifPresent(texture -> {
                    final Rectangle r = new Rectangle(topLeft, new Dimension(GRID_SIZE + 3, 3));
                    drawWall(gfx, r, texture);
                    sq.northWallTextureMetadata().ifPresent(metadata -> drawDoor(gfx, northDoor, texture, metadata));
                });
            }
        }
    }

    // 0x7a fence, 0x73 door
    private void drawWall(Graphics2D gfx, Rectangle wall, int texture) {
        switch (texture) {
            case 0x6e, 0x73, 0x7d, 0x7e, 0x7f -> gfx.setColor(WALL);
            case 0x7a -> gfx.setColor(FENCE);
            default -> gfx.setColor(GRID);
        }
        gfx.fill(wall);
    }

    private void drawDoor(Graphics2D gfx, Rectangle door, int texture, int metadata) {
        if ((metadata & 0x40) == 0) {
            if (texture == 0x7a) return;
            if (texture == 0x73) gfx.setColor(Color.WHITE);
            else gfx.setColor(Color.LIGHT_GRAY);
            gfx.fill(door);
            gfx.setColor(WALL);
            gfx.draw(door);
        }
    }

    private void drawFloors(Graphics2D gfx, MapData mapData) {
        for (int y = 0; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                final MapData.Square sq = mapData.getSquare(x, y);

                final Point topLeft = new Point(((x + 1) * GRID_SIZE) + 1, ((yMax - y) * GRID_SIZE) + 1);
                final Point middle = new Point(
                        ((x + 1) * GRID_SIZE) + (GRID_SIZE / 2),
                        ((yMax - y) * GRID_SIZE) + (GRID_SIZE / 2)
                );

                final Dimension floorDim = new Dimension(GRID_SIZE - 2, GRID_SIZE - 2);
                final Rectangle floor = new Rectangle(topLeft, floorDim);
                final Point circleStart = new Point(middle.x - (2 * GRID_SCALE), middle.y - (2 * GRID_SCALE));
                final Dimension circleDim = new Dimension(4 * GRID_SCALE, 4 * GRID_SCALE);
                final Ellipse2D.Float circle = new Ellipse2D.Float(circleStart.x, circleStart.y, circleDim.width, circleDim.height);
                final Ellipse2D.Float cluster1 = new Ellipse2D.Float(
                        circleStart.x - GRID_SCALE,
                        circleStart.y - GRID_SCALE,
                        circleDim.width,
                        circleDim.height
                );
                final Ellipse2D.Float cluster2 = new Ellipse2D.Float(
                        circleStart.x + GRID_SCALE,
                        circleStart.y,
                        circleDim.width,
                        circleDim.height
                );
                final Ellipse2D.Float cluster3 = new Ellipse2D.Float(
                        circleStart.x,
                        circleStart.y + GRID_SCALE,
                        circleDim.width,
                        circleDim.height
                );

                switch (sq.floorTextureChunk()) {
                    case 0x70 -> gfx.setColor(RED_FLOOR); // red floor
                    case 0x75 -> gfx.setColor(OCEAN); // water
                    case 0x7c -> gfx.setColor(STONE_FLOOR); // stone floor
                    default -> gfx.setColor(Color.WHITE);
                }

                gfx.fill(floor);
                if (sq.otherTextureChunk().isPresent()) {
                    switch (sq.otherTextureChunk().get()) {
                        case 0x71 -> { // tree
                            gfx.setColor(TREE);
                            gfx.fill(cluster1);
                            gfx.fill(cluster2);
                            gfx.fill(cluster3);
                        }
                        case 0x72 -> { // rock
                            gfx.setColor(ROCK);
                            gfx.fill(circle);
                        }
                        case 0x74 -> { // puddle
                            gfx.setColor(WATER);
                            gfx.fill(circle);
                        }
                        case 0x77 -> { // pile of rubble
                            gfx.setColor(ROCK);
                            gfx.fill(cluster1);
                            gfx.fill(cluster2);
                            gfx.fill(cluster3);
                        }
                        case 0x78 -> { // bush
                            gfx.setColor(TREE);
                            gfx.fill(circle);
                        }
                        case 0x79 -> {
                            gfx.setColor(FIRE);
                            gfx.fill(circle);
                        }
                        case 0x7f, 0x81 -> { // statue
                            gfx.setColor(STATUE);
                            gfx.fill(circle);
                        }
                    }
                }
            }
        }
    }

    private void drawFog(Graphics2D gfx, MapData mapData) {
        gfx.setColor(FOG);
        for (int y = 0; y < yMax; y++) {
            for (int x = 0; x < xMax; x++) {
                final MapData.Square sq = mapData.getSquare(x, y);

                final Point topLeft = new Point(((x + 1) * GRID_SIZE) + 1, ((yMax - y) * GRID_SIZE) + 1);
                final Dimension floorDim = new Dimension(GRID_SIZE - 2, GRID_SIZE - 2);
                final Rectangle floor = new Rectangle(topLeft, floorDim);

                if (!sq.touched()) gfx.fill(floor);
            }
        }
    }

    private void drawAvatar(Graphics2D gfx, MapData mapData) {
        final GridCoordinate pos = Heap.getPartyLocation().pos();
        final int x, y;
        if (mapData.isWrapping()) {
            final GridCoordinate temp = pos.modulus(mapData.getMaxX(), mapData.getMaxY());
            x = temp.x();
            y = temp.y();
        } else {
            if (pos.isOutside(mapData.getMaxX(), mapData.getMaxY())) return;
            x = pos.x();
            y = pos.y();
        }
        final AffineTransform translation = AffineTransform.getTranslateInstance(
                ((x + 1) * GRID_SIZE),
                ((yMax - y) * GRID_SIZE)
        );
        gfx.drawImage(avatarIcon, translation, null);
    }
}
