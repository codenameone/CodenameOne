---
title: "Swing"
date: 2015-03-03
slug: "swing"
---

# Swing

Codename One introduction for Swing developers

1. [Home](/)
2. [Developers](https://beta.codenameone.com/developing-in-codename-one.html)
3. Swing

<iframe src="https://www.youtube.com/embed/6SBZEKd_ulo?rel=0" width="640" height="360" frameborder="0" allowfullscreen="allowfullscreen"></iframe>

If you are familiar with Swing then Codename One will be instantly familiar to you! It was built by Swing fans and was deeply inspired by everything in Swing and everything missing in Swing. We took what we liked in Swing and added all the stuff that was missing. So Codename One is Swing redesigned for mobile with the advantage of "hindsight"!

### What is similar to Swing

- Lightweight architecture, you can just override paint
- EDT - Event Dispatch Thread
- List renderer, List model
- Layout managers
- Component-Container hierarchy
- Dialog's block code execution
- Runtime PLAF/Theme switching

### What is different

- Built in transitions & animations
- InvokeAndBlock() - you can stop the EDT in its place when doing it right! (like foxtrot)
- Painters
- Styles
- Themes
- Standardized GUI builder
- Standardized resource file
- Seamless localization
- Integration with IO/Networking (e.g. progress indication with one line of code)
- Multi-DPI support
- 9-piece image borders
- Heavyweight/native integration
- Statically linked (no worries about "is this version installed")
- No JComponent hack, Component-Container as originally intended

To learn more about Codename One in general you can go to our developer section. Read below for information on how Codename One is similar/different from Swing below.

### Getting Started

As a Swing developer you should have no trouble understanding typical handcoded Codename One apps, you have a Component/Container hierarchy and no "J" prefix e.g.:

```
Button first = new Button("First");
Button second = new Button("Second");
Container cnt = new Container(new BoxLayout(BoxLayout.X_AXIS));
cnt.addComponent(first);
cnt.addComponent(second);
```

Just places the two buttons in a row one next to the other just like it would in Swing!

So what is different? A great deal of things but the first thing that is highly noticeable is theming and styles. You can style a component via the designer tool's UI, every component accepts the style of the theme. So to make a text area look like a label (which allows for a multi-line label) all we have to do is:

```
TextArea t = new TextArea("Text area that looks like a label and breaks lines");
t.setEditable(false);
t.setUIID("Label");
```

### Lifecycle & Mobile Development

Programming for mobile is very different from programming to the desktop, there is no main method. Instead you have a "lifecycle" class that manages the application behavior and states as the phone/tablet switches states (think of this as a sort of Applet). Unlike Swing which focused on solving only the UI side of things, Codename One handles IO and all phone functionality as well and integrtes this together.

Last but not least, because code is statically translated on the server you can't do things such as adding libraries that weren't compiled properly or might use unavailable functionality. You can use native code or work from sources as discussed [here](/blog/jaring-and-libraries.html).

### Forms & Dialogs

Swing has top level components specifically Frame and Dialog (or more accurately JFrame/JDialog & JWindow), Codename One has Form. Since mobile devices don't have Windows a Form always takes up the entire screen and there is always only one current form (which you can acquire via Display.getInstance().getCurrent()). A Dialog is really a special case Form (it subclasses the Form class) and is modal just like the Swing/AWT Dialog in the sense that it blocks the EDT thread until its disposed (with the dispose() method).

Codename One has a ContentPane in much the same way and it is "hidden" so addComponent calls to form are effectively equivalent to form.getContentPane().addComponent(...).

In some platforms the Form's title is just a component in the NORTH part of the internal border layout (the layer above the content pane which is hidden from developers) however on some platforms (Android 4.x specifically) it is natively implemented by default. So while you can get the title area for the form it is ill advisable since that code will not be portable.
