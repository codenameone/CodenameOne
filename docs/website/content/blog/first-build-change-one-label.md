---
title: "Your First Codename One Build: Change One Label"
slug: first-build-change-one-label
url: /blog/first-build-change-one-label/
date: '2026-10-20'
author: Shai Almog
description: Change one label in a Java starter app, run it in the simulator, and submit an Android build with Codename One.
feed_html: '<img src="https://www.codenameone.com/blog/first-build-change-one-label.jpg" alt="A first Java app build" /> Change one label, run your Java app, and submit an Android build.'
---

![A first Java app build](/blog/first-build-change-one-label.jpg)

Your first Codename One project can be one screen with a label you recognize. Run that screen locally, then submit an Android build. That gives you a small exercise with an observable result before you add your application's features.

Codename One lets you write app screens in Java. It renders its own UI components; a `Label` does not wrap a separate native label widget on each platform. The simulator lets you work on that screen locally. A cloud build is a separate step that produces the platform build.

## Keep the starter small

Create a basic app project and open the generated project in your local development environment. Use the generated project's setup instructions for its JDK and Maven requirements. Keep its lifecycle and initialization code.

In the main application's `start()` method, find the starter screen. Replace its screen-construction lines with these, leaving any existing resume handling in place:

```java
Form firstBuild = new Form("First build", BoxLayout.y());
firstBuild.add(new Label("This is my first build."));
firstBuild.show();
```

The imports are `com.codename1.ui.Form`, `com.codename1.ui.Label`, and `com.codename1.ui.layouts.BoxLayout`. The label is deliberately recognizable: it helps you tell your changed app from the original starter.

This follows the screen-construction pattern in the project's [archetype example](https://github.com/codenameone/CodenameOne/blob/master/maven/codenameone-maven-plugin/src/main/asciidoc/mojos/generate-archetype.adoc).

## Run it, then submit a build

Use the project's simulator run action. Check that the screen shows your new label. If the project fails to compile, resolve that error before submitting a cloud build.

For a Maven project, run this from the project root to request an Android device build:

```sh
mvn package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device
```

Authenticate with your BuildCloud account when prompted. Follow the build's status through to its result. Submission alone does not mean the build succeeded. If it fails, read the build log and keep the first actionable error with the command you ran.

The local simulator cannot establish that every device feature behaves correctly. A successful build is another checkpoint; installing and testing the app on a device comes after it. Cloud builds also require connectivity and available build quota.

## Start with that one screen

[Create a project / start building](https://cloud.codenameone.com/register?utm_source=blog&utm_medium=community&utm_campaign=first-build&utm_content=2026-10-20-v1-b1). Create your account, set up a basic app project, and make the label yours. Leave databases, sign-in screens, and backend integration for a later iteration.
