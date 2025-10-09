# TODO

- Add more combat information to the Combat Log window, like target HP after a hit.
- Do a better job listing inventory items in the Party window.
- Depths of Nisir: "...which namtar will march against" [sic] This one's tricky because you need to *insert* a 5b 
  character into the packed string.
- Figure out why the Magic Lamp's light has an infinite duration.
- Better multithreading, including (maybe) putting the interpreter on its own thread so it's not on the JavaFX
  Application thread.
- Add mouse support.
- Add support for multiple save files (that are *just* chunks 0x07 and 0x10).

# Known Bugs

- You can't update a Property from within a ChangeListener on that Property. In the case where enabling the music 
  system fails, I work around this by popping a dialog and then resetting the Property value afterwards. Ick.
- I slowed down sounds to make the 'dragon roar' noise (sound 7, see Dragon Valley or Phoebus Dungeon) sound better. 
  Is everything else off now?

# Implementation Differences

- The routine that decrements the spell counters `[0f:032f]` gets triggered (I *think*) by an idle counter that runs
  while the game is waiting for you to press a key (and possibly increments once per keypress as well?). I 
  implemented this with a timer thread, so it won't be a perfect copy, but it should be more consistent.
- My implementation of Frobs is definitely incomplete/different. Freeing segments too aggressively caused bugs. I'm 
  currently erring on the side of not freeing segments, which is fine because the memory footprint is still fairly 
  small.
- Using JavaFX's `scale` function to scale the output `Image` doesn't actually result in integer scaling, and I 
  have no idea why not.
