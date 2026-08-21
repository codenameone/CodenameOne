---
title: "App Intents: One Java Declaration for Siri, Spotlight, and Shortcuts"
slug: app-intents-siri-spotlight-shortcuts
url: /blog/app-intents-siri-spotlight-shortcuts/
date: '2026-08-27'
author: Shai Almog
description: "Codename One App Intents generate reflection-free handlers for Siri, Spotlight, Shortcuts, Android launcher shortcuts, widgets, and an application command layer from Java annotations."
feed_html: '<img src="https://www.codenameone.com/blog/app-intents-siri-spotlight-shortcuts.jpg" alt="One Java app-intent declaration branching to Siri, Spotlight, Shortcuts, Android, and widgets" /> Codename One App Intents generate reflection-free handlers for Siri, Spotlight, Shortcuts, Android launcher shortcuts, widgets, and an application command layer from Java annotations.'
series: ["release-2026-08-21"]
---

![One Java app-intent declaration branching to Siri, Spotlight, Shortcuts, Android, and widgets](/blog/app-intents-siri-spotlight-shortcuts.jpg)

Siri, Spotlight, the Shortcuts app, an Android launcher shortcut, and a widget button all need the application to describe an action and run it later. The operating-system surfaces differ. The application capability does not.

[PR #5559](https://github.com/codenameone/CodenameOne/pull/5559) adds `com.codename1.intents` and build-time annotations for declaring that capability once. The build projects it onto the native surfaces each target supports.

This closes the [weekly release series that began with portable encrypted SQLite](/blog/sqlite-portable-encrypted/). Both features rely on generated or tested platform adapters while keeping the application contract in Java.

## A handler is a public static method

An app intent can run in a process the system started only to answer it. There may be no application object and no visible form.

```java
@AppIntent(value = "log_workout", title = "Log a workout",
        description = "Records a completed workout",
        phrases = {"Log a workout in ${applicationName}"},
        headless = true, timeoutSeconds = 5)
public static IntentResult logWorkout(
        @IntentParam(value = "kind", title = "What kind of workout?",
                options = {"run", "ride", "swim"}) String kind,
        @IntentParam(value = "minutes", title = "How many minutes?") int minutes) {
    int total = Preferences.get("totalMinutes", 0) + minutes;
    Preferences.set("totalMinutes", total);
    return IntentResult.value(String.valueOf(total))
            .withDialog("Logged " + minutes + " minutes.");
}
```

The method is static because the build generates a direct call. Runtime annotation lookup is unavailable in translated iOS code, and dead-code elimination removes methods with no Java caller. A reflection-based dispatcher could compile successfully and disappear from the shipped application.

{{< mermaid >}}
flowchart TD
    A[Java annotations<br/>AppIntent and IntentEntity] --> B[Maven bytecode processor]
    B --> C[Reflection-free Java dispatch table]
    B --> D[iOS App Intents and Spotlight declarations]
    B --> E[Android launcher and dynamic shortcuts]
    C --> F[Intents.invoke on every port]
    D --> C
    E --> C
    G[Simulator App Intents window] --> C
{{< /mermaid >}}

The Maven plugin validates declarations and generates native metadata during the build. A malformed phrase, missing entity query, or invalid route fails the build instead of producing a shortcut that silently vanishes.

## Intents and routes answer different questions

A route maps a URL to a screen. An intent accepts typed parameters, can ask the user to choose an entity, and returns a result. A route has no return channel.

They meet when the intent should open a screen. `opensRoute` passes bound parameters through the existing route table:

```java
@AppIntent(value = "show_workout", title = "Show a workout",
        phrases = {"Show my ${workout} in ${applicationName}"},
        opensRoute = "/workouts/{workout}")
public static IntentResult showWorkout(
        @IntentParam(value = "workout", title = "Which workout?")
        Workout workout) {
    return IntentResult.ok();
}

@Route("/workouts/:id")
public static Form workoutForm(@RouteParam("id") String id) {
    return buildWorkoutForm(id);
}
```

The `{workout}` placeholder expands to the selected entity's stable ID before the router matches `/workouts/:id`. The screen keeps one address. A deep link and a system intent reach it through the same router.

## Entities let the platform ask which object you meant

An entity parameter represents an application noun such as a workout, playlist, or invoice. The platform can search those entities and ask the user to choose before the handler runs.

```java
@IntentEntity(value = "workout", title = "Workout", indexed = true)
public static class Workout {
    @EntityId
    public String getId() { return id; }

    @EntityTitle
    public String getName() { return name; }

    @EntityQuery(EntityQuery.Kind.BY_ID)
    public static Workout byId(String id) {
        return WorkoutStore.find(id);
    }

    @EntityQuery(EntityQuery.Kind.SEARCH)
    public static List<Workout> matching(String query) {
        return WorkoutStore.search(query);
    }
}
```

The `BY_ID` query is required because an entity crosses the native boundary as its identifier. The identifier must survive releases and data reordering. A list position or content hash can later resolve to the wrong object without producing an error.

`Intents.index(...)` publishes entities to device search. Remove them when the underlying content disappears, or stale results remain visible after the application data is gone.

## Headless means no window

A headless handler can use `Storage`, `Preferences`, `Database`, networking, logging, surfaces publishing, and indexing. It cannot use `Form`, `Dialog`, the camera, capture, or anything else that needs a window.

Platform-dispatched handlers do not run on the event dispatch thread. A foreground handler that needs to update the UI must use `Display.callSerially(...)`. `Intents.invoke(...)` is synchronous and runs on the calling thread, which makes it useful as an internal command layer but also means a long handler can freeze the UI if invoked from the event thread.

Each invocation has a deadline. Cancellation is cooperative through `IntentContext.isCancelled()`. Commit durable changes as the work proceeds because a result returned after the platform deadline can be discarded.

## Android receives the surfaces it can support

Android has no equivalent to Siri's typed invocation contract and spoken result channel. The API does not label launcher shortcuts as voice parity.

| Capability | iOS | Android |
| --- | --- | --- |
| Declared intent | App Intent and App Shortcut | Launcher shortcut |
| Voice phrases | Siri | Not available |
| Entity disambiguation | System picker | Application picker |
| Device search | Core Spotlight | Long-lived launcher shortcuts |
| Donation | Learned suggestions | Dynamic shortcuts |
| Headless execution | Background application launch | Service without an activity |

Branch on `Intents.isVoiceInvocationSupported()` before telling a user to speak to the application. `areIntentsSupported()` answers a broader question.

`Intents.invoke(...)` works on every port because it calls the generated Java dispatcher. A desktop or web application can use the same intent as an internal command even when the operating system provides no external intent surface.

## System integrations stay out when unused

An application that never references `com.codename1.intents` gets no native intent plumbing, shortcut resources, frameworks, manifest entries, or deployment-target change.

Spotlight indexing and donation use older Objective-C APIs and do not raise the iOS deployment target. Declaring an `@AppIntent` generates the newer Swift declarations. `ios.intents.appIntents=false` keeps indexing and donation while suppressing those declarations. `ios.intents.minDeploymentTarget` lets the project choose whether an intent contributes a floor.

The simulator's **Simulate > App Intents** window lists the generated declarations, builds parameter forms, fills entity pickers from the real query methods, and invokes the same dispatch table used by the native ports.

This release is another form of surface reuse. A workout action remains one piece of application logic while Siri, Spotlight, a widget, Android, and the application's own UI decide how to expose it. The platform integration is native. The behavior and tests stay with the shared code.

Return to [the parent post](/blog/sqlite-portable-encrypted/) for the database, watch, web, smart-home, camera, and security work that shipped with it.

---

## Discussion

_Which application action should be callable from the system without opening its main screen?_

{{< giscus >}}
