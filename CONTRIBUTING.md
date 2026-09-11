# How to Contribute to this Project

There are lots of ways you can help make *DragonJars* a better project! This document lists the ones I've thought of.

## Ground Rules

The most important rule for contributing to this project is this: **No generative AI. None.** I don't want it in the documentation and I don't want it in the code. This rule is not negotiable, and I am unlikely to engage with any attempt at discussion.

Apart from that, please keep in mind that *DragonJars* was and continues to be a labor of love on my part, and no one is getting paid here. *Dragon Wars* isn't my game, after all. I don't have a Code of Conduct — *yet* — because not enough people have expressed an interest in helping. In lieu of a formal document, please follow [Wheaton's Law](https://knowyourmeme.com/memes/wheatons-law).

## How You Can Help

In short:

- Check the [TODO file](TODO.md). I keep a running list of bugs I know about, things I'd like to do in the future, etc.
- If your bug, idea, problem, or wish list item isn't in that file, write a [GitHub Issue](https://github.com/fraterrisus/dragonjars/issues) against this repository.

More details follow in the sections below.

## Play the Game

The best way to help out is simply to download the project and use it to play *Dragon Wars*! I'm just one person running my own archived copy of the game on my own machine. 

Do the Windows and MacOS versions even work? Does it work with the Steam or GOG versions of *Dragon Wars*? If you try it, I'd love to know — file an Issue on this repository with your notes!

## Share Ideas

If you've got an idea for *DragonJars* but don't want to or can't take it on yourself, your best bet is to file an Issue. Describe the feature request, runtime problem, documentation issue, or wish list item as best you can. The more detail you can provide, the more likely I am to share your vision and want to take it on! I don't promise to respond to every issue, but I'll do my best to at least give a :+1: / :-1: if I think it's feasible.

## Contribute Code

I welcome code contributions from fans and outside developers. However, I have a pretty high bar for code quality and I'm pretty opinionated about software design, so if you're thinking of submitting some code you might do well to file an Issue and have a conversation with me about *how* to fix it before you go off and do it. That said, filing a PR without telling me first won't get you rejected out of hand.

As is pretty common for GitHub, your best bet is to fork the repo, make a branch with your changes, and file a PR to request a merge. Tag me as a reviewer so I get a notification; otherwise it might take me a while to see your request.

If you'd like to contribute but don't know what changes you'd like to make, check the [TODO file](TODO.md) for some ideas.

### Environment Setup

_DragonJars_ is written in Java. I use OpenJDK as my development JRE and Gradle as my build tool. You (or your IDE) ought to be able to figure out things like which JRE version I'm developing against (24 at the time of this writing) by reading the Gradle project file. My development environment run on Linux using JetBrains IDEA, but Java and JavaFX *should* be cross-platform enough that neither of those should ultimately matter.

### Understanding the Engine

If you want to dig into the internals of the engine itself (as opposed to the UI), you may want to check out the sibling repository where attempt to document the internals of the game, the data structures, and how it does things: [dragonwars-crack](https://github.com/fraterrisus/dragonwars-crack)

In particular, that repo has some utilities that can be used to rip apart the data files and get at the microcode that runs the actual game. Some of that work got duplicated here (check the `test` directories) too.

### Building

In short, `./gradlew build`.

You should be able to either use the gradle wrapper script or compile through your IDE, presuming it's capable of reading gradle definition files. The `build` target should download the correct version of Gradle, compile the Java, and run the test suite.

You may also want:

- `gradlew wrapper` to update the wrapper script.
- `gradlew run` to start a local development copy of *DragonJars*.
- `gradlew jpackage` to build an installable package for your local operating system.

### Testing

I don't demand that everyone write tests for every block of code they contribute; that would be exceptionally 
hypocritical. I have *some* tests in this code base, but only for the parts that seemed critical and worth testing 
at the time.

That being said, regardless of whether you write any test code, I do appreciate at least a statement of how you 
tested your change, why you're confident it does the right thing, and for real bonus points, a set of steps that I 
can run to convince myself that your code does the right thing. 

You may notice that there are some tests that are `@DisabledIf` a certain property doesn't exist. That property is 
set to the empty string in `com/hitchhikerprod/dragonjars/system.properties`; if you want to set a value, you should 
create `com/hitchhikerprod/dragonjars/personal.properties` and override the value there. That file is in the 
`.gitignore` so there's no danger of you (or me!) accidentally checking it in.

### Code Review

This section definitely needs to be filled out in more detail. I don't have a particular coding style standard; because I'm the solo developer, it's pretty much based on "gut feel" at the moment.

Philosophically, I see code review as a *conversation*. Don't expect me to merge your code immediately. If it's anything more than trivial, I will probably ask questions about why you built something the particular way you did, and may point out an alternative implementation or something you may not have noticed about the code. This isn't because I'm ungrateful; it's because I expect there are things that we can both learn from each other through code review.

## Improve the Docs

*DragonJars* is, like most open source projects, woefully under-documented. I've handwaved my way at a "quick start" guide in the README but that's about it. If you've got ideas for ways to make it better, I'm all ears!

Similarly, I could use a much better icon, as opposed to the clipped version of the title screen I'm currently using.