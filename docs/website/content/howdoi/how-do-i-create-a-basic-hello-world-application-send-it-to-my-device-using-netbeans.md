---
title: CREATE A BASIC HELLO WORLD APPLICATION & SEND IT TO MY DEVICE USING NETBEANS
slug: how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-netbeans
url: /how-do-i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-netbeans/
type: howdoi
original_url: https://www.codenameone.com/how_di_i/how-do-i-create-a-basic-hello-world-application-send-it-to-my-device-using-netbeans.html
tags:
- basic
- featured
- ui
description: Getting started guide for NetBeans developers
youtube_id: 73d65cvyQv4
thumbnail: https://www.codenameone.com/wp-content/uploads/2020/09/hqdefault-21.jpg
---
{{< youtube "73d65cvyQv4" >}} 
The first Codename One application you build in NetBeans should be deliberately simple. [Create the project with the initializr](/initializr/), open it in NetBeans as a Maven project, and aim for an app that starts in the simulator, shows a form, and responds to one button press. The video uses the old NetBeans plugin flow, which is no longer the recommended way to start a project, but the underlying lesson is still the same: begin with a tiny app that teaches you the lifecycle and the development loop.

The package name is one of the first things worth deciding carefully. It may look unimportant in a hello world app, but later it becomes part of signing, native packaging, and store submission. A stable reverse-domain package name will save you from an annoying rename once the project begins to grow.

After the project opens, look at the generated application class before making larger changes. `init()` is for one-time application setup, `start()` is where you create and show the first form, `stop()` handles the app moving to the background, and `destroy()` is there for shutdown cleanup. Those methods are the backbone of a Codename One app, and understanding them early makes the rest of the framework much easier to follow.

For the actual hello world UI, keep it boring on purpose. Create a form, add a button, and have the button show a dialog. That is enough to prove that the project opens correctly, the lifecycle is working, the UI renders, and action listeners are firing. You do not need complex navigation or custom components to learn the right first lessons.

The simulator is still the fastest place to validate this work. Run the app, switch device skins if needed, and make sure the form behaves the way you expect. Once that loop is stable, send a native build. Android is usually the easiest first target. iOS is still more demanding because of certificates and provisioning, so it is normal to get Android working first and then return to Apple signing later.

One part of the video does need an explicit update. It moves naturally from the generated app into the older theme designer and resource editor. For new projects, CSS is now the better default for styling and l10n property bundles are the better default for localization. The designer and resource editor are still supported, but they are not where most modern Codename One projects should start.

So the modern NetBeans version of this lesson is straightforward: start from the initializr, import the Maven project, understand the lifecycle, build a tiny interactive form, verify it in the simulator, then send a device build. From there, keep the app logic in code, style with CSS, and use property bundles when you localize.

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
