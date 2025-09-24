package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Gender;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.exec.Interpreter;
import com.hitchhikerprod.dragonjars.exec.Memory;
import com.hitchhikerprod.dragonjars.tasks.SpellDecayTask;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        root.getStyleClass().add("party-border");
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
        memberGrid.getStyleClass().add("party-grid");

        final ScrollPane scrollPane = new ScrollPane(memberGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }

    private void emptyCharPane() {
        memberGrid.getChildren().clear();
    }

    private static final Map<String, String> TOOLTIPS = Map.of(
            "Merchant", "Easter egg: some encounters check it, but you can't buy it",
            "Poisoned", "Easter egg: nothing checks this or applies it"
    );

    private void setCharPane(PartyMember member) {
        memberGrid.getChildren().clear();
        int row = 0;

        final Label nameLabel = new Label(member.name);
        nameLabel.getStyleClass().add("party-label-name");
        memberGrid.addRow(row++, nameLabel);
        GridPane.setColumnSpan(nameLabel, 2);

        if (member.npcID() != 0) {
            memberGrid.addRow(row++, new Label("NPC ID"), new Label(String.valueOf(member.npcID)));
        }

        if (member.summonedLifespan != 0) {
            final long seconds = Math.round(60.0 * member.summonedLifespan);
            final long min = seconds / 60;
            final long sec = seconds % 60;
            final Label lifespanLabel = new Label(String.format("%dm %02ds", min, sec));
            memberGrid.addRow(row++, new Label("Lifespan"), lifespanLabel);
        }

        memberGrid.addRow(row++, new Label("Gender"), new Label(member.gender.toString()));
        memberGrid.addRow(row++, new Label("Level"), new Label(String.valueOf(member.level)));
        final int xpNeeded = Integer.min(6600, 50 * member.level * (member.level + 1));
        memberGrid.addRow(row++, new Label("Experience"), new Label(member.experience + " / " + xpNeeded));
        memberGrid.addRow(row++, new Label("Unspent AP"), new Label(String.valueOf(member.unspentAP)));
        memberGrid.addRow(row++, new Label("Gold"), new Label(String.valueOf(member.gold)));

        final Label attrsLabel = new Label("Attributes");
        attrsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, attrsLabel);

        memberGrid.addRow(row++,
                new Label("Strength"),
                new Label(member.currentStrength + " / " + member.maximumStrength));

        memberGrid.addRow(row++,
                new Label("Dexterity"),
                new Label(member.currentDexterity + " / " + member.maximumDexterity));

        memberGrid.addRow(row++,
                new Label("Intelligence"),
                new Label(member.currentIntelligence + " / " + member.maximumIntelligence));

        memberGrid.addRow(row++,
                new Label("Spirit"),
                new Label(member.currentSpirit + " / " + member.maximumSpirit));

        memberGrid.addRow(row++,
                new Label("Health"),
                new Label(member.currentHealth + " / " + member.maximumHealth));

        memberGrid.addRow(row++,
                new Label("Stun"),
                new Label(member.currentStun + " / " + member.maximumStun));

        memberGrid.addRow(row++,
                new Label("Power"),
                new Label(member.currentPower + " / " + member.maximumPower));

        memberGrid.addRow(row++, new Label("AV"), new Label(String.valueOf(member.attackValue)));
        memberGrid.addRow(row++, new Label("DV"), new Label(String.valueOf(member.defenseValue)));
        memberGrid.addRow(row++, new Label("AC"), new Label(String.valueOf(member.armorClass)));

        final Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, statusLabel);

        buildBooleanRow(row++, member.isChained(), "in Chains");
        buildBooleanRow(row++, member.isPoisoned(), "Poisoned", TOOLTIPS.get("Poisoned"));
        buildBooleanRow(row++, member.isStunned(), "Stunned");
        buildBooleanRow(row++, member.isDead(), "Dead");

        final Label flagsLabel = new Label("Flags");
        flagsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, flagsLabel);

        buildBooleanRow(row++, member.swamFromPurgatory(), "Swam from Purgatory");
        buildBooleanRow(row++, member.blessedByIrkalla(), "Irkalla's blessing");
        buildBooleanRow(row++, member.blessedByEnkidu(), "Enkidu's Druid Magic");
        buildBooleanRow(row++, member.blessedByUniversalGod(), "Universal God's +3AP");

        final Label skillsLabel = new Label("Skills");
        skillsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, skillsLabel);

        for (String skillName : PartyMember.SKILLS) {
            if (member.skills.get(skillName) == 0) continue;
            final Label skillNameNode = new Label(skillName);
            final Label skillValueNode = new Label(String.valueOf(member.skills.get(skillName)));
            if (TOOLTIPS.containsKey(skillName)) {
                final Tooltip tooltipNode = new Tooltip(TOOLTIPS.get(skillName));
                Tooltip.install(skillNameNode, tooltipNode);
                skillNameNode.getStyleClass().add("easter-egg");
            }
            memberGrid.addRow(row++, skillNameNode, skillValueNode);
        }

        final Label spellsLabel = new Label("Spells");
        spellsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, spellsLabel);

        for (String school : PartyMember.MAGIC_SCHOOLS.stream().map(PartyMember.MagicSchool::name).toList()) {
            if (!member.spells.containsKey(school)) continue;
            final List<String> spells = member.spells.get(school);
            spells.sort(String::compareTo);
            memberGrid.addRow(row++,
                    new Label(school),
                    new TextFlow(new Text(String.join(", ", spells))));
        }

        final ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(Region.USE_PREF_SIZE);
        col0.setHgrow(Priority.NEVER);
        final ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        memberGrid.getColumnConstraints().setAll(col0, col1);
    }

    private void buildBooleanRow(int row, boolean flag, String header) {
        buildBooleanRow(row, flag, header, null);
    }

    private void buildBooleanRow(int row, boolean flag, String header, String tooltip) {
        final Label valueNode = new Label("No");
        if (flag) {
            valueNode.setText("Yes");
            valueNode.getStyleClass().add("bold");
        }
        final Label headerNode = new Label(header);
        if (Objects.nonNull(tooltip)) {
            final Tooltip tooltipNode = new Tooltip(tooltip);
            Tooltip.install(headerNode, tooltipNode);
            headerNode.getStyleClass().add("easter-egg");
        }
        memberGrid.addRow(row, headerNode, valueNode);
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
            Gender gender,
            int npcID,
            double summonedLifespan,
            int level,
            int experience,
            int unspentAP,
            int gold,
            int currentStrength,
            int maximumStrength,
            int currentDexterity,
            int maximumDexterity,
            int currentIntelligence,
            int maximumIntelligence,
            int currentSpirit,
            int maximumSpirit,
            int currentHealth,
            int maximumHealth,
            int currentStun,
            int maximumStun,
            int currentPower,
            int maximumPower,
            int attackValue,
            int defenseValue,
            int armorClass,
            Map<String, Integer> skills,
            Map<String, List<String>> spells,
            int flags,
            int status
    ) {
        private record MagicSchool(String name, List<String> spells) { }

        public static final List<String> SKILLS = List.of("Arcane Lore", "Cave Lore", "Forest Lore", "Mountain Lore",
                "Town Lore", "Bandage", "Climb", "Fistfighting", "Hiding", "Lockpick", "Pickpocket", "Swim", "Tracker",
                "Bureaucracy", "Druid Magic", "High Magic", "Low Magic", "Merchant", "Sun Magic", "Axes", "Flails",
                "Maces", "Swords", "Two-Handers", "Bows", "Crossbows", "Thrown Weapons");

        // This HAS to be a list because order matters, and Map.entrySet() (etc.) aren't guaranteed ordered
        public static final List<MagicSchool> MAGIC_SCHOOLS = List.of(
                new MagicSchool("Low Magic", List.of("Mage Fire", "Disarm", "Charm", "Luck", "Lesser Heal", "Mage Light")),
                new MagicSchool("High Magic", List.of("Fire Light", "Elvar's Fire", "Poog's Vortex", "Ice Chill",
                        "Big Chill", "Dazzle", "Mystic Might", "Reveal Glamour", "Sala's Swift", "Vorn's Guard",
                        "Cowardice", "Healing", "Group Heal", "Cloak Arcane", "Sense Traps", "Air Summon",
                        "Earth Summon", "Water Summon", "Fire Summon")),
                new MagicSchool("Druid Magic", List.of("Death Curse", "Fire Blast", "Insect Plague", "Whirl Wind",
                        "Scare", "Brambles", "Greater Healing", "Cure All", "Create Wall", "Soften Stone",
                        "Invoke Spirit", "Beast Call", "Wood Spirit")),
                new MagicSchool("Sun Magic", List.of("Sun Stroke", "Exorcism", "Rage of Mithras", "Wrath of Mithras",
                        "Fire Storm", "Inferno", "Holy Aim", "Battle Power", "Column of Fire", "Mithras's Bless",
                        "Light Flash", "Armor of Light", "Sun Light", "Heal", "Major Healing", "Disarm Trap",
                        "Guidance", "Radiance", "Summon Salamander", "Charger")),
                new MagicSchool("Misc. Magic", List.of("Zak's Speek", "Kill Ray", "Prison"))
        );

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

            final Map<String, Integer> skills = new HashMap<>();
            int skillOffset = offset + 0x20;
            for (String skill : SKILLS) {
                skills.put(skill, data.getUnsignedByte(skillOffset++));
            }

            final int unspentAP = data.getUnsignedByte(offset + 0x3b);

            final Map<String, List<String>> spells = new HashMap<>();
            int spellOffset = offset + 0x3c;
            int spellBitfield = data.getUnsignedByte(spellOffset);
            int spellMask = 0x80;
            for (MagicSchool school : MAGIC_SCHOOLS) {
                final List<String> spellNames = new ArrayList<>();
                for (String spellName : school.spells()) {
                    if ((spellBitfield & spellMask) > 0) spellNames.add(spellName);
                    spellMask = spellMask >> 1;
                    if (spellMask == 0) {
                        spellOffset++;
                        spellBitfield = data.getUnsignedByte(spellOffset);
                        spellMask = 0x80;
                    }
                }
                if (!spellNames.isEmpty()) spells.put(school.name(), spellNames);
            }

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

            // See SpellDecayTask for notes
            final int summonedMinutes = data.getWord(offset + 0x66);
            final int summonedTicks = data.getWord(offset + 0x68);
            final double summonedLifespan = summonedMinutes + ((double)summonedTicks / SpellDecayTask.CYCLES_PER_POINT);

            return new PartyMember(
                    nameBuffer.toString(), gender, npcID, summonedLifespan,
                    level, experience, unspentAP, gold,
                    currentStrength, maximumStrength,
                    currentDexterity, maximumDexterity,
                    currentIntelligence, maximumIntelligence,
                    currentSpirit, maximumSpirit,
                    currentHealth, maximumHealth,
                    currentStun, maximumStun,
                    currentPower, maximumPower,
                    attackValue, defenseValue, armorClass,
                    skills, spells, flags, status
            );
        }

        public static ListCell<PartyMember> cellFactory(ListView<PartyMember> ignored) {
            final ListCell<PartyMember> cell = new ListCell<>() {
                @Override
                protected void updateItem(PartyMember member, boolean empty) {
                    super.updateItem(member, empty);
                    if (Objects.isNull(member) || empty) {
                        setText("");
                    } else if (member.npcID() != 0) {
                        setText(member.name() + " (NPC)");
                    } else if (member.summonedLifespan != 0) {
                        setText(member.name() + " (summoned)");
                    } else {
                        setText(member.name());
                    }
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

        public boolean isStunned() {
            return (status & 0x80) > 0;
        }

        public boolean isPoisoned() {
            return (status & 0x04) > 0;
        }

        public boolean isChained() {
            return (status & 0x02) > 0;
        }

        public boolean isDead() {
            return (status & 0x01) > 0;
        }
    }
}
