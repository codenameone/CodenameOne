---
title: "Packaging Java Apps for Xbox Developer Mode, and Why Consoles Need Help"
slug: codename-one-on-xbox
url: /blog/codename-one-on-xbox/
date: '2026-11-10'
author: Shai Almog
description: "Codename One can now package an app or game for Xbox Developer Mode with one build hint. That covers the open part of a console port. Shipping on Xbox, PlayStation or Nintendo needs platform SDKs that sit behind NDAs, and that's where we need help."
feed_html: "<img src=\"https://www.codenameone.com/blog/codename-one-on-xbox.jpg\" alt=\"A game controller next to a Java app\" /> Codename One can now package an app or game for Xbox Developer Mode with one build hint. Shipping on consoles needs SDKs behind NDAs, and that's where we need help."
series: ["release-2026-11-06"]
---

![A game controller next to a Java app](/blog/codename-one-on-xbox.jpg)

Codename One targets iOS, Android, macOS, Windows, Linux, the web, watches, TVs and now embedded boards. There's one major category missing: game consoles.

The technical side looks manageable. A console is a computer with a GPU and a controller, and we already render through Metal, Direct3D and OpenGL ES and map controllers to game actions. They're hard because everything about them sits behind a non-disclosure agreement. This week we took the first step that doesn't need one.

## One build hint

Any retail Xbox can be switched into Developer Mode, which lets you sideload UWP apps for testing. Codename One can now build that package:

```properties
codename1.arg.windows.xbox=true
```

That builds the native Windows port as a UWP app and packages it as a signed `.msix`. If you don't provide a certificate, the build makes a self-signed development one next to the package. Then:

1. Install Dev Home on the console and turn on Developer Mode and Remote Access.
2. Open the Device Portal at `https://<console>:11443` in a browser.
3. Under Home, add the `.msix` and pick the `.cer` the build produced.

You can try the same package on a Windows PC first: import the certificate into the Local Machine's Trusted People store, then run `Add-AppxPackage` on the `.msix`.

## What changes on a console

A console app isn't a desktop app with a bigger screen. Three things had to change in the port:

{{< mermaid >}}
flowchart LR
    G[Your game or app<br/>com.codename1.gaming, Unity import, or any CN1 app] --> P[Native Windows port]
    P -->|windows.xbox=true| U[UWP app container<br/>CoreWindow, no desktop window]
    U --> R[Direct2D into a Direct3D swap chain]
    U --> I[Controller: D-pad and stick to directions<br/>A to fire, B to back]
    U --> S[TV overscan as the display safe area]
{{< /mermaid >}}

**The app container.** There's no desktop window. The port runs on a `CoreWindow` and draws with Direct2D into a Direct3D swap chain. The build links against `WindowsApp.lib` alone, which proves at build time that every system call the port makes is allowed inside the container. If something isn't allowed, the build fails instead of the console.

**The controller.** The D-pad and the left stick arrive as the four directions, A as fire and B as back, through the same `Display.getGameAction()` your game already uses for arrow keys. A game written with `com.codename1.gaming` or imported from a Unity 2D project doesn't need console-specific input code.

**The TV.** Televisions crop the edges of the picture. The visible area the console reports comes through `Display.getDisplaySafeArea()`. Use it to keep the score and the menus where people can see them.

Here's how a game reads input, unchanged from phone to desktop to console:

```java {class="cn1-no-try"}
public void keyPressed(int keyCode) {
    switch (Display.getInstance().getGameAction(keyCode)) {
        case Display.GAME_LEFT:  player.moveLeft();  break;
        case Display.GAME_RIGHT: player.moveRight(); break;
        case Display.GAME_FIRE:  player.jump();      break;
    }
}
```

## Where it stands

This is experimental, and the limits are real.

- CI cross-compiles the target on Linux, installs the package on a Windows machine and checks that it reaches its first frame inside the app container. We haven't run it on a real Xbox yet. Controller input and overscan only exist on the console, so both are untested.
- Some things the desktop port does aren't available in the container: secondary windows, menus, the tray, native text editors, file dialogs, the clipboard, printing, the camera, audio recording and the share sheet. The only file root is the app's local folder.
- Most important: **Developer Mode is a test sandbox, not a store.** You can't sell a game this way. Retail Xbox titles are built with Microsoft's Game Development Kit under a publishing agreement.

## Why consoles need a different kind of help

Every console platform holder, Microsoft, Sony and Nintendo, keeps its SDK, its development hardware and its certification process behind an NDA. You get access by signing a publishing or developer agreement, and what you learn under it can't be published. That's a hard wall for an open source project. We can't put code that uses those SDKs in a public repository, and we can't even describe the APIs in a public issue.

So a console port has two halves:

- **The portable half** belongs in Codename One, in the open: the windowless host, controller mapping, the safe area and game code that runs anywhere. That's what this week's Xbox work builds and exercises.
- **The platform half** depends on each SDK. It would have to stay proprietary and be available only to developers who hold a license for that console's SDK.

We'd like to get there. If you work at a platform holder, at a studio with console agreements, or you know the right person to talk to, please [reach out](/contact-us.html). A Java game on a console, written once and also shipped to phones, desktop and the web, is something I think a lot of small studios would use.

The [Windows chapter](/developer-guide/working-with-windows/#xbox-developer-mode) covers building and installing, and the [gaming consoles appendix](/developer-guide/gaming-consoles/) tracks the state of console work.

Tomorrow: {{< post-link path="/blog/cn1libs-with-a-server-side" text="Port Your Spring Code With Fewer Edits, and Libraries That Bring Their Own Server" >}}.

{{< giscus >}}
