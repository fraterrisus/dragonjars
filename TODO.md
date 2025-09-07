# TODO

- Find the timer mechanism on summons.
- Add a menu item that draws the current parsed map.
- Add a menu item with documentation, including this list, BUGFIXes, design choices, and other places where this
  diverges from the original.
- Check that the weird wrapping behavior on the Dwarf Clan Hall works.

# Known Bugs and Differences

- Monster animations continue after the combat booty dialog pops up, but stop once you clear it.
- The routine that decrements the spell counters seems to run out of 0f/032f, which *I think* gets triggered by an 
  idle counter that runs while the game is waiting for you to press a key (and possibly increments once per keypress 
  as well?). I'm not going to emulate that; I'm going to put my own timer thread on it.
- MapData reads the 3b square data "backwards", or in a more big-endian style.
- "Enter Slave Camp" + "?" results in a full line with the question mark on the following. This is because we don't
  do the weird string "redirect to list, then print the saved list" thing.
- The original relies on the fact that calling DrawCurrentViewport multiple times takes long enough that you get to see
  each screen draw before it executes the next one. So, for instance, running from combat actually draws three frames 
  (turn R, turn R, step). Our video handler runs multiple frames on a single thread, so that doesn't work. It's possible
  that fixing this requires separating the `Interpreter` out into a separate thread and using `Platform.runLater` to 
  manage the screen draws.
- Figure out what the fuck Frobs actually are, and manage them better. (This is a memory allocation thing; right now
  you're lazily assuming that you can load as many chunks as you want without penalty, which is basically true.)
- Using JavaFX's `scale` function to scale the output `Image` doesn't actually result in integer scaling, and I 
  have no idea why not.
