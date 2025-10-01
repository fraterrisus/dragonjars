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



## Future Work

- Mouse support.
- A combat log that shows hidden information about your opponents, attack rolls, etc.
- Better threading safety.

## Bugs I Fixed

- Fixed some typos in the game's text (misplaced apostrophes, misspelled words, etc.)
- If you attacked with a Thrown Weapon, the original used your Fistfighting skill when determining your combat skill.
  This was clearly a bug, because the Thrown Weapons skill is _right there!_
- **Thrown Weapons can now be used from the back row** (slots 5-7). This one might be controversial, but I genuinely
  think this was a mistake by the developers rather than an intentional design choice. Regardless, if you don't like it,
  there's an in-app preference to disable it!
- On the Dwarf Ruins map, there's a chest that contains the Dwarf Hammer. The game state bit that determines whether
  you can see this chest is the same as the one for the chest behind the wizard's house in the Slave Camp. There's
  no reason finding that chest should prevent you from finding the Dwarf Hammer, so I moved it to another unused bit
  in the game state.
- The original's automap doesn't draw walls along its eastern edge, due to the way the wall data is stored in adjacent
  squares. Mine does.
- There are a handful of places where leaving a map drops you on Dilmun at a place that doesn't make any sense. For
  example, leaving Purgatory on foot to the North should drop you at (13,5) but you wind up at (15,5). The Pilgrim
  Dock's exits are also messed up.
- Near where you enter the Depths of Nisir there's an area of icy winds. In the NE corner of that region, the
  spinner at (19,23) has been moved to (20,23) to match the other three corners.
- There's an occasional display bug based on a set of hardcoded "regions" in the display. The region for the party
  space has its Y offset set to the wrong value. You probably won't even notice I fixed this.
 
