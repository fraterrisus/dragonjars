# TODO

- Add a menu item with documentation, including this list, BUGFIXes, design choices, and other places where this
  diverges from the original. (Note the use of data patches in the LoadDataTask.)
- Keep adding more combat information; improve the presentation layer.
- Check for TODOs and FIXMEs in the comments.
- Patch the Freeport $100 large shields.
- Depths of Nisir: "...which namtar will march against" [sic] This one's tricky because you need to *insert* a 5b 
  character into the packed string.

# Known Bugs

- You can't update a Property from within a ChangeListener on that Property. In the case where enabling the music 
  system fails, I work around this by popping a dialog and then resetting the Property value afterwards. Ick.
- I slowed down sounds to make the 'dragon roar' noise (sound 7, see Dragon Valley or Phoebus Dungeon) sound better. 
  Is everything else off now?

# Bugs in the original that are fixed in this version

- On the Dwarf Ruins map, there's a chest that contains the Dwarf Hammer. The game state bit that determines whether 
  you can see this chest is the same as the one for the chest behind the wizard's house in the Slave Camp. There's 
  no reason finding that chest should prevent you from finding the Dwarf Hammer, so I moved it to another unused bit 
  in the game state.
- Thrown Weapons can't be used from the back rank (slots 5-7). It's probably controversial of me to say this, but I 
  think this is a bug caused by a pair of off-by-one errors, and the patch to fix them is tiny.
- The automap doesn't draw walls along its eastern edge, due to the way the wall data is stored in adjacent squares.
- There are a handful of places where leaving a map drops you on Dilmun at a place that doesn't make any sense. For 
  example, leaving Purgatory on foot to the North should drop you at (13,5) but you wind up at (15,5). The Pilgrim 
  Dock's exits are also messed up.
- Near where you enter the Depths of Nisir there's an area of icy winds. In the NE corner of that region, the 
  spinner at (19,23) has been moved to (20,23) to match the other three corners.
- There's an occasional display bug based on a set of hardcoded "regions" in the display. The region for the party 
  space has its Y offset set to the wrong value. You probably won't even notice I fixed this.

# Implementation Differences

- I added an "auto-Bandage" routine that tries to hew closely to having your 'medic' use Bandage on everyone in the 
  party. Hit Ctrl-B in travel mode.
- The routine that decrements the spell counters seems to run out of 0f/032f, which *I think* gets triggered by an 
  idle counter that runs while the game is waiting for you to press a key (and possibly increments once per keypress 
  as well?). I'm not going to emulate that; I'm going to put my own timer thread on it.
- My implementation of Frobs is definitely incomplete/different. It's possible that I'm freeing some segments too 
  aggressively and that's causing bugs.
- Using JavaFX's `scale` function to scale the output `Image` doesn't actually result in integer scaling, and I 
  have no idea why not.

# Threading the Interpreter

The original relies on the fact that calling DrawCurrentViewport multiple times takes long enough that you get to see
each screen draw before it executes the next one. So, for instance, running from combat actually draws three frames
(turn R, turn R, step). Our video handler runs multiple frames on a single thread -- on the UI thread, no less -- so 
that doesn't work. The simple solution, which I've already implemented, is just to insert a 100ms pause before
DrawCurrentViewport does its work. It's inelegant, but effective.

The other alternative is to split the Interpreter out into its own thread. The primary difficulty will be 
separating out all the places where the Interpreter calls the App directly; in particular, the way that we "return 
null" in order to wait for user input will be different, so we may have to rebuild the #reenter system (again). The 
second difficulty will be rewriting the VideoHelper to push everything to the screen Image with Platform.runLater(); 
we'll also want to run some sort of delay when we do it. But how will we know when we've pushed the "whole screen"? 

Places where we try to talk to the DragonWarsApp object directly:
- DragonWarsApp#startInterpreter gets a little more interesting because it's a Thread#start
- Calls to app#setKeyHandler happen in a bunch of places; this requires re-architecting user input, but that might be 
  *better*. I wonder if we can manage the thread communication with an actual Lock object.
- Interpreter#mainLoop calls app#openParagraphsWindow. Trivial runLater.
- app#close should be fine.
- We talk to the MusicService via the app, which may be tricky given the way we currently play the title music. Or 
  maybe this just works with runLater(); it's spawning off its own threads, after all.
- YouWin probably needs some rewriting, although mostly it's playTitleMusic(), setKeyHandler(), and one call to 
  setImage() (that's the tricky one).

Uses of PixelBuffer#writeTo(PixelWriter):
- app#showTitleScreen(), app#testPattern(), fine
- YouWin#handler(), see above
- Interpreter#bitBlast(), which gets used in a limited number of places:
  - drawViewportCorners
  - drawHUD, drawMapTitle, and spell icons, which could easily be refactored into a local buffer
  - drawPartyInfoArea
  - MonsterAnimationTask, which already uses Platform.runLater()

It's also worth thinking clearly about the use of locks and cross-thread communication, which is currently kind of a 
mess.