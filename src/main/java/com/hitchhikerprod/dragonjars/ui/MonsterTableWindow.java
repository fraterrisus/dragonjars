package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.ChunkStringBinding;
import com.hitchhikerprod.dragonjars.data.PowerInt;
import com.hitchhikerprod.dragonjars.data.StringDecoder;
import com.hitchhikerprod.dragonjars.exec.ALU;
import com.hitchhikerprod.dragonjars.exec.CombatData;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Subscription;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MonsterTableWindow {
    private static final MonsterTableWindow INSTANCE = new MonsterTableWindow();

    private final Stage stage;
    private ListView<Integer> groupList;
    private Map<Integer, GroupData> groupDataMap = Map.of();
    private List<MonsterData> monsterDataList = List.of();
    private Chunk combatDataChunk;
    private Subscription chunkListener;

    private MonsterTableWindow() {
        final Parent root = buildElements();
        final Scene scene = new Scene(root);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());

        this.stage = new Stage();
        this.stage.initModality(Modality.NONE);
        this.stage.initStyle(StageStyle.DECORATED);
        this.stage.setTitle("Monster Table");
        this.stage.setResizable(true);
        this.stage.setScene(scene);

        this.stage.setMinWidth(600);
        this.stage.setMinHeight(200);

        stage.sizeToScene();
    }

    private Parent buildElements() {
        groupList = new ListView<>();
        groupList.setCellFactory(MonsterCell::new);
        groupList.setPrefWidth(750);

        final ScrollPane parent = new ScrollPane(groupList);
        parent.getStyleClass().add("monster-window");
        parent.setFitToHeight(true);
        parent.setFitToWidth(true);
        return parent;
    }

    public static MonsterTableWindow getInstance() {
        return INSTANCE;
    }

    public void show() {
        this.stage.show();
//        DragonWarsApp.dump(this.stage.getScene().getRoot());
    }

    private class MonsterCell extends ListCell<Integer> {
        private final VBox root;
        private final Text headerText;
        private final TableView<GroupData> groupTable;
        private final TableView<MonsterData> monsterTable;

        public MonsterCell(ListView<Integer> view) {
            this.headerText = new Text();
            this.headerText.getStyleClass().add("monster-header");

            this.groupTable = new TableView<>();
            this.groupTable.setMinHeight(52);
            this.groupTable.setMaxHeight(52);
            final var groupCols = this.groupTable.getColumns();
            groupCols.addAll(GroupData.getAllColumns());

            this.monsterTable = new TableView<>();
            this.monsterTable.setMinHeight(52);
            final var monsterCols = this.monsterTable.getColumns();
            monsterCols.addAll(MonsterData.getAllColumns());

            this.root = new VBox(this.headerText, this.groupTable, this.monsterTable);
            this.root.setFillWidth(true);
            this.root.setPrefWidth(700);
        }

        @Override
        protected void updateItem(Integer pointer, boolean empty) {
            if (empty || Objects.isNull(combatDataChunk)) {
                super.updateItem(pointer, true);
                setGraphic(null);
                return;
            }

            int index = -1;
            for (int i = 0; i < 4; i++) {
                final int p = combatDataChunk.read(CombatData.GROUP_DATA_POINTERS + (2 * i), 2);
                if (pointer == p) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                super.updateItem(pointer, true);
                setGraphic(null);
                return;
            }

            final ObservableList<GroupData> groupRows = this.groupTable.getItems();
            groupRows.clear();
            final ObservableList<MonsterData> monsterRows = this.monsterTable.getItems();
            monsterRows.clear();

            final GroupData groupData = groupDataMap.get(pointer);
            if (Objects.isNull(groupData)) {
                super.updateItem(pointer, true);
                setGraphic(null);
                return;
            }
            groupRows.setAll(groupData);

            this.headerText.setText(groupData.getName());

            int count = 0;
            for (MonsterData md : monsterDataList) {
                if (md.getGroupId() != index) continue;
                if (count++ >= groupData.getSize()) break;
                monsterRows.add(md);
            }
            this.monsterTable.setPrefHeight(25 * (groupData.getSize() + 1));

            super.updateItem(pointer, false);
            setGraphic(this.root);
        }
    }

    public void unsetChunk() {
        this.groupDataMap.values().forEach(GroupData::unbind);
        this.groupDataMap = Map.of();
        this.monsterDataList.forEach(MonsterData::unbind);
        this.monsterDataList = List.of();
        if (Objects.nonNull(this.chunkListener)) {
            this.chunkListener.unsubscribe();
        }
        this.combatDataChunk = null;
    }

    public void setChunk(Chunk chunk, StringDecoder decoder) {
        if (this.combatDataChunk == chunk) return;

        // Remove previous bindings
        unsetChunk();

        // Update chunk pointer
        this.combatDataChunk = chunk;

        // Build group data from the current group mapping (data pointers)
        this.groupDataMap = IntStream.range(0, 4)
                .map(x -> combatDataChunk.read(CombatData.GROUP_DATA_POINTERS + (2 * x), 2)).boxed()
                .collect(Collectors.toMap(
                        pointer -> pointer,
                        pointer -> GroupData.bind(combatDataChunk, pointer, decoder)
                ));

        // Build monster data
        this.monsterDataList = IntStream.range(0, 0x32)
                .mapToObj(idx -> MonsterData.bind(combatDataChunk, idx))
                .toList();

        // Subscribe to chunk invalidation events
        this.invalidationListener();
        this.chunkListener = chunk.getObservable().subscribe(() -> Platform.runLater(this::invalidationListener));
    }

    private void invalidationListener() {
        final ObservableList<Integer> items = groupList.getItems();
        items.clear();
        for (int idx = 0; idx < 4; idx++) {
            final int pointer = combatDataChunk.read(CombatData.GROUP_DATA_POINTERS + (2 * idx), 2);
            final GroupData groupData = groupDataMap.get(pointer);
            // Because this is an invalidation listener, writes to two bytes will cause this to trigger twice :(
            // which means we get one run when only half the word has been written.
            if (Objects.isNull(groupData)) continue;
            if (groupData.getSize() > 0) items.add(pointer);
        }
    }

    @FunctionalInterface
    private interface CellFactory<S, T> {
        void apply(
                TableColumn<S, T> column,
                TableCell<S, T> cell,
                T item,
                boolean empty
        );
    }

    public static final class GroupData {
        private final StringProperty name = new SimpleStringProperty();
        private final IntegerProperty size = new SimpleIntegerProperty();
        private final IntegerProperty distance = new SimpleIntegerProperty();
        private final IntegerProperty dex = new SimpleIntegerProperty();
        private final IntegerProperty av = new SimpleIntegerProperty();
        private final IntegerProperty avCalc = new SimpleIntegerProperty();
        private final IntegerProperty dvCalc = new SimpleIntegerProperty();
        private final IntegerProperty avMod = new SimpleIntegerProperty();
        private final IntegerProperty dvMod = new SimpleIntegerProperty();
        private final IntegerProperty speed = new SimpleIntegerProperty();
        private final IntegerProperty undead = new SimpleIntegerProperty();
        private final IntegerProperty disarm = new SimpleIntegerProperty();
        private final IntegerProperty xp = new SimpleIntegerProperty();

        private GroupData() {}

        private static GroupData bind(Chunk chunk, int pointer, StringDecoder decoder) {
            final GroupData data = new GroupData();
            final ChunkStringBinding nameBinder = new ChunkStringBinding(chunk, decoder,
                    pointer + CombatData.GROUP_NAME, 0x13);
            data.name.bind(nameBinder);
            data.size.bind(chunk.watch(pointer + CombatData.GROUP_SIZE, 1));
            data.distance.bind(chunk.watch(pointer + CombatData.GROUP_DIST, 1));
            data.dex.bind(chunk.watch(pointer + CombatData.GROUP_DEX, 1));
            data.av.bind(chunk.watch(pointer + CombatData.GROUP_AV, 1));
            data.avCalc.bind(data.dex.divide(4).add(data.av));
            data.dvCalc.bind(data.dex.divide(4));
            data.avMod.bind(chunk.watch(pointer + CombatData.GROUP_AV_MOD, 1));
            data.dvMod.bind(chunk.watch(pointer + CombatData.GROUP_DV_MOD, 1));
            data.speed.bind(chunk.watch(pointer + CombatData.GROUP_SPEED, 1));
            data.undead.bind(chunk.watch(pointer + CombatData.GROUP_FLAGS, 1));
            data.disarm.bind(chunk.watch(pointer + CombatData.GROUP_DISARM, 1));
            data.xp.bind(chunk.watch(pointer + CombatData.GROUP_XP, 1));
            return data;
        }

        public void unbind() {
            name.unbind();
            size.unbind();
            distance.unbind();
            dex.unbind();
            av.unbind();
            avCalc.unbind();
            dvCalc.unbind();
            avMod.unbind();
            dvMod.unbind();
            speed.unbind();
            undead.unbind();
            disarm.unbind();
            xp.unbind();
        }

        public String getName() {
            return name.get();
        }

        public int getSize() {
            return size.get();
        }

        public int getDistance() {
            return distance.get();
        }

        public int getCalculatedAv() {
            return avCalc.get();
        }

        public int getCalculatedDv() {
            return dvCalc.get();
        }

        public int getAvMod() {
            return avMod.get();
        }

        public int getDvMod() {
            return dvMod.get();
        }

        public int getSpeed() {
            return speed.get();
        }

        public int getUndead() {
            return undead.get();
        }

        public int getDisarm() {
            return disarm.get();
        }

        public int getXp() {
            return xp.get();
        }

        public StringProperty nameProperty() {
            return name;
        }

        public IntegerProperty sizeProperty() {
            return size;
        }

        public IntegerProperty distanceProperty() {
            return distance;
        }

        public IntegerProperty calculatedAvProperty() {
            return avCalc;
        }

        public IntegerProperty calculatedDvProperty() {
            return dvCalc;
        }

        public IntegerProperty speedProperty() {
            return speed;
        }

        public IntegerProperty undeadProperty() {
            return undead;
        }

        public IntegerProperty disarmProperty() {
            return disarm;
        }

        public IntegerProperty xpProperty() {
            return xp;
        }

        private static final Map<String, String> TOOLTIPS = Map.of(
                "#", "Number of monsters in group",
                "Distance", "Distance from party",
                "Speed", "Distance group can close in one round",
                "AV", "Calculated from DEX and base AV.\n" +
                    "Does not include temporary modifier.",
                "DV", "Calculated from DEX.\n" +
                    "Does not include temporary modifier.",
                "+AV", "Temporary AV modifier due to spells",
                "+DV", "Temporary DV modifier due to spells"
        );

        private static <T> TableColumn<GroupData, T> getBaseColumn(
                final String header,
                final String propertyName,
                final CellFactory<GroupData, T> cellFactory
        ) {
            final TableColumn<GroupData, T> column = new TableColumn<>();
            final Label headerLabel = new Label(header);
            final String tooltip = TOOLTIPS.get(header);
            if (Objects.nonNull(tooltip)) {
                headerLabel.setTooltip(new Tooltip(tooltip));
            }
            column.setGraphic(headerLabel);
            column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
            column.setCellFactory((col) -> new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER);
                    cellFactory.apply(col, this, item, empty);
                }
            });
            return column;
        }

        private static final CellFactory<GroupData, Integer> signedInt =
                (col, cell, item, empty) -> cell.setText(empty ? "—" : String.format("%+d", ALU.signExtend(item, 1)));

        private static final CellFactory<GroupData, Integer> footMarker =
                (col, cell, item, empty) -> cell.setText(empty ? "—" : String.format("%d0'", item));

        private static <T> TableColumn<GroupData, T> getPropertyColumn(String header, String propertyName) {
            return getBaseColumn(header, propertyName,
                    (col, cell, item, empty) -> cell.setText(empty ? "—" : item.toString()));
        }

        private static TableColumn<GroupData, String> getNameColumn() {
            return getPropertyColumn("Name", "name");
        }

        private static TableColumn<GroupData, String> getSizeColumn() {
            return getPropertyColumn("#", "size");
        }

        private static TableColumn<GroupData, String> getDexColumn() {
            return getPropertyColumn("DEX", "dex");
        }

        private static TableColumn<GroupData, String> getCalculatedAvColumn() {
            return getPropertyColumn("AV", "calculatedAv");
        }

        private static TableColumn<GroupData, String> getCalculatedDvColumn() {
            return getPropertyColumn("DV", "calculatedDv");
        }

        private static TableColumn<GroupData, Integer> getAvModColumn() {
            return getBaseColumn("+AV", "avMod", signedInt);
        }

        private static TableColumn<GroupData, Integer> getDvModColumn() {
            return getBaseColumn("+DV", "dvMod", signedInt);
        }

        private static TableColumn<GroupData, Integer> getDistanceColumn() {
            return getBaseColumn("Distance", "distance", footMarker);
        }

        private static TableColumn<GroupData, Integer> getSpeedColumn() {
            return getBaseColumn("Speed", "speed", footMarker);
        }

        private static TableColumn<GroupData, Integer> getUndeadColumn() {
            return getBaseColumn("Undead", "undead",
                    (col, cell, item, empty) -> {
                        if (empty) cell.setText("—");
                        else if ((item & 0x08) > 0) cell.setText("Yes");
                        else cell.setText("No");
                    });
        }

        private static TableColumn<GroupData, Integer> getDisarmColumn() {
            return getBaseColumn("Can Disarm", "undead",
                    (col, cell, item, empty) -> {
                        if (empty) cell.setText("—");
                        else if ((item & 0x08) > 0) cell.setText("No");
                        else cell.setText("Yes");
                    });
        }

        private static TableColumn<GroupData, Integer> getXpColumn() {
            return getBaseColumn("XP", "xp",
                    (col, cell, item, empty) -> {
                        if (empty) cell.setText("—");
                        else cell.setText(new PowerInt((byte) (item & 0xff)).plus(1).toString());
                    });
        }

        public static List<TableColumn<GroupData, ?>> getAllColumns() {
            return List.of(
                    getSizeColumn(),
                    getDistanceColumn(),
                    getSpeedColumn(),
                    getCalculatedAvColumn(),
                    getCalculatedDvColumn(),
                    getAvModColumn(),
                    getDvModColumn(),
                    getUndeadColumn(),
                    getDisarmColumn(),
                    getXpColumn()
            );
        }
    }

    public static final class MonsterData {
        private final IntegerProperty hp = new SimpleIntegerProperty();
        private final IntegerProperty initiative = new SimpleIntegerProperty();
        private final IntegerProperty status = new SimpleIntegerProperty();
        private final IntegerProperty av = new SimpleIntegerProperty();
        private final IntegerProperty dv = new SimpleIntegerProperty();
        private final IntegerProperty groupId = new SimpleIntegerProperty();

        private MonsterData() {}

        private static MonsterData bind(Chunk chunk, int idx) {
            final MonsterData data = new MonsterData();
            data.hpProperty().bind(chunk.watch(CombatData.MONSTER_HP + (2 * idx), 2));
            data.initiativeProperty().bind(chunk.watch(CombatData.MONSTER_INIT + idx, 1));
            data.statusProperty().bind(chunk.watch(CombatData.MONSTER_ACTION + idx, 1));
            data.avProperty().bind(chunk.watch(CombatData.MONSTER_AV + idx, 1));
            data.dvProperty().bind(chunk.watch(CombatData.MONSTER_DV + idx, 1));
            data.groupIdProperty().bind(chunk.watch(CombatData.MONSTER_GROUP_ID + idx, 1));
            return data;
        }

        public void unbind() {
            hp.unbind();
            initiative.unbind();
            status.unbind();
            av.unbind();
            dv.unbind();
            groupId.unbind();
        }

        public IntegerProperty hpProperty() {
            return hp;
        }

        public IntegerProperty initiativeProperty() {
            return initiative;
        }

        public IntegerProperty statusProperty() {
            return status;
        }

        public IntegerProperty avProperty() {
            return av;
        }

        public IntegerProperty dvProperty() {
            return dv;
        }

        public int getGroupId() {
            return groupId.get();
        }

        public IntegerProperty groupIdProperty() {
            return groupId;
        }

        private static final Map<String, String> TOOLTIPS = Map.of(
                "HP", "Current health",
                "Init", "Current initiative roll;\n" +
                        "set to zero once creature has acted",
                "AV", "Current total AV",
                "DV", "Current total DV",
                "Targetable", "Monsters can only be attacked once until\n" +
                        "every monster in the group has been attacked.\n" +
                        "This property resets frequently.\n",
                "Blocking", "This creature has an active Block;\n" +
                        "the next melee attack will be unsuccessful.",
                "Damaged", "This creature has successfully been attacked.",
                "Attacked", "This creature has been attacked at least once.",
                "Disarmed", "This creature has no weapon and will\n" +
                        "spend its next attack retrieving it.",
                "Fleeing", "This creature will flee (100% success\n" +
                        "rate) at the next opportunity."
        );

        private static <T> TableColumn<MonsterData, T> getBaseColumn(
                final String header,
                final String propertyName,
                final CellFactory<MonsterData, T> cellFactory
        ) {
            final TableColumn<MonsterData, T> column = new TableColumn<>();
            final Label headerLabel = new Label(header);
            final String tooltip = TOOLTIPS.get(header);
            if (Objects.nonNull(tooltip)) {
                headerLabel.setTooltip(new Tooltip(tooltip));
            }
            column.setGraphic(headerLabel);
            column.setCellValueFactory(new PropertyValueFactory<>(propertyName));
            column.setCellFactory((col) -> new TableCell<>() {
                @Override
                protected void updateItem(T item, boolean empty) {
                    super.updateItem(item, empty);
                    setAlignment(Pos.CENTER);
                    cellFactory.apply(col, this, item, empty);
                }
            });
            return column;
        }

        private static final CellFactory<MonsterData, Integer> signExtension =
                (col, cell, item, empty) -> cell.setText(empty ? "—" : String.format("%+d", ALU.signExtend(item, 1)));

        private static <T> TableColumn<MonsterData, T> getPropertyColumn(String header, String propertyName) {
            return getBaseColumn(header, propertyName,
                    (col, cell, item, empty) -> cell.setText(empty ? "—" : item.toString()));
        }

        private static TableColumn<MonsterData, Number> getHpColumn() {
            return getPropertyColumn("HP", "hp");
        }

        private static TableColumn<MonsterData, Integer> getInitiativeColumn() {
            return getPropertyColumn("Init", "initiative");
        }

        private static TableColumn<MonsterData, Integer> getAVColumn() {
            return getBaseColumn("AV", "av", signExtension);
        }

        private static TableColumn<MonsterData, Integer> getDVColumn() {
            return getBaseColumn("DV", "dv", signExtension);
        }

        private static TableColumn<MonsterData, Integer> getGroupIDColumn() {
            return getPropertyColumn("Grp", "groupId");
        }

        private static TableColumn<MonsterData, Integer> getActionColumn() {
            return getBaseColumn("Status", "status",
                    (col, cell, item, empty) -> {
                        if (empty) cell.setText("—");
                        else cell.setText(String.format("%-8s", Integer.toBinaryString(item)).replace(" ", "0"));
                    });
        }

        private static TableColumn<MonsterData, Integer> getStatusColumn(String header, int field) {
            return getStatusColumn(header, field, false);
        }

        private static TableColumn<MonsterData, Integer> getStatusColumn(String header, int field, boolean flip) {
            return getBaseColumn(header, "status",
                    (col, cell, item, empty) -> {
                    if (empty) cell.setText("—");
                    else cell.setText((flip) ^ ((item & (0x1 << field)) > 0) ? "Yes" : "No");
                });
        }

        public static List<TableColumn<MonsterData, ?>> getAllColumns() {
            return List.of(
                    getHpColumn(),
                    getInitiativeColumn(),
                    getAVColumn(),
                    getDVColumn(),
                    getStatusColumn("Targetable", 7, true),
                    getStatusColumn("Blocking", 6),
                    getStatusColumn("Damaged", 5),
                    getStatusColumn("Attacked", 4),
                    getStatusColumn("Disarmed", 2),
                    getStatusColumn("Fleeing", 1)
            );
        }
    }
}
