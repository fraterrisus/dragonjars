package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Gender;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Memory;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.awt.*;
import java.net.URL;
import java.util.Objects;

public class PartyStateDialog extends Dialog<Void> {
    private final ModifiableChunk partyData;

    public PartyStateDialog(Window parent, Memory memory) {
        super();
        super.initOwner(parent);
        super.setResultConverter(buttonType -> null);
        super.setTitle("Party State");

        this.partyData = memory.getSegment(Interpreter.PARTY_SEGMENT);

        final ButtonType dismissButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        final DialogPane root = getDialogPane();
        root.getStyleClass().add("party-dialog");
        root.getButtonTypes().add(dismissButton);
        root.setPrefHeight(300);
        root.setContent(buildGrid());
        super.setResizable(true);

        final URL cssUrl = getClass().getResource("dialog.css");
        if (cssUrl == null) {
            throw new RuntimeException("Can't load styles file");
        }
        root.getStylesheets().add(cssUrl.toExternalForm());
    }

    private Parent buildGrid() {
        final BorderPane root = new BorderPane();
        root.getStyleClass().add("party-grid");

        root.setLeft(buildListPane());
        root.setCenter(buildCharPane());
        BorderPane.setAlignment(root, Pos.TOP_LEFT);

        return root;
    }

