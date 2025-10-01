# DragonJars

A full Java-based reimplementation of the engine for [Dragon Wars (1990)](https://homeoftheunderdogs.net/game.php?id=336),
with several bug fixes, improvements, and quality-of-life additions.

**Note:** In order to use this program, you'll need a copy of the game's data files (PC version). As of this writing,
someone operating the "Interplay Entertainment" license seems to be re-issuing the company's old IP on more modern
platforms (basically, a bundled version of DOSBOX plus the data files). In an effort to not violate their copyright, I
will not distribute those data files. You can currently buy a copy of _Dragon Wars_ on Steam or GOG for around 10 USD.

## Features

- All the original EGA graphics and animation, with video scaling selectable as an in-app preference.
- All the original sound, but without the tinny PC speaker harshness! Volume levels can be set as an in-app preference.
- Save-compatible with the original game.
- Combat delay can be set as an in-app preference.
- Automatically shows the paragraphs from the manual when you step on the right square.
- Contains a list of spells for quick reference, including fixing some places where the manual is inaccurate.
- Displays, on request, a full copy of the map from the in-memory map data.
- Lets you peek at the party's data, including hidden flags and information about your inventory.
- Lets you peek at _and edit_ the game's state flags on the fly.
- Hit Ctrl-B in travel mode to "auto-Bandage" everyone in your party.
- Fixed some bugs (see list below).

## Who are you?

I'm Ben Cordes, aka FraterRisus. I'm a giant fan of _Dragon Wars_, and have been since 1990 or so when I bought a copy
of it for the family PC and played it to death. (I got real quick on stopping the blaring title music so as to not
annoy the rest of my family, which is why adding a _persistent_ sound preference was one of the first things I did
after I got the music working.)

I'm also the author of what I think is [the most comprehensive walkthrough](https://walkthroughs.hitchhikerprod.com/dragon-wars)
ever written for this game, based not only on dozens of playthroughs but also a year spent decompiling the source code
and the data files to figure out what makes them all tick. Which is how we arrived here: with a complete
reimplementation of the game's engine.

## Quick Start

You'll need a Java runtime that supports at least Java 22, with Java 24 recommended. I use [OpenJDK](https://openjdk.org);
other versions should also work.

1. Download the latest release from the [Releases page](https://github.com/fraterrisus/dragonjars/releases) (.zip or 
   .tar.bz2, your choice, they're the same)
2. Uncompress the archive somewhere useful.
3. (Recommended) Copy `DRAGON.COM`, `DATA1`, and `DATA2` into the `dragonjars` directory.
4. Run `bin/dragonjars` (Linux, MacOS) or `bin/dragonjars.bat` (Windows)
5. Open File > Preferences and point the game at the three data files.

## Future Work

- Mouse support.
- A combat log that shows hidden information about your opponents, attack rolls, etc.
- Better threading safety.

## Bugs I Fixed

- If you attacked with a Thrown Weapon, the original used your Fistfighting skill when determining your combat skill.
  This was clearly a bug, because the Thrown Weapons skill is _right there!_
- **Thrown Weapons can now be used from the back row** (slots 5-7). This one might be controversial, but I genuinely
  think this was a mistake by the developers rather than an intentional design choice. Regardless, if you don't like it,
  there's an in-app preference to disable it!
- Most players have never heard of the Dwarf Hammer, because the game state bit that determines whether you 
  can see it (it's in the Dwarf Ruins) is the same as the bit for the chest behind the wizard's house in the Slave 
  Camp. I can't think of a reason why the two would be mutually exclusive, so now the Dwarf Hammer chest is on a 
  different bit.
- I patched some places where leaving a map moves you to a space on the Dilmun map that doesn't make sense. Leaving 
  Purgatory to the North should drop you at (13,5) but you end up at (15,5). Likewise, the Pilgrim Dock's exits were 
  all off by a couple squares.
- Large Shields at Ryan's Armor in Freeport now cost the normal $1000 instead of an inexplicable $100. Sorry not sorry.
- Fixed some typos in the game's text (misplaced apostrophes, misspelled words, etc.)
- If you look closely, the automap doesn't draws walls along its eastern edge. Mine does.
- The "icy winds" region just outside your starting spot in the Depths of Nisir had a couple of mistakes, including 
  a misplaced spinner and a square that doesn't have the right special code. Those are fixed.
- There's an occasional display bug based on a set of hardcoded "regions" in the display. The region for the party
  space has its Y offset set to the wrong value. You probably won't even notice I fixed this.
 
