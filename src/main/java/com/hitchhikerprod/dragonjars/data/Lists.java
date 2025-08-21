package com.hitchhikerprod.dragonjars.data;

public class Lists {
    static final String[] SKILL_NAMES = {
        "Arcane Lore", "Cave Lore", "Forest Lore", "Mountain Lore",
        "Town Lore", "Bandage", "Climb", "Fistfighting",
        "Hiding", "Lockpick", "Pickpocket", "Swim",
        "Tracker", "Bureaucracy", "Druid Magic", "High Magic",
        "Low Magic", "Merchant" /* ?! */, "Sun Magic", "Axes",
        "Flails", "Maces", "Swords", "Two-handers",
        "Bows", "Crossbows", "Thrown Weapons"
    };

    static final String[][] SPELL_NAMES = {
            { "L:Mage Fire", "L:Disarm", "L:Charm", "L:Luck", "L:Lesser Heal",
                    "L:Mage Light", "H:Fire Light", "H:Elvar's Fire" },
            { "H:Poog's Vortex", "H:Ice Chill", "H:Big Chill", "H:Dazzle",
                    "H:Mystic Might", "H:Reveal Glamour", "H:Sala's Swift", "H:Vorn's Guard" },
            { "H:Cowardice", "H:Healing", "H:Group Heal", "H:Cloak Arcane",
                    "H:Sense Traps", "H:Air Summon", "H:Earth Summon", "H:Water Summon" },
            { "H:Fire Summon", "D:Death Curse", "D:Fire Blast", "D:Insect Plague",
                    "D:Whirl Wind", "D:Scare", "D:Brambles", "D:Greater Healing" },
            { "D:Cure All", "D:Create Wall", "D:Soften Stone", "D:Invoke Spirit",
                    "D:Beast Call", "D:Wood Spirit", "S:Sun Stroke", "S:Exorcism" },
            { "S:Rage of Mithras", "S:Wrath of Mithras", "S:Fire Storm", "S:Inferno",
                    "S:Holy Aim", "S:Battle Power", "S:Column of Fire", "S:Mithras' Bless" },
            { "S:Light Flash", "S:Armor of Light", "S:Sun Light", "S:Heal",
                    "S:Major Healing", "S:Disarm Trap", "S:Guidance", "S:Radiance" },
            { "S:Summon Salamander", "S:Charger", "M:Zak's Speed", "M:Kill Ray",
                    "M:Prison", "0x04", "0x02", "0x01" }
            // S:Light Flash and M:Prison are in the manual, but never found in the game.
    };

    static public final String[] CHAR_FIELDS = { // base offset 0x4c
        "Status", "NPC ID", "Gender", "Level b0", "Level b1",
        "XP b0", "XP b1", "XP b2", "XP b3",
        "Gold b0", "Gold b1", "Gold b2", "Gold b3",
        "AV", "DV", "AC", "Flags"
    };

    static public final String[] REQUIREMENTS = { // base offset 0x0c
        // apostrophe -> current (potentially modified) value
        "Strength'", "Strength", "Dexterity'", "Dexterity",
        "Intelligence'", "Intelligence", "Spirit'", "Spirit",
        "Health'b0", "Health'b1", "Health b0", "Health b1",
        "Stun'b0", "Stun'b1", "Stun b0", "Stun b1",
        "Power'b0", "Power'b1", "Power b0", "Power b1",
        "Arcane Lore", "Cave Lore", "Forest Lore", "Mountain Lore",
        "Town Lore", "Bandage", "Climb", "Fistfighting",
        "Hiding", "Lockpick", "Pickpocket", "Swim",
        "Tracker", "Bureaucracy", "Druid Magic", "High Magic",
        "Low Magic", "Merchant", "Sun Magic", "Axes",
        "Flails", "Maces", "Swords", "Two-handers",
        "Bows", "Crossbows", "Thrown Weapons", "-",
        "-", "-", "-", "-",
        "-", "-", "-", "-",
        "-", "-", "-", "-",
        "-", "-", "-", "0xFF"
    };

    static final String[] ITEM_TYPES = {
        "General Item", "Shield", "Full Shield", "Axe",
        "Flail", "Sword", "Two-handed sword", "Mace",
        "Bow", "Crossbow", "Gun" /* ?!?! */, "Thrown weapon",
        "Ammunition", "Gloves", "Mage Gloves", "Ammo Clip",
        "Cloth Armor", "Leather Armor", "Cuir Bouilli Armor", "Brigandine Armor",
        "Scale Armor", "Chain Armor", "Plate and Chain Armor", "Full Plate Armor",
        "Helmet", "Scroll", "Boots", "-",
        "-", "-", "-", "-"
    };

    static final String[] WEAPON_TYPES = {
        "Axe", "Flail", "Sword", "Two-handed sword", "Mace", "Thrown weapon", "Ammunition"
    };

    static final String[] MONSTER_IMAGES = {
        "0x8a",
        "Water Spirit", // 0x8c
        "0x8e", // 0x8e
        "Skeleton", // 0x90
        "Zombie", // 0x92
        "0x94",
        "0x96",
        "Barbarian", // 0x98
        "0x9a", // 0x9a
        "Spear Guard", // 0x9c
        "Bear", // 0x9e
        "Turtle", // 0xa0
        "Dragon Lizard", // 0xa2
        "Martial Artist", // 0xa4
        "Halbard Guard", // 0xa6
        "Wolf", // 0xa8
        "Adventurer", // 0xaa
        "Old Crone", // 0xac
        "Namtar", // 0xae
        "Executioner", // 0xb0
        "Lizardman", // 0xb2
        "Dragon Warrior", // 0xb4
        "Wraith", // 0xb6
        "0xb8", // 0xb8
        "Murk Tree", // 0xba
        "Rock Man", // 0xbc
        "Gaze Demon", // 0xbe
        "Snake", // 0xc0
        "Hydra", // 0xc2
        "Spider", // 0xc4
        "Creepy Mage", // 0xc6
        "Beggar", // 0xc8
        "Gladiator", // 0xca
        "Old Man", // 0xcc
        "Slave", // 0xce
        "Greeting Machine", // 0xd0
        "Lance Guard", // 0xd2
        "Wild Man", // 0xd4
        "Pirate", // 0xd6
        "Scorpion", // 0xd8
        "Humbaba", // 0xda
        "Stosstrupen", // 0xdc
        "Wild Boy", // 0xde
        "Nergal", // 0xe0
        "Goblin", // 0xe2
        "Cocatrice", // 0xe4
        "Pilgrim", // 0xe6
        "Irkalla", // 0xe8
        "0xea",
        "Dragon Whelp", // 0xec
        "Forester", // 0xee
        "Stag", // 0xf0
        "Robed Man", // 0xf2
        "Wizard", // 0xf4
        "0xf6", // 0xf6
        "Robot", // 0xf8
        "0xfa",
        "Rat", // 0xfc
        "Dragon Queen" // 0xfe
    };
}
