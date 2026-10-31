package com.hitchhikerprod.dragonjars.ui;

import com.hitchhikerprod.dragonjars.data.Chunk;
import com.hitchhikerprod.dragonjars.data.Gender;
import com.hitchhikerprod.dragonjars.data.Item;
import com.hitchhikerprod.dragonjars.data.Lists;
import com.hitchhikerprod.dragonjars.data.ModifiableChunk;
import com.hitchhikerprod.dragonjars.exec.Heap;
import com.hitchhikerprod.dragonjars.tasks.SpellDecayTask;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PartyStateDialog extends Dialog<Void> {
    private final ModifiableChunk partyData;
    private ListView<PartyMember> partyList;
    private final ButtonType saveType;
    private final ButtonType editType;

    public PartyStateDialog(Window parent, ModifiableChunk partyData) {
        super();
        super.initOwner(parent);
        super.setResultConverter(buttonType -> null);
        super.setTitle("Party State");

        this.partyData = partyData;

        final DialogPane root = getDialogPane();
        root.getStyleClass().add("party-dialog");
        saveType = new ButtonType("Save", ButtonBar.ButtonData.LEFT);
        editType = new ButtonType("Edit", ButtonBar.ButtonData.LEFT);
        root.getButtonTypes().addAll(saveType, editType, ButtonType.CLOSE);

        // Clicking "save" should not close the dialog but should run the validate-and-save handler
        final Button save = (Button)root.lookupButton(saveType);
        save.setDisable(true);
        save.addEventFilter(ActionEvent.ACTION, ev -> {
            this.saveFormData();
            ev.consume();
        });

        final Button edit = (Button)root.lookupButton(editType);
        edit.setDisable(true);
        edit.addEventFilter(ActionEvent.ACTION, ev -> {
            setCharPaneToEdit(partyList.getSelectionModel().getSelectedItem());
            ev.consume();
        });

        final Button close = (Button)root.lookupButton(ButtonType.CLOSE);
        close.setDefaultButton(true);

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
        this.partyList = new ListView<>();
        this.partyList.setCellFactory(PartyMember::cellFactory);
        this.partyList.setPrefWidth(200);

        final MultipleSelectionModel<PartyMember> selectionModel = this.partyList.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener((obs, oVal, nVal) -> {
            if (Objects.isNull(nVal)) emptyCharPane();
            else setCharPaneToView(nVal);
        });

        populatePartyList(this.partyList);

        return this.partyList;
    }

    private ScrollPane memberScroll;
    private GridPane memberGrid;

    private Node buildCharPane() {
        memberGrid = new GridPane();
        memberGrid.getStyleClass().add("party-grid");

        memberScroll = new ScrollPane(memberGrid);
        memberScroll.setFitToWidth(true);
        memberScroll.setFitToHeight(true);
        return memberScroll;
    }

    private void emptyCharPane() {
        memberGrid.getChildren().clear();
        getDialogPane().lookupButton(saveType).setDisable(true);
        getDialogPane().lookupButton(editType).setDisable(true);
    }

    private static final Map<String, String> TOOLTIPS = Map.of(
            "Heal", "Valar comes with it, but otherwise unlearnable",
            "Light Flash", "Easter egg: not found in the game, but should work",
            "Merchant", "Easter egg: some encounters check it, but you can't buy it",
            "Poisoned", "Easter egg: nothing checks this or applies it",
            "Prison", "Easter egg: not found in the game, but should work",
            "Wrath of Mithras", "Monsters can cast at you, but otherwise unlearnable"
    );

    private record AttributeBuilder(
            String label,
            Predicate<PartyMember> showIf,
            Function<PartyMember, Node> displayNode,
            Function<PartyMember, Node> editorNode,
            BiConsumer<PartyMember.Builder, Node> updateMember
    ) {
        public Label getLabel() {
            final Label rowLabel = new Label(label());
            final String tooltip = TOOLTIPS.get(label());
            if (Objects.nonNull(tooltip)) {
                final Tooltip tooltipNode = new Tooltip(tooltip);
                Tooltip.install(rowLabel, tooltipNode);
                rowLabel.getStyleClass().add("easter-egg");
            }
            return rowLabel;
        }
    }

    private static TextField textFieldOfWidth(String text, int maxWidth) {
        final TextField tf = new TextField(text);
        tf.setMaxWidth(maxWidth);
        return tf;
    }

    private static AttributeBuilder simpleAttributeBuilderBuilder(
            String label,
            Function<PartyMember, Integer> getValue,
            BiConsumer<PartyMember.Builder, Integer> setValue
    ) {
        return new AttributeBuilder(
                label,
                null,
                (pm) -> new Label(String.valueOf(getValue.apply(pm))),
                (pm) -> textFieldOfWidth(String.valueOf(getValue.apply(pm)), 75),
                (b, node) -> setValue.accept(b, Integer.parseInt(((TextField) node).getText()))
        );
    }

    private static AttributeBuilder pairedAttributeBuilderBuilder(
            String label,
            Function<PartyMember, Integer> getCurrentValue,
            Function<PartyMember, Integer> getMaximumValue,
            BiConsumer<PartyMember.Builder, Integer> setCurrentValue,
            BiConsumer<PartyMember.Builder, Integer> setMaximumValue
    ) {
        return new AttributeBuilder(
                label,
                null,
                (pm) -> new Label(getCurrentValue.apply(pm) + " / " + getMaximumValue.apply(pm)),
                (pm) -> new HBox(
                        textFieldOfWidth(String.valueOf(getCurrentValue.apply(pm)), 75),
                        new Label(" / "),
                        textFieldOfWidth(String.valueOf(getMaximumValue.apply(pm)), 75)
                ),
                (b, node) -> {
                    final ObservableList<Node> kids = ((HBox)node).getChildren();
                    setCurrentValue.accept(b, Integer.parseInt(((TextField)kids.get(0)).getText()));
                    setMaximumValue.accept(b, Integer.parseInt(((TextField)kids.get(2)).getText()));
                }
        );
    }

    private static AttributeBuilder booleanAttributeBuilderBuilder(
            String label,
            Function<PartyMember, Boolean> getValue,
            BiConsumer<PartyMember.Builder, Boolean> setValue
    ) {
        return new AttributeBuilder(
                label,
                null,
                (pm) -> {
                    final Label node = new Label("No");
                    if (getValue.apply(pm)) {
                        node.setText("Yes");
                        node.getStyleClass().add("bold");
                    }
                    return node;
                },
                (pm) -> {
                    final CheckBox node = new CheckBox();
                    node.setSelected(getValue.apply(pm));
                    return node;
                },
                (b, node) -> setValue.accept(b, ((CheckBox)node).isSelected())
        );
    }

    private static final List<AttributeBuilder> DEMOGRAPHICS = List.of(
            new AttributeBuilder(
                    "NPC ID",
                    (pm) -> pm.npcID() != 0,
                    (pm) -> new Label(String.valueOf(pm.npcID)),
                    null,
                    null
            ),
            new AttributeBuilder(
                    "Lifespan",
                    (pm) -> pm.summonedLifespan() != 0,
                    (pm) -> {
                        final long seconds = Math.round(60.0 * pm.summonedLifespan());
                        final long min = seconds / 60;
                        final long sec = seconds % 60;
                        return new Label(String.format("%dm %02ds", min, sec));
                    },
                    null,
                    null
            ),
            new AttributeBuilder(
                    "Gender",
                    null,
                    (pm) -> new Label(pm.gender().toString()),
                    (pm) -> {
                        final MenuButton mb = new MenuButton(pm.gender().toString());
                        final List<MenuItem> genders = Arrays.stream(Gender.values())
                                .map(Gender::toString).map(MenuItem::new).toList();
                        genders.forEach(g -> g.setOnAction(ev -> mb.setText(g.getText())));
                        mb.getItems().addAll(genders);
                        return mb;
                    },
                    (b, node) -> b.gender(Gender.valueOf(((MenuButton)node).getText()))
            ),
            simpleAttributeBuilderBuilder("Level", PartyMember::level, PartyMember.Builder::level),
            new AttributeBuilder(
                    "Experience",
                    null,
                    (pm) -> {
                        final int xpNeeded = Integer.min(6600, 50 * pm.level() * (pm.level() + 1));
                        return new Label(pm.experience() + " / " + xpNeeded);
                    },
                    (pm) -> textFieldOfWidth(String.valueOf(pm.experience()), 75),
                    (b, node) -> b.experience(Integer.parseInt(((TextField)node).getText()))
            ),
            simpleAttributeBuilderBuilder("Unspent AP", PartyMember::unspentAP, PartyMember.Builder::unspentAP),
            simpleAttributeBuilderBuilder("Gold", PartyMember::gold, PartyMember.Builder::gold)
    );

    private static final List<AttributeBuilder> ATTRIBUTES = List.of(
            pairedAttributeBuilderBuilder(
                    "Strength", PartyMember::currentStrength, PartyMember::maximumStrength,
                    PartyMember.Builder::currentStrength, PartyMember.Builder::maximumStrength
            ),
            pairedAttributeBuilderBuilder(
                    "Dexterity", PartyMember::currentDexterity, PartyMember::maximumDexterity,
                    PartyMember.Builder::currentDexterity, PartyMember.Builder::maximumDexterity
            ),
            pairedAttributeBuilderBuilder(
                    "Intelligence", PartyMember::currentIntelligence, PartyMember::maximumIntelligence,
                    PartyMember.Builder::currentIntelligence, PartyMember.Builder::maximumIntelligence
            ),
            pairedAttributeBuilderBuilder(
                    "Spirit", PartyMember::currentSpirit, PartyMember::maximumSpirit,
                    PartyMember.Builder::currentSpirit, PartyMember.Builder::maximumSpirit
            ),
            pairedAttributeBuilderBuilder(
                    "Health", PartyMember::currentHealth, PartyMember::maximumHealth,
                    PartyMember.Builder::currentHealth, PartyMember.Builder::maximumHealth
            ),
            pairedAttributeBuilderBuilder(
                    "Stun", PartyMember::currentStun, PartyMember::maximumStun,
                    PartyMember.Builder::currentStun, PartyMember.Builder::maximumStun
            ),
            pairedAttributeBuilderBuilder(
                    "Power", PartyMember::currentPower, PartyMember::maximumPower,
                    PartyMember.Builder::currentPower, PartyMember.Builder::maximumPower
            ),
            new AttributeBuilder("AV", null, (pm) -> new Label(String.valueOf(pm.attackValue())), null, null),
            new AttributeBuilder("DV", null, (pm) -> new Label(String.valueOf(pm.defenseValue())), null, null),
            new AttributeBuilder("AC", null, (pm) -> new Label(String.valueOf(pm.armorClass())), null, null)
    );

    private static final List<AttributeBuilder> STATUSES = List.of(
            booleanAttributeBuilderBuilder("in Chains", PartyMember::isChained, PartyMember.Builder::isChained),
            booleanAttributeBuilderBuilder("Poisoned", PartyMember::isPoisoned, PartyMember.Builder::isPoisoned),
            booleanAttributeBuilderBuilder("Stunned", PartyMember::isStunned, PartyMember.Builder::isStunned),
            booleanAttributeBuilderBuilder("Dead", PartyMember::isDead, PartyMember.Builder::isDead)
    );

    private static final List<AttributeBuilder> FLAGS = List.of(
        booleanAttributeBuilderBuilder("Swam from Purgatory",
                PartyMember::swamFromPurgatory, PartyMember.Builder::swamFromPurgatory),
        booleanAttributeBuilderBuilder("Irkalla's blessing",
                PartyMember::blessedByIrkalla, PartyMember.Builder::blessedByIrkalla),
        booleanAttributeBuilderBuilder("Enkidu's Druid Magic",
                PartyMember::blessedByEnkidu, PartyMember.Builder::blessedByEnkidu),
        booleanAttributeBuilderBuilder("Universal God's blessing",
                PartyMember::blessedByUniversalGod, PartyMember.Builder::blessedByUniversalGod)
    );

    private static final List<AttributeBuilder> SKILLS = Stream.of(Lists.SKILL_NAMES)
            .map(name -> simpleAttributeBuilderBuilder(
                    name,
                    (pm) -> pm.skills.get(name),
                    (b, val) -> b.setSkill(name, val)))
            .toList();

    private static final Map<String, AttributeBuilder> SPELLS = PartyMember.MAGIC_SCHOOLS.stream()
            .flatMap(school -> school.spells().stream().map(spell -> new Pair<>(school, spell)))
            .collect(Collectors.toMap(
                    pair -> pair.getValue(),
                    pair -> booleanAttributeBuilderBuilder(
                            pair.getValue(),
                            (pm) -> pm.hasSpell(pair.getKey().name(), pair.getValue()),
                            (b, val) -> b.setSpell(pair.getKey().name(), pair.getValue(), val)
                    ))
            );

    private void setCharPaneToView(PartyMember member) {
        memberGrid.getChildren().clear();
        int row = 0;

        final BiFunction<Integer, List<AttributeBuilder>, Integer> rowBuilder = (rowId, builders) -> {
            int rowNum = rowId;
            for (AttributeBuilder att : builders) {
                if (Objects.nonNull(att.showIf()) && !att.showIf().test(member)) continue;
                memberGrid.addRow(rowNum++, att.getLabel(), att.displayNode().apply(member));
            }
            return rowNum;
        };

        final Label nameLabel = new Label(member.name);
        nameLabel.getStyleClass().add("party-label-name");
        memberGrid.addRow(row++, nameLabel);
        GridPane.setColumnSpan(nameLabel, 2);

        row = rowBuilder.apply(row, DEMOGRAPHICS);

        final Label attrsLabel = new Label("Attributes");
        attrsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, attrsLabel);

        row = rowBuilder.apply(row, ATTRIBUTES);

        final Label inventoryLabel = new Label("Inventory");
        inventoryLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, inventoryLabel);

        for (Item item : member.inventory) {
            // FIXME: You can do better than toString() here.
            final TextFlow itemLabel = new TextFlow(new Text(item.toString()));
            memberGrid.addRow(row++, itemLabel);
            GridPane.setColumnSpan(itemLabel, 2);
        }

        final Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, statusLabel);

        row = rowBuilder.apply(row, STATUSES);

        final Label flagsLabel = new Label("Flags");
        flagsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, flagsLabel);

        row = rowBuilder.apply(row, FLAGS);

        final Label skillsLabel = new Label("Skills");
        skillsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, skillsLabel);

        for (AttributeBuilder att : SKILLS) {
            if (Objects.nonNull(att.showIf()) && !att.showIf().test(member)) continue;
            final Label displayLabel = (Label) att.displayNode().apply(member);
            if (displayLabel.getText().equals("0")) continue;
            memberGrid.addRow(row++, att.getLabel(), displayLabel);
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

        getDialogPane().lookupButton(saveType).setDisable(true);
        getDialogPane().lookupButton(editType).setDisable(false);
    }

    private void setCharPaneToEdit(PartyMember member) {
        memberGrid.getChildren().clear();
        int row = 0;

        final Label nameLabel = new Label(member.name);
        nameLabel.getStyleClass().add("party-label-name");
        memberGrid.addRow(row++, nameLabel);
        GridPane.setColumnSpan(nameLabel, 2);

        final BiFunction<Integer, List<AttributeBuilder>, Integer> rowBuilder = (rowId, builders) -> {
            int rowNum = rowId;
            for (AttributeBuilder att : builders) {
                if (Objects.nonNull(att.showIf()) && !att.showIf().test(member)) continue;
                Function<PartyMember, Node> editor = att.editorNode();
                if (Objects.isNull(editor)) editor = att.displayNode();
                memberGrid.addRow(rowNum++, att.getLabel(), editor.apply(member));
            }
            return rowNum;
        };

        row = rowBuilder.apply(row, DEMOGRAPHICS);

        final Label attrsLabel = new Label("Attributes");
        attrsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, attrsLabel);

        row = rowBuilder.apply(row, ATTRIBUTES);

        final Label statusLabel = new Label("Status");
        statusLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, statusLabel);

        row = rowBuilder.apply(row, STATUSES);

        final Label flagsLabel = new Label("Flags");
        flagsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, flagsLabel);

        row = rowBuilder.apply(row, FLAGS);

        final Label skillsLabel = new Label("Skills");
        skillsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, skillsLabel);

        row = rowBuilder.apply(row, SKILLS);

        final Label spellsLabel = new Label("Spells");
        spellsLabel.getStyleClass().add("party-label-section-header");
        memberGrid.addRow(row++, spellsLabel);

        for (PartyMember.MagicSchool school : PartyMember.MAGIC_SCHOOLS) {
            final Label schoolLabel = new Label(school.name());
            schoolLabel.getStyleClass().add("bold");
            memberGrid.addRow(row++, schoolLabel);

            for (String spell : school.spells()) {
                final AttributeBuilder att = SPELLS.get(spell);
                if (Objects.nonNull(att.showIf()) && !att.showIf().test(member)) continue;
                Function<PartyMember, Node> editor = att.editorNode();
                if (Objects.isNull(editor)) editor = att.displayNode();
                memberGrid.addRow(row++, att.getLabel(), editor.apply(member));
            }
        }

        getDialogPane().lookupButton(saveType).setDisable(false);
        getDialogPane().lookupButton(editType).setDisable(true);
    }

    private static Node getNodeFromGrid(GridPane grid, int row, int col) {
        return grid.getChildrenUnmodifiable().stream()
                .filter(n -> GridPane.getColumnIndex(n) == col)
                .filter(n -> GridPane.getRowIndex(n) == row)
                .findFirst().orElse(null);
    }

    private void saveFormData() {
        final int index = partyList.getSelectionModel().getSelectedIndex();
        final ObservableList<PartyMember> members = partyList.getItems();
        final PartyMember selected = members.get(index);
        if (Objects.isNull(selected)) return;
        
        int errorNodeId = -1;

        final PartyMember.Builder builder = selected.thaw();

        int rowId = 0;

        for (List<AttributeBuilder> builders : List.of(DEMOGRAPHICS, ATTRIBUTES, STATUSES, FLAGS, SKILLS)) {
            rowId++;
            for (AttributeBuilder att : builders) {
                if (Objects.nonNull(att.showIf()) && !att.showIf().test(selected)) continue;
                if (Objects.nonNull(att.editorNode())) {
                    final Node valueNode = getNodeFromGrid(memberGrid, rowId, 1);
                    if (Objects.isNull(valueNode)) continue;
                    try {
                        att.updateMember().accept(builder, valueNode);
                        valueNode.getStyleClass().remove("validation-error");
                    } catch (RuntimeException e) {
                        if (errorNodeId == -1) errorNodeId = rowId;
                        valueNode.getStyleClass().add("validation-error");
                    }
                }
                rowId++;
            }
        }

        rowId++; // spell header
        for (var school : PartyMember.MAGIC_SCHOOLS) {
            rowId++; // school header
            for (String spellName : school.spells()) {
                final AttributeBuilder att = SPELLS.get(spellName);
                final Node valueNode = getNodeFromGrid(memberGrid, rowId, 1);
                try {
                    att.updateMember().accept(builder, valueNode);
                    valueNode.getStyleClass().remove("validation-error");
                } catch (RuntimeException e) {
                    if (errorNodeId == -1) errorNodeId = rowId;
                    valueNode.getStyleClass().add("validation-error");
                }
                rowId++;
            }
        }

        if (errorNodeId > 0) {
            memberScroll.setVvalue(errorNodeId * (memberScroll.getVmax() - memberScroll.getVmin()) / rowId);
            return;
        }

        final PartyMember replacement = builder.freeze();
        replacement.save(this.partyData);
        Heap.get(Heap.PC_DIRTY + index).write(0);
        partyList.getItems().set(index, replacement);

        setCharPaneToView(replacement);
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
            int baseAddress,
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
            List<Item> inventory,
            int flags,
            int status
    ) {
        private record MagicSchool(String name, List<String> spells) { }

        public static final int MASK_IRKALLA = 0x80;
        public static final int MASK_SWIM_EXIT = 0x40;
        public static final int MASK_ENKIDU = 0x20;
        public static final int MASK_UNIVERSAL = 0x10;

        public static final int MASK_STUN = 0x80;
        public static final int MASK_POISON = 0x04;
        public static final int MASK_CHAINS = 0x02;
        public static final int MASK_DEAD = 0x01;

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
                new MagicSchool("Misc. Magic", List.of("Zak's Speed", "Kill Ray", "Prison"))
        );

        public boolean hasSpell(String schoolName, String spellName) {
            return spells().getOrDefault(schoolName, List.of()).contains(spellName);
        }

        public static PartyMember parse(Chunk data, int offset) {
            final Builder builder = new Builder()
                    .baseAddress(offset);
            
            final StringBuilder nameBuffer = new StringBuilder();
            for (Byte b : data.getBytes(offset, 12)) {
                nameBuffer.append(Character.toChars(b & 0x7f));
                if ((b & 0x80) == 0) break;
            }
            builder.name(nameBuffer.toString())
                    .currentStrength(data.getUnsignedByte(offset + 0x0c))
                    .maximumStrength(data.getUnsignedByte(offset + 0x0d))
                    .currentDexterity(data.getUnsignedByte(offset + 0x0e))
                    .maximumDexterity(data.getUnsignedByte(offset + 0x0f))
                    .currentIntelligence(data.getUnsignedByte(offset + 0x10))
                    .maximumIntelligence(data.getUnsignedByte(offset + 0x11))
                    .currentSpirit(data.getUnsignedByte(offset + 0x12))
                    .maximumSpirit(data.getUnsignedByte(offset + 0x13))
                    .currentHealth(data.getWord(offset + 0x14))
                    .maximumHealth(data.getWord(offset + 0x16))
                    .currentStun(data.getWord(offset + 0x18))
                    .maximumStun(data.getWord(offset + 0x1a))
                    .currentPower(data.getWord(offset + 0x1c))
                    .maximumPower(data.getWord(offset + 0x1e));

            final Map<String, Integer> skills = new HashMap<>();
            int skillOffset = offset + 0x20;
            for (String skill : Lists.SKILL_NAMES) {
                skills.put(skill, data.getUnsignedByte(skillOffset++));
            }
            builder.skills(skills);

            builder.unspentAP(data.getUnsignedByte(offset + 0x3b));

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
            builder.spells(spells);
            
            builder.status(data.getUnsignedByte(offset + 0x4c))
                    .npcID(data.getUnsignedByte(offset + 0x4d))
                    .gender(Gender.of(data.getUnsignedByte(offset + 0x4e)).orElseThrow())
                    .level(data.getWord(offset + 0x4f))
                    .experience(data.getQuadWord(offset + 0x51))
                    .gold(data.getQuadWord(offset + 0x55))
                    .attackValue(data.getUnsignedByte(offset + 0x59))
                    .defenseValue(data.getUnsignedByte(offset + 0x5a))
                    .armorClass(data.getUnsignedByte(offset + 0x5b))
                    .flags(data.getUnsignedByte(offset + 0x5c));

            // See SpellDecayTask for notes
            final int summonedMinutes = data.getWord(offset + 0x66);
            final int summonedTicks = data.getWord(offset + 0x68);
            final double summonedLifespan = summonedMinutes + ((double)summonedTicks / SpellDecayTask.CYCLES_PER_POINT);
            builder.summonedLifespan(summonedLifespan);

            final List<Item> inventory = new ArrayList<>();
            int itemOffset = offset + 0xec;
            while (inventory.size() < 12) {
                final int firstCh = data.getUnsignedByte(itemOffset + 0x0b);
                if (firstCh == 0) break;

                final Item newItem = new Item(data);
                newItem.decode(itemOffset);
                inventory.add(newItem);
                itemOffset += 0x17;
            }
            builder.inventory(inventory);

            return builder.freeze();
        }

        public void save(ModifiableChunk data) {
            data.write(baseAddress + 0x0c, 1, currentStrength());
            data.write(baseAddress + 0x0d, 1, maximumStrength());
            data.write(baseAddress + 0x0e, 1, currentDexterity());
            data.write(baseAddress + 0x0f, 1, maximumDexterity());
            data.write(baseAddress + 0x10, 1, currentIntelligence());
            data.write(baseAddress + 0x11, 1, maximumIntelligence());
            data.write(baseAddress + 0x12, 1, currentSpirit());
            data.write(baseAddress + 0x13, 1, maximumSpirit());
            data.write(baseAddress + 0x14, 2, currentHealth());
            data.write(baseAddress + 0x16, 2, maximumHealth());
            data.write(baseAddress + 0x18, 2, currentStun());
            data.write(baseAddress + 0x1a, 2, maximumStun());
            data.write(baseAddress + 0x1c, 2, currentPower());
            data.write(baseAddress + 0x1e, 2, maximumPower());

            int skillOffset = baseAddress + 0x20;
            for (String skill : Lists.SKILL_NAMES) {
                data.write(skillOffset++, 1, skills.get(skill));
            }

            data.write(baseAddress + 0x3b, 1, unspentAP());

            int spellMask = 0x80;
            int spellValue = 0;
            List<Byte> spellBytes = new ArrayList<>();
            for (MagicSchool school : MAGIC_SCHOOLS) {
                final List<String> knownSpellNames = spells().getOrDefault(school.name(), List.of());
                for (String spellName : school.spells()) {
                    if (knownSpellNames.contains(spellName)) {
                        spellValue = spellValue | spellMask;
                    }
                    spellMask = spellMask >> 1;
                    if (spellMask == 0) {
                        spellMask = 0x80;
                        spellBytes.add((byte)spellValue);
                        spellValue = 0;
                    }
                }
            }
            if (spellValue != 0) spellBytes.add((byte)spellValue);
            data.setBytes(baseAddress + 0x3c, spellBytes);

            data.write(baseAddress + 0x4c, 1, status());
            data.write(baseAddress + 0x4e, 1, gender().getIndex());
            data.write(baseAddress + 0x4f, 1, level());
            data.write(baseAddress + 0x51, 4, experience());
            data.write(baseAddress + 0x55, 4, gold());
            data.write(baseAddress + 0x5c, 1, flags());
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
            return (flags & MASK_IRKALLA) > 0;
        }

        public boolean swamFromPurgatory() {
            return (flags & MASK_SWIM_EXIT) > 0;
        }

        public boolean blessedByEnkidu() {
            return (flags & MASK_ENKIDU) > 0;
        }

        public boolean blessedByUniversalGod() {
            return (flags & MASK_UNIVERSAL) > 0;
        }

        public boolean isStunned() {
            return (status & MASK_STUN) > 0;
        }

        public boolean isPoisoned() {
            return (status & MASK_POISON) > 0;
        }

        public boolean isChained() {
            return (status & MASK_CHAINS) > 0;
        }

        public boolean isDead() {
            return (status & MASK_DEAD) > 0;
        }
        
        public Builder thaw() {
            return new Builder()
                    .baseAddress(baseAddress)
                    .name(name)
                    .gender(gender)
                    .npcID(npcID)
                    .summonedLifespan(summonedLifespan)
                    .level(level)
                    .experience(experience)
                    .unspentAP(unspentAP)
                    .gold(gold)
                    .currentStrength(currentStrength)
                    .maximumStrength(maximumStrength)
                    .currentDexterity(currentDexterity)
                    .maximumDexterity(maximumDexterity)
                    .currentIntelligence(currentIntelligence)
                    .maximumIntelligence(maximumIntelligence)
                    .currentSpirit(currentSpirit)
                    .maximumSpirit(maximumSpirit)
                    .currentHealth(currentHealth)
                    .maximumHealth(maximumHealth)
                    .currentStun(currentStun)
                    .maximumStun(maximumStun)
                    .currentPower(currentPower)
                    .maximumPower(maximumPower)
                    .attackValue(attackValue)
                    .defenseValue(defenseValue)
                    .armorClass(armorClass)
                    .skills(skills)
                    .spells(spells)
                    .inventory(inventory)
                    .flags(flags)
                    .status(status);
        }
        
        private static class Builder {
            private int baseAddress;
            private String name;
            private Gender gender;
            private int npcID;
            private double summonedLifespan;
            private int level;
            private int experience;
            private int unspentAP;
            private int gold;
            private int currentStrength;
            private int maximumStrength;
            private int currentDexterity;
            private int maximumDexterity;
            private int currentIntelligence;
            private int maximumIntelligence;
            private int currentSpirit;
            private int maximumSpirit;
            private int currentHealth;
            private int maximumHealth;
            private int currentStun;
            private int maximumStun;
            private int currentPower;
            private int maximumPower;
            private int attackValue;
            private int defenseValue;
            private int armorClass;
            private Map<String, Integer> skills;
            private Map<String, Set<String>> spells;
            private List<Item> inventory;
            private int flags;
            private int status;

            public Builder baseAddress(int baseAddress) {
                this.baseAddress = baseAddress;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder gender(Gender gender) {
                this.gender = gender;
                return this;
            }

            public Builder npcID(int npcID) {
                this.npcID = npcID;
                return this;
            }

            public Builder summonedLifespan(double summonedLifespan) {
                this.summonedLifespan = summonedLifespan;
                return this;
            }

            public Builder level(int level) {
                this.level = level;
                return this;
            }

            public Builder experience(int experience) {
                this.experience = experience;
                return this;
            }

            public Builder unspentAP(int unspentAP) {
                this.unspentAP = unspentAP;
                return this;
            }

            public Builder gold(int gold) {
                this.gold = gold;
                return this;
            }

            public Builder currentStrength(int currentStrength) {
                this.currentStrength = currentStrength;
                return this;
            }

            public Builder maximumStrength(int maximumStrength) {
                this.maximumStrength = maximumStrength;
                return this;
            }

            public Builder currentDexterity(int currentDexterity) {
                this.currentDexterity = currentDexterity;
                return this;
            }

            public Builder maximumDexterity(int maximumDexterity) {
                this.maximumDexterity = maximumDexterity;
                return this;
            }

            public Builder currentIntelligence(int currentIntelligence) {
                this.currentIntelligence = currentIntelligence;
                return this;
            }

            public Builder maximumIntelligence(int maximumIntelligence) {
                this.maximumIntelligence = maximumIntelligence;
                return this;
            }

            public Builder currentSpirit(int currentSpirit) {
                this.currentSpirit = currentSpirit;
                return this;
            }

            public Builder maximumSpirit(int maximumSpirit) {
                this.maximumSpirit = maximumSpirit;
                return this;
            }

            public Builder currentHealth(int currentHealth) {
                this.currentHealth = currentHealth;
                return this;
            }

            public Builder maximumHealth(int maximumHealth) {
                this.maximumHealth = maximumHealth;
                return this;
            }

            public Builder currentStun(int currentStun) {
                this.currentStun = currentStun;
                return this;
            }

            public Builder maximumStun(int maximumStun) {
                this.maximumStun = maximumStun;
                return this;
            }

            public Builder currentPower(int currentPower) {
                this.currentPower = currentPower;
                return this;
            }

            public Builder maximumPower(int maximumPower) {
                this.maximumPower = maximumPower;
                return this;
            }

            public Builder attackValue(int attackValue) {
                this.attackValue = attackValue;
                return this;
            }

            public Builder defenseValue(int defenseValue) {
                this.defenseValue = defenseValue;
                return this;
            }

            public Builder armorClass(int armorClass) {
                this.armorClass = armorClass;
                return this;
            }

            public Builder skills(Map<String, Integer> skills) {
                this.skills = skills;
                return this;
            }

            public Builder setSkill(String name, Integer val) {
                this.skills.put(name, val);
                return this;
            }

            public Builder spells(Map<String, List<String>> spells) {
                // make sure we don't trip on unmodifiable inputs
                final Map<String, Set<String>> mySpells = new HashMap<>();
                for (var entry : spells.entrySet()) {
                    mySpells.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
                this.spells = mySpells;
                return this;
            }

            public Builder setSpell(String schoolName, String spellName, boolean has) {
                final boolean anySpells;
                final Set<String> spellsInSchool;
                if (spells.containsKey(schoolName)) {
                    anySpells = true;
                    spellsInSchool = spells.get(schoolName);
                } else {
                    anySpells = false;
                    spellsInSchool = new HashSet<>();
                }

                if (has) {
                    spellsInSchool.add(spellName);
                } else {
                    spellsInSchool.remove(spellName);
                }

                if (spellsInSchool.isEmpty()) {
                    if (anySpells) this.spells.remove(schoolName);
                } else {
                    if (!anySpells) this.spells.put(schoolName, spellsInSchool);
                }

                return this;
            }

            public Builder inventory(List<Item> inventory) {
                this.inventory = inventory;
                return this;
            }

            public Builder flags(int flags) {
                this.flags = flags;
                return this;
            }

            private void updateFlag(int mask, boolean flag) {
                if (flag)
                    flags = flags | mask;
                else
                    flags = flags & (~mask);
            }

            public Builder blessedByIrkalla(boolean flag) {
                updateFlag(MASK_IRKALLA, flag);
                return this;
            }

            public Builder swamFromPurgatory(boolean flag) {
                updateFlag(MASK_SWIM_EXIT, flag);
                return this;
            }

            public Builder blessedByEnkidu(boolean flag) {
                updateFlag(MASK_ENKIDU, flag);
                return this;
            }

            public Builder blessedByUniversalGod(boolean flag) {
                updateFlag(MASK_UNIVERSAL, flag);
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            private void updateStatus(int mask, boolean flag) {
                if (flag)
                    status = status | mask;
                else
                    status = status & (~mask);
            }

            public Builder isStunned(boolean flag) {
                updateStatus(MASK_STUN, flag);
                return this;
            }

            public Builder isPoisoned(boolean flag) {
                updateStatus(MASK_POISON, flag);
                return this;
            }

            public Builder isChained(boolean flag) {
                updateStatus(MASK_CHAINS, flag);
                return this;
            }

            public Builder isDead(boolean flag) {
                updateStatus(MASK_DEAD, flag);
                return this;
            }

            public PartyMember freeze() {
                final Map<String, List<String>> theirSpells = new HashMap<>();
                for (var school : MAGIC_SCHOOLS) {
                    final Set<String> knownSpells = spells.getOrDefault(school.name(), Set.of());
                    if (!knownSpells.isEmpty()) {
                        theirSpells.put(school.name(), new ArrayList<>(knownSpells));
                    }
                }

                return new PartyMember(
                        baseAddress,
                        name, gender, npcID, summonedLifespan,
                        level, experience, unspentAP, gold,
                        currentStrength, maximumStrength,
                        currentDexterity, maximumDexterity,
                        currentIntelligence, maximumIntelligence,
                        currentSpirit, maximumSpirit,
                        currentHealth, maximumHealth,
                        currentStun, maximumStun,
                        currentPower, maximumPower,
                        attackValue, defenseValue, armorClass,
                        skills, theirSpells, inventory,
                        flags, status
                );
            }
        }
    }
}
