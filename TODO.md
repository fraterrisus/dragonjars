# TODO

- The startup check on files doesn't actually check anything on DRAGON.COM.
- Write the Torch animation handler.
- Figure out how the timers work for spell duration (including summons!!!)
- Make the combat delay configurable.
- Add a menu item that draws the current parsed map.
- Add a menu item with documentation, including this TODO list, BUGFIXes, design choices, and other places where this diverges from the original.
- Figure out what the fuck Frobs actually are, and manage them better. (This is a memory allocation thing; right now you're lazily assuming that you can load as many chunks as you want without penalty, which is basically true.)
- Check that the weird wrapping behavior on the Dwarf Clan Hall works.

# Known Bugs and Differences

- The original relies on the fact that calling DrawCurrentViewport multiple times takes long enough that you get to see each screen draw before it executes the next one. So, for instance, running from combat actually draws three frames (turn R, turn R, step). Our video handler runs multiple frames on a single thread, so that doesn't work. It's possible that fixing this requires separating the `Interpreter` out into a separate thread and using `Platform.runLater` to manage the screen draws.
- Using JavaFX's `scale` function to scale the output `Image` doesn't actually result in integer scaling, and I have no idea why not.
- If you refuse to move to another board (when presented with a Y/N Modal), we mess up the return address
- Hitting the stone with a plaque on the south side of the first Guard Bridge throws an error
