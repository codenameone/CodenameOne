---
title: "The Process Disappeared. The User\u2019s Work Should Not."
slug: continuity-restoring-work
url: /blog/continuity-restoring-work/
date: '2026-09-15'
author: Shai Almog
description: "Codename One adds persistent state restoration, Apple Handoff, and application-owned relays. Restoration is explicit, and account changes remain an application security boundary."
feed_html: '<img src="https://www.codenameone.com/blog/continuity-restoring-work.jpg" alt="Pick Up The Same Work" /> Codename One adds persistent state restoration, Apple Handoff, and application-owned relays. Restoration is explicit, and account changes remain an application security boundary.'
series: ["release-2026-09-11"]
---

![Pick Up The Same Work](/blog/continuity-restoring-work.jpg)

Keeping the current `Form` in a field works until the operating system kills the process. When the application starts again, the field is gone. The user returns to the first screen even though they were halfway through a task.

[PR #5663](https://github.com/codenameone/CodenameOne/pull/5663) adds `com.codename1.continuity` to preserve that work. It restores a local checkpoint and the router's screen stack, and can offer the same activity to another device.

## Save a description of the work

The router already represents navigation as a stack of paths. Those paths can be serialized and reconstructed without animating through every intermediate screen. A `StateProvider` adds the application data needed to resume the task.

This example assumes `draftField` is the application's TextArea and the routes have already been registered:

```java
Continuity.setStateProvider(new StateProvider() {
    public Map<String, Object> saveState() {
        Map<String, Object> state = new HashMap<String, Object>();
        state.put("draft", draftField.getText());
        return state;
    }

    public void restoreState(Map<String, Object> state) {
        Object draft = state.get("draft");
        draftField.setText(draft instanceof String ? (String) draft : "");
    }
});
```

Registering the provider enables continuity. The saved map should describe reconstructible application state, not retain UI objects or carry credentials to another device. Keep the payload small and decide explicitly whether sensitive draft contents belong in it.

Navigation schedules a checkpoint once per event-loop pass. Data edits that do not navigate can request one too:

```java
Continuity.setTitle("Draft message");
Continuity.checkpoint();
```

Saving continuously avoids making shutdown the only opportunity to save. It also avoids placing all the persistence work into a suspend callback that may be blocking a platform thread.

## Restore after the account is known

Restoration is explicit. The framework cannot decide whether your login flow, encryption setup, or account selection has finished.

```java
import com.codename1.continuity.Continuity;
import com.codename1.router.Navigation;

// Run after authentication and route registration are complete.
if (!Continuity.restore()) {
    Navigation.navigate("/home");
}
```

A restored route identifies work; it does not authorize access to it. The application must still check that the current account can open the document or perform the action named by that route.

{{< mermaid >}}
flowchart LR
    N[Navigation and edits] --> C[Checkpoint]
    C --> L[Local storage]
    C --> H[Apple Handoff]
    C --> R[Application StateRelay]
    L --> A[Authenticate and register routes]
    H --> A
    R --> A
    A --> V[Validate state and restore]
{{< /mermaid >}}

The handoff and relay paths are separate options. They do not turn a local checkpoint into an automatically trusted cloud document.

## The platform boundaries are visible

| Operation | Availability in this work |
| --- | --- |
| Local state and route-stack restoration | iOS, macOS, Android, desktop/simulator, JavaScript |
| Platform device continuation | Apple Handoff; simulator support for testing |
| Platform key-value synchronization | Apple iCloud; simulator support for testing |
| Application-defined cross-device relay | `StateRelay`, backed by your endpoint |

`Continuity.isContinuationSupported()` lets the application check the platform facility. Android does not claim to provide Apple-style Handoff. An application relay can instead use the product's existing accounts to carry state between Android, Apple devices, or a browser.

`RestStateRelay` is the provided HTTP starting point. Codename One does not run a relay service or decide which saved states belong to the same person. Your server owns authentication, account isolation, retention, and conflict policy.

The iCloud key-value store lives in `com.codename1.continuity.sync`. That package requires the appropriate entitlement on the Apple App ID. Basic restoration and Handoff do not acquire that requirement just because the app wants to resume a screen.

## A received activity need not interrupt the current task

An application can turn off automatic restoration with `Continuity.setAutoRestore(false)` and use a continuation listener to offer a choice. Declining should call `Continuity.acknowledge(state)` if the app does not want the same unchanged relay state offered again after every relaunch.

For short-lived workflows, `Continuity.setMaxAge(...)` limits how old a checkpoint may be. A checkout or confirmation screen should not resume indefinitely with stale assumptions about price, availability, or authorization.

## Logout closes the restoration path

Clear saved state and disable arrivals when the account signs out:

```java
Continuity.clear();
Continuity.disable();
```

Both calls matter. Clearing stored state alone leaves continuity enabled, so a later activity could restore the previous account's route over the login screen. Re-enable it only after the next account is ready. The guide's [continuity examples](https://github.com/codenameone/CodenameOne/blob/a3c56579cf/docs/demos/common/src/main/java/com/codenameone/developerguide/continuity/ContinuitySnippets.java) include relay setup, user confirmation, expiry, and logout.

Android task removal handles a different part of ending a session, covered in {{< post-link path="/blog/pem-keys-and-clearing-recents" text="the security follow-up" >}}. Neither operation replaces server-side credential revocation.

## Resume useful work, release the old process

The PR verified core state tests and generated Apple builds, including a single `NSUserActivityTypes` array shared correctly with App Intents. That is source and build evidence; it should not be read as a report of a physical two-device Handoff session. The paired cloud-builder integration also needs to be present in the builder serving your application.

The {{< post-link path="/blog/performance-work-between-benchmarks" text="performance work this week" >}} tries to reduce the memory a running process needs. Continuity handles the case where the operating system reclaims the process anyway. A shared state model lets us address both without asking each app to invent its own restoration protocol or blur the boundary between remembered state and current permission.

---

## Discussion

_What is the smallest checkpoint that would let your user continue after a process restart?_

{{< giscus >}}
