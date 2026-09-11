---
title: "Native Drag and Drop Meets Cross-Device Continuity"
slug: continuity-restoring-work
url: /blog/continuity-restoring-work/
date: '2026-09-15'
author: Shai Almog
description: "Drag files into another application and pick up a task on another device. Codename One adds native drag and drop and cross-device continuity through shared Java APIs."
feed_html: '<img src="https://www.codenameone.com/blog/continuity-restoring-work.jpg" alt="Drag, Drop, And Continue" /> Drag files into another application and pick up a task on another device. Codename One adds native drag and drop and cross-device continuity through shared Java APIs.'
series: ["release-2026-09-11"]
---

![Drag, Drop, And Continue](/blog/continuity-restoring-work.jpg)

Drag a document from your app into another application. Pick up an unfinished task on another device. These are ordinary things users want to do, but until now Codename One's shared APIs did not provide the native drag session or the cross-device checkpoint to carry them through.

This week adds both. [Native drag and drop](#native-drag-and-drop-bring-other-apps-into-the-workflow) connects your components to the OS through the same payload model as copy and paste. [Cross-device continuity](#cross-device-continuity-save-the-work-not-the-screen) preserves application state and the router stack so another process or device can reconstruct the task. The application decides what may travel and which account may open it.

## Cross-device continuity: save the work, not the screen

[PR #5663](https://github.com/codenameone/CodenameOne/pull/5663) adds `com.codename1.continuity` for local restoration, Apple Handoff, and application-owned relays.

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

Android task removal handles a different part of ending a session, covered in {{< post-link path="/blog/android-37-readiness-location-button" text="the security follow-up" >}}. Neither operation replaces server-side credential revocation.

The continuity PR verified core state tests and generated Apple builds, including a single `NSUserActivityTypes` array shared correctly with App Intents. It did not report a physical two-device Handoff session. The paired cloud-builder integration also needs to be present in the builder serving your application.


## Native drag and drop: bring other apps into the workflow

A checkpoint carries a task to a new process or device. A drag carries a document, image, or selection into another application. Both need a payload the receiver can understand without access to the source's live UI objects.

[PR #5662](https://github.com/codenameone/CodenameOne/pull/5662) adds native operating-system drag and drop beside the lightweight `setDraggable` and `setDropTarget` API. Its payload is the same `ClipboardContent` used for copy and paste. The existing in-form drag behavior remains available.

## Reuse the representations you can already copy

A drag target may want plain text, HTML, or a list of files. The source can offer several representations and let the receiver request one it understands. That is already the clipboard's job.

The new API separates that payload from the operation's allowed actions, drag image, and completion. A file source and destination can be configured like this, with `paths` holding existing export paths and `inbox` identifying the receiving component:

```java
Label file = new Label("report.pdf");
file.setNativeDragOperation(NativeDragOperation.createFileDrag(paths));

inbox.setNativeDropTarget(true);
inbox.setAcceptedDropMimeTypes(ClipboardContent.MIME_FILE);
inbox.addNativeDropListener(event -> {
    String[] received = ((NativeDropEvent) event).getFiles();
    if (received != null) {
        queueImport(received);
    }
});
```

`queueImport` belongs to the application. It should validate the input and move expensive parsing off the event dispatch thread. Accepting a file MIME type is a format filter, not proof that a document is safe to parse or trusted to execute.

## A provider can still run when the drag starts

`ClipboardContent.setDataProvider` lets a representation supply its data through a callback. The native port determines when that callback runs. An abandoned drag can still pay for an expensive export.

| Port or representation | When the provider may run |
| --- | --- |
| JavaSE | When a receiver requests the representation |
| Android | At drag start, while building the complete `ClipData` |
| iOS file lists | At drag start, because UIKit needs the item count |

Other iOS representations can be deferred, but code should not treat every representation as lazy. Keep providers cheap enough to run at drag start. Prepare or cache expensive exports before enabling the gesture instead of relying on cancellation to avoid the work.

{{< mermaid >}}
flowchart TD
    C[ClipboardContent with providers] --> P{Native port and representation}
    P -->|Android| A[Resolve providers into complete ClipData]
    A --> D[Start native drag]
    P -->|iOS file list| I[Resolve files to determine item count]
    I --> D
    P -->|JavaSE| J[Advertise formats without resolving data]
    J --> D
    D --> R{Receiver requests data?}
    R -->|JavaSE deferred representation| G[Run provider]
    R -->|Already prepared| V[Use prepared data]
{{< /mermaid >}}

A move adds an ownership decision. The source must wait for native completion before acting on an accepted move. Starting a drag is not confirmation that another application received the bytes, so deleting the source at that point risks data loss.

## The drag callback cannot wait for the wrong thread

Native drops arrive on the platform's drag thread. Codename One resolves the target there using accepted MIME types and actions, then delivers application callbacks on the event dispatch thread.

JavaSE makes the reason concrete. Its event dispatch thread can wait on AWT to present a frame. If an AWT drag callback synchronously waited for the Codename One thread, both sides could wait forever.

Static MIME filters therefore affect the cursor immediately. A decision made later in an application callback can reach the cursor on the following drag event. `canAcceptNativeDrop` is the exception that runs off the Codename One event dispatch thread; implementations must respect that contract rather than treating it like an ordinary UI listener.

## Where native drags work in this release

| Port | Native drag/drop | Crossing into another app |
| --- | --- | --- |
| JavaSE and simulator | Supported | Supported through AWT |
| Android | Supported | Android Nougat and later using a global drag |
| iPadOS and Mac Catalyst | Supported | Supported |
| iPhone | Supported | Not offered by this implementation's interaction model |
| JavaScript, native AppKit, native Windows/Linux | Not implemented here | Not implemented here |

Check `NativeDragAndDrop.isSupported()` before offering a native-only workflow. Unsupported calls are no-ops, and the lightweight drag/drop API continues to work as before. Mac Catalyst and native AppKit are distinct ports; supporting UIKit drag interactions does not automatically implement AppKit dragging.

On Android, the conversion reuses the clipboard's `ClipData` machinery and file-provider URIs. On iOS, UIKit owns recognition of the gesture. Recognition and data preparation are separate steps; once a session begins, the provider timing above applies.

## The test boundary

The PR reports passing core and JavaSE tests, including lazy file transfer and MIME conversion, plus native Apple compilation and translation checks. It explicitly does not report a physically driven operating-system drag. Synthetic mouse input did not reach the window server in that environment. Android and iOS paths had compilation and analysis evidence rather than device-driven drag evidence.

That distinction belongs beside the platform matrix. A compiled bridge and a real user moving a document into another application answer different questions.


## Move the work, keep the account boundary

The {{< post-link path="/blog/performance-work-between-benchmarks" text="performance changes this week" >}} reduce what a running process needs. Continuity lets useful work survive when that process goes away anyway, and native drag and drop lets the user choose another application to receive its output.

The framework now supplies more of the transfer machinery, while the application decides what may cross each boundary. Restore after authentication, check access to the restored route, and validate incoming files before parsing them. An old checkpoint does not grant access, and a file offered by another app is still external input. Those rules make continuity and native integration useful without quietly weakening the security of the application using them.

---

## Discussion

_What should your users be able to carry to another device or application without starting over?_

{{< giscus >}}
