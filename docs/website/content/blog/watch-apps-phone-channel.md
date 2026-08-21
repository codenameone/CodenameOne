---
title: "Watch Apps: One Codebase, Two Real Applications"
slug: watch-apps-phone-channel
url: /blog/watch-apps-phone-channel/
date: '2026-08-22'
author: Shai Almog
description: "Codename One now builds a separate watch application from one watch entry point and connects it to the phone through a portable asynchronous API for messages, state, and files."
feed_html: '<img src="https://www.codenameone.com/blog/watch-apps-phone-channel.jpg" alt="A phone and watch running separate Codename One applications connected by an asynchronous data channel" /> Codename One now builds a separate watch application from one watch entry point and connects it to the phone through a portable asynchronous API for messages, state, and files.'
series: ["release-2026-08-21"]
---

![A phone and watch running separate Codename One applications connected by an asynchronous data channel](/blog/watch-apps-phone-channel.jpg)

A watch app is not a second form in the phone process. It is another application on another device, with its own storage, startup sequence, and periods when the other side is unreachable.

[PR #5487](https://github.com/codenameone/CodenameOne/pull/5487) now builds that application from `codename1.watchMain` on Apple Watch and Wear OS. It also adds one phone-to-watch API that maps to `WCSession` on Apple platforms and the Wearable Data Layer on Android.

This follows the [portable SQLite and weekly release overview](/blog/sqlite-portable-encrypted/). Both changes replace an interface-shaped promise with behavior that runs in the simulator and in port tests.

## One setting creates the second entry point

The watch application starts from a fully qualified class name:

```properties
codename1.watchMain=com.example.MyWatchApp
```

The build derives the watch bundle identifier, deployment target, signing team, and display name from settings the project already has. `codename1.watchStandalone=true` is the only additional choice because a build cannot infer whether the watch is a companion or the product itself.

The phone and watch share source files, resources, CSS, and themes. They do not share runtime state. Each has its own `Storage`, `Preferences`, and SQLite files.

{{< mermaid >}}
flowchart LR
    A[Shared Java source<br/>resources and CSS] --> B[Phone application]
    A --> C[Watch application<br/>watchMain]
    B <-->|sendMessage<br/>live request and reply| C
    B <-->|putData<br/>latest replicated state| C
    B <-->|transferFile<br/>background payload| C
{{< /mermaid >}}

Wear OS reuses the Android port. watchOS uses a separate Core Graphics renderer because it has no UIKit view hierarchy, OpenGL ES, or Metal. The watch runtime sits inside a SwiftUI shell and runs its own ParparVM translation rooted at the watch entry point.

## A message and a value solve different problems

The platforms offer several transports because a watch spends much of its life asleep.

Use `putData()` for state that should converge when the watch next wakes:

```java
WearableConnection.putData(new WearableMessage("/steps")
        .put("count", stepCount)
        .put("goalReached", stepCount >= 10000));
```

Register the listener during `init()`. A payload can be the reason the platform started the process, so listeners attached from a later form may miss the replay window.

```java
WearableConnection.addDataListener(new WearableDataListener() {
    public void dataChanged(WearableMessage data) {
        stepsLabel.setText("" + data.getInt("count", 0));
    }

    public void dataRemoved(String path) {
        stepsLabel.setText("--");
    }
});
```

Each data path holds the latest value. Two rapid writes can arrive as one update. That is correct for a step count and wrong for a queue of events.

Use `sendMessage()` when both applications must be awake and the sender needs an answer now:

```java
if (WearableConnection.isReachable()) {
    WearableConnection.sendMessage(
            new WearableMessage("/workout/start"),
            new WearableReplyHandler() {
                public void replyReceived(WearableMessage reply) {
                    showWorkout(reply.getString("id", null));
                }

                public void replyFailed(String message) {
                    showReplicatedWorkoutState();
                }
            });
}
```

Failure is a normal branch. The phone may be asleep, out of range, or running an older version that does not know the message path. `transferFile()` covers files and large payloads that can arrive later.

## The simulator runs two processes

The **Watch > Launch Watch App** command starts the watch beside the phone. The applications run in separate processes and connect through the desktop bridge, so `sendMessage()` and `putData()` take the same asynchronous route the application code expects on a device.

The simulator includes Apple Watch 41 mm and 45 mm skins, plus round and square Wear skins. Test the round skin even if the first target is Apple Watch. It catches layouts that depend on rectangular corners.

`CN.isWatch()` selects the form-factor-specific UI. The `watch` theme override changes styling without forking the rest of the theme:

```java
Form form = new Form(BoxLayout.y());
if (CN.isWatch()) {
    form.add(new Label("Hi Watch"));
    form.getToolbar().setVisible(false);
} else {
    form.add(new SpanLabel("Welcome to the phone application"));
}
form.show();
```

## Complications reuse the surfaces model

A complication is a small system-rendered surface driven by a timeline. That is the same model Codename One uses for widgets, Live Activities, and Dynamic Island content.

```java
WidgetKind steps = new WidgetKind("steps")
        .setDisplayName("Steps")
        .addSupportedSize(WidgetSize.WATCH_CIRCULAR)
        .addSupportedSize(WidgetSize.WATCH_RECTANGULAR);
```

The watch sizes belong to `WidgetSize` instead of a second complication API. Application content can therefore share the same surface descriptors and timeline logic.

The system targets that render those watch families are not generated yet. watchOS still needs its WidgetKit extension target, and Wear OS still needs complication or tile services. The API establishes the common model without claiming those final platform adapters have shipped.

Android has one more current limit. Standalone Wear applications build today. A companion configuration does not yet produce a second Wear APK beside the phone APK. Apple Watch supports both companion and standalone targets, although standalone App Store submission still needs a manual archive step in Xcode.

## Reuse should survive the platform boundary

The useful reuse here is not pretending the watch and phone are the same screen. They are separate products sharing application rules, visual assets, and surface descriptions. `WearableConnection` then makes the real runtime boundary visible in ordinary Java code.

That distinction matters for write-once-run-anywhere. Reuse is strongest when the common layer expresses what the applications share and leaves different lifecycles intact. A phone message can fail. A replicated value can arrive after a relaunch. A complication can render while neither application is active.

The {{< post-link path="/blog/javascript-dom-text-search" text="next post keeps the Codename One renderer while restoring browser-native text behavior" >}}.

---

## Discussion

_Which watch data do you treat as replicated state, and which operations really need a live reply from the phone?_

{{< giscus >}}
