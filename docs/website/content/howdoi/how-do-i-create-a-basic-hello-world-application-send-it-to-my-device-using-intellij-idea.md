---
title: CREATE A BASIC HELLO WORLD APPLICATION & SEND IT TO MY DEVICE USING INTELLIJ/IDEA
slug: how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-intellij-idea
url: /how-do-i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-intellij-idea/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-intellij-idea.html
tags:
- basic
- ui
description: Getting started guide for IntelliJ developers
youtube_id: oR3KHYf5OrY
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-1-1.jpg
---
{{< youtube "oR3KHYf5OrY" >}}

The easiest way to get started in IntelliJ is to open a Maven-based Codename One project and keep the first version of the app very small. [Create the project with the initializr](/initializr/), import it into IntelliJ, and aim for something that starts in the simulator without surprises and does one obvious thing when you press a button. The video demonstrates the same first milestone, but it gets there through the old IDE plugin flow, which is no longer the recommended setup. That small app is enough to prove that the environment is working and to give you a solid base for the next step.

The first decision worth making carefully is the package name. In Codename One it eventually shows up in builds, signing, store metadata, and native packaging, so it is worth choosing a stable reverse-domain package from the beginning. Renaming a package later is possible, but once a project has started to accumulate build configuration and native artifacts it becomes much more annoying than it looks.

Once the project is open, the generated application class is the most important thing to understand. Codename One applications still revolve around `init()`, `start()`, `stop()`, and `destroy()`. `init()` is where one-time application setup belongs. `start()` is where you create or show the first screen. `stop()` is called when the app is backgrounded, and `destroy()` is reserved for final cleanup when the application is really shutting down. If you understand those four methods early, the rest of the framework becomes much easier to reason about.

For a first app, create a single form and add one interactive component to it. A button that opens a dialog is enough. That tiny exercise proves several important things at once: the app started successfully, the form was shown from `start()`, components were added correctly, and events are firing as expected. In practice that is much more valuable than jumping immediately into themes, navigation frameworks, or platform integration.

The simulator is still the fastest place to do this first pass. Run the app, press the button, switch simulator skins if you want to see how the screen behaves on different devices, and make sure the application survives stop-and-start cycles. You do not need to solve device-specific issues before you even know that the application works locally. Once the simulator loop is stable, send a native build. Android is usually the easiest first target. iOS still requires signing and provisioning, so it is normal to validate Android first and then handle Apple certificates afterwards.

One part of the video is clearly dated. It moves from the generated app into the older theme designer and resource-editor workflow. For a new project, the better path is to style with CSS and handle localization with l10n property bundles. The designer and resource editor still work, but they are awkward enough that they should not be the default recommendation for new development.

So the practical workflow is straightforward: start with a Maven project, understand the lifecycle, build one small interactive form, validate it in the simulator, and then send a device build. After that, keep the app logic in code, move styling into CSS, and use property bundles when you add localization.

## Further Reading

- [Getting Started](/getting-started/)
- [Development Environment](/development-environment/)
- [Hello World](/hello-world/)
- [Build Server](/build-server/)
- [Themeing](/themeing/)
- [Create an iOS Provisioning Profile](/create-an-ios-provisioning-profile/)
- [Moving To Maven](/blog/moving-to-maven/)

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
