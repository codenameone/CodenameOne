---
title: CREATE A BASIC HELLO WORLD APPLICATION & SEND IT TO MY DEVICE USING ECLIPSE
slug: how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-eclipse
url: /how-do-i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-eclipse/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-eclipse.html
tags:
- basic
- ui
description: Getting started guide for Eclipse developers
youtube_id: fmNpMFLwABA
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-21.jpg
---
{{< youtube "fmNpMFLwABA" >}} 
The first Codename One application you build in Eclipse should be intentionally small. [Create the project with the initializr](/initializr/), import it into Eclipse as a Maven project, and aim for something that starts in the simulator, shows a form, and responds when you press a button. The video is out of date in one important way: it starts from the old Eclipse plugin workflow. The actual lesson is still valid, but the modern setup is Maven-based and does not depend on the legacy IDE plugins.

One of the first choices that matters is the package name. It is easy to treat that as temporary when you are just building a sample app, but package names become part of the app's identity and eventually affect signing, store metadata, and native packaging. Pick a stable reverse-domain package from the beginning so you do not have to rename it later when the project is already in motion.

Once the project is open, spend a few minutes reading the generated application class. Codename One applications still revolve around `init()`, `start()`, `stop()`, and `destroy()`. `init()` is where one-time setup belongs. `start()` is where you build and show the first UI. `stop()` handles the app moving into the background, and `destroy()` is there for shutdown cleanup. Understanding those lifecycle methods is one of the most useful things a hello world app can teach.

Keep the first screen simple. Create a `Form`, add a button, attach an action listener, and show a dialog when the button is pressed. That tiny exercise proves that the project imports correctly, the application starts, the UI appears, and events are firing. You do not need more than that to confirm that the environment is healthy.

The simulator should be your primary feedback loop at this stage. Run the app, switch device skins if you want to see how the layout behaves, and make sure the form survives the normal stop-and-start flow. Device builds matter, but they come after the simulator loop is already stable. In practice Android is usually the easiest first target. iOS generally comes later because certificates and provisioning need to be set up before the build becomes useful.

Styling and localization are the other places where the old workflow needs translation. The video moves from the basic app into the theme designer and resource editor because that was a common path at the time. For new projects, CSS is the better default for styling and l10n property bundles are the better default for localization. The older tools still work, but they are no longer the path most new apps should start with.

That leaves a straightforward modern workflow: generate a Maven project, choose a good package name, understand the lifecycle, build one small interactive form, validate it in the simulator, and then send a device build. Once those basics are in place, you can keep building in normal Java code while using CSS for styling and property bundles for localization.

## Further Reading

- [Initializr](/initializr/)
- [Getting Started](/getting-started/)
- [Hello World](/hello-world/)
- [Development Environment](/development-environment/)
- [Build Server](/build-server/)
- [Themeing](/themeing/)
- [Moving To Maven](/blog/moving-to-maven/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
