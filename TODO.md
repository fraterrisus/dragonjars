# TODO

- Find the timer mechanism on summons.
- Add a menu item that draws the current parsed map.
- Add a menu item with documentation, including this list, BUGFIXes, design choices, and other places where this
  diverges from the original. (Note the use of data patches in the LoadDataTask.)
- Check that the weird wrapping behavior on the Dwarf Clan Hall works.

# Known Bugs

# Implementation Differences

- The routine that decrements the spell counters seems to run out of 0f/032f, which *I think* gets triggered by an 
  idle counter that runs while the game is waiting for you to press a key (and possibly increments once per keypress 
  as well?). I'm not going to emulate that; I'm going to put my own timer thread on it.
- MapData reads the 3b square data "backwards", or in a more big-endian style.
- The automap is not persisting correctly, but I'm not sure what mechanism is or is not causing that.
- The original relies on the fact that calling DrawCurrentViewport multiple times takes long enough that you get to see
  each screen draw before it executes the next one. So, for instance, running from combat actually draws three frames 
  (turn R, turn R, step). Our video handler runs multiple frames on a single thread, so that doesn't work. It's possible
  that fixing this requires separating the `Interpreter` out into a separate thread and using `Platform.runLater` to 
  manage the screen draws.
- My implementation of Frobs is definitely incomplete/different. It's possible that I'm freeing some segments too 
  aggressively and that's causing bugs.
- Using JavaFX's `scale` function to scale the output `Image` doesn't actually result in integer scaling, and I 
  have no idea why not.