    private Node buildListPane() {
        final ListView<PartyMember> listView = new ListView<>();
        listView.setCellFactory(PartyMember::cellFactory);

        final MultipleSelectionModel<PartyMember> selectionModel = listView.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) emptyCharPane();
            else setCharPane(nVal);
        });

        populatePartyList(listView);

        return listView;
    }

    private GridPane memberGrid;

    private Node buildCharPane() {
        memberGrid = new GridPane();
        return memberGrid;
    }

    private void emptyCharPane() {
        memberGrid.getChildren().clear();
    }

    private void setCharPane(PartyMember member) {
        memberGrid.getChildren().clear();
        int row = 0;
        memberGrid.addRow(row++, new Label(member.name()));

        final CheckBox irkalla = new CheckBox();
        irkalla.selectedProperty().set(member.blessedByIrkalla());
        irkalla.setDisable(true);
        memberGrid.addRow(row++, new Label("Blessed by Irkalla"), irkalla);
        
        final CheckBox swimmer = new CheckBox();
        swimmer.selectedProperty().set(member.swamFromPurgatory());
        swimmer.setDisable(true);
        memberGrid.addRow(row++, new Label("Swam from Purgatory"), swimmer);

        final CheckBox enkidu = new CheckBox();
        enkidu.selectedProperty().set(member.blessedByEnkidu());
        enkidu.setDisable(true);
        memberGrid.addRow(row++, new Label("Blessed by Enkidu"), enkidu);

        final CheckBox universalGod = new CheckBox();
        universalGod.selectedProperty().set(member.blessedByUniversalGod());
        universalGod.setDisable(true);
        memberGrid.addRow(row++, new Label("Blessed by the Universal God"), universalGod);
    }

    private void populatePartyList(ListView<PartyMember> listView) {
        final ObservableList<PartyMember> items = listView.getItems();
        items.clear();
        final int partySize = Heap.get(Heap.PARTY_SIZE).read();
        for (int pcid = 0; pcid < partySize; pcid++) {
            final int baseAddress = Heap.get(Heap.MARCHING_ORDER + pcid).read() << 8;
//            System.out.format("PC#%1d: [0x%04x]\n", pcid, baseAddress);
            items.add(PartyMember.parse(partyData, baseAddress));
        }
    }

    private record PartyMember(
            String name,
            int flags
    ) {
        public static PartyMember parse(Chunk data, int offset) {
            final StringBuilder nameBuffer = new StringBuilder();
            for (Byte b : data.getBytes(offset, 12)) {
                nameBuffer.append(Character.toChars(b & 0x7f));
                if ((b & 0x80) == 0) break;
            }

            final int currentStrength = data.getUnsignedByte(offset + 0x0c);
            final int maximumStrength = data.getUnsignedByte(offset + 0x0d);
            final int currentDexterity = data.getUnsignedByte(offset + 0x0e);
            final int maximumDexterity = data.getUnsignedByte(offset + 0x0f);
            final int currentIntelligence = data.getUnsignedByte(offset + 0x10);
            final int maximumIntelligence = data.getUnsignedByte(offset + 0x11);
            final int currentSpirit = data.getUnsignedByte(offset + 0x12);
            final int maximumSpirit = data.getUnsignedByte(offset + 0x13);
            final int currentHealth = data.getWord(offset + 0x14);
            final int maximumHealth = data.getWord(offset + 0x16);
            final int currentStun = data.getWord(offset + 0x18);
            final int maximumStun = data.getWord(offset + 0x1a);
            final int currentPower = data.getWord(offset + 0x1c);
            final int maximumPower = data.getWord(offset + 0x1e);

            final int skillArcaneLore = data.getUnsignedByte(offset + 0x20);
            final int skillCaveLore = data.getUnsignedByte(offset + 0x21);
            final int skillForestLore = data.getUnsignedByte(offset + 0x22);
            final int skillMountainLore = data.getUnsignedByte(offset + 0x23);
            final int skillTownLore = data.getUnsignedByte(offset + 0x24);
            final int skillBandage = data.getUnsignedByte(offset + 0x25);
            final int skillClimb = data.getUnsignedByte(offset + 0x26);
            final int skillFistfighting = data.getUnsignedByte(offset + 0x27);
            final int skillHiding = data.getUnsignedByte(offset + 0x28);
            final int skillLockpick = data.getUnsignedByte(offset + 0x29);
            final int skillPickpocket = data.getUnsignedByte(offset + 0x2a);
            final int skillSwim = data.getUnsignedByte(offset + 0x2b);
            final int skillTracker = data.getUnsignedByte(offset + 0x2c);
            final int skillBureaucracy = data.getUnsignedByte(offset + 0x2d);
            final int skillDruidMagic = data.getUnsignedByte(offset + 0x2e);
            final int skillHighMagic = data.getUnsignedByte(offset + 0x2f);
            final int skillLowMagic = data.getUnsignedByte(offset + 0x30);
            final int skillMerchant = data.getUnsignedByte(offset + 0x31);
            final int skillSunMagic = data.getUnsignedByte(offset + 0x32);
            final int skillAxes = data.getUnsignedByte(offset + 0x33);
            final int skillFlails = data.getUnsignedByte(offset + 0x34);
            final int skillMaces = data.getUnsignedByte(offset + 0x35);
            final int skillSwords = data.getUnsignedByte(offset + 0x36);
            final int skillTwoHanders = data.getUnsignedByte(offset + 0x37);
            final int skillBows = data.getUnsignedByte(offset + 0x38);
            final int skillCrossbows = data.getUnsignedByte(offset + 0x39);
            final int skillThrown = data.getUnsignedByte(offset + 0x3a);

            final int unspentAP = data.getUnsignedByte(offset + 0x3b);

            // spells

            final int status = data.getUnsignedByte(offset + 0x4c);
            final int npcID = data.getUnsignedByte(offset + 0x4d);
            final Gender gender = Gender.of(data.getUnsignedByte(offset + 0x4e)).orElseThrow();
            final int level = data.getWord(offset + 0x4f);
            final int experience = data.getQuadWord(offset + 0x51);
            final int gold = data.getQuadWord(offset + 0x55);
            final int attackValue = data.getUnsignedByte(offset + 0x59);
            final int defenseValue = data.getUnsignedByte(offset + 0x5a);
            final int armorClass = data.getUnsignedByte(offset + 0x5b);
            final int flags = data.getUnsignedByte(offset + 0x5c);

            final int summonedLifespan = data.getUnsignedByte(offset + 0x66);

            return new PartyMember(
                    nameBuffer.toString(),
                    flags
            );
        }

        public static ListCell<PartyMember> cellFactory(ListView<PartyMember> ignored) {
            final ListCell<PartyMember> cell = new ListCell<>() {
                @Override
                protected void updateItem(PartyMember item, boolean empty) {
                    super.updateItem(item, empty);
                    if (Objects.isNull(item) || empty) setText("");
                    else setText(item.name());
                }
            };
            cell.getStyleClass().add("party-list-cell");
            return cell;
        }

        public boolean blessedByIrkalla() {
            return (flags & 0x80) > 0;
        }

        public boolean swamFromPurgatory() {
            return (flags & 0x40) > 0;
        }

        public boolean blessedByEnkidu() {
            return (flags & 0x20) > 0;
        }

        public boolean blessedByUniversalGod() {
            return (flags & 0x10) > 0;
        }
    }
}
