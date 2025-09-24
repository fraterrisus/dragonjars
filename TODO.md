# TODO

- Add a menu item with documentation, including this list, BUGFIXes, design choices, and other places where this
  diverges from the original. (Note the use of data patches in the LoadDataTask.)
- Check for TODOs and FIXMEs in the comments.

# Known Bugs

- Monsters are making weird combat decisions, like calling for help against a level 5 party and dodging instead of 
  advancing and attacking.
- You can't update a Property from within a ChangeListener on that Property. In the case where enabling the music 
  system fails, I work around this by popping a dialog and then resetting the Property value afterwards. Ick.

# Implementation Differences

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