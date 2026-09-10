---
title: "A Drag Is a Clipboard Payload With a Destination"
slug: native-drag-drop-clipboard
url: /blog/native-drag-drop-clipboard/
date: '2026-09-15'
author: Shai Almog
description: "Codename One adds native operating-system drag and drop beside its lightweight API. ClipboardContent supplies formats and lazy data, while native completion and threading preserve platform behavior."
feed_html: '<img src="https://www.codenameone.com/blog/native-drag-drop-clipboard.jpg" alt="Drag It Outside The App" /> Codename One adds native operating-system drag and drop beside its lightweight API. ClipboardContent supplies formats and lazy data, while native completion and threading preserve platform behavior.'
series: ["release-2026-09-11"]
---

![Drag It Outside The App](/blog/native-drag-drop-clipboard.jpg)

A file icon that moves around inside a form is useful. A file the user can drag onto the desktop is a different operation. Until this work, Codename One's lightweight drag and drop stopped at the application boundary.

[PR #5662](https://github.com/codenameone/CodenameOne/pull/5662) adds native operating-system drag and drop beside `setDraggable` and `setDropTarget`. Its payload is the same `ClipboardContent` object used for copy and paste.

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

## Do not generate the export for a canceled drag

A user may begin dragging a report and then drop it nowhere. Eagerly generating a large file on every gesture would make that canceled operation expensive.

`ClipboardContent.setDataProvider` declares a representation whose data can be constructed when a receiver asks for it. `setFiles` and `getFiles` provide one consistent file-list form, and `text/uri-list` supports receivers that use URI lists.

{{< mermaid >}}
sequenceDiagram
    participant User
    participant Source as Source app
    participant OS as Native drag session
    participant Target as Receiving app
    User->>Source: Start drag
    Source->>OS: Offer formats and lazy data providers
    User->>Target: Drop
    Target->>OS: Request supported representation
    OS->>Source: Read requested data
    Source->>Target: Supply content
    OS->>Source: Report completion action
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

On Android, the conversion reuses the clipboard's `ClipData` machinery and file-provider URIs. On iOS, UIKit owns recognition of the gesture. The payload is fetched when the native session requests it, so simply touching the component does not materialize an export file.

## The test boundary

The PR reports passing core and JavaSE tests, including lazy file transfer and MIME conversion, plus native Apple compilation and translation checks. It explicitly does not report a physically driven operating-system drag. Synthetic mouse input did not reach the window server in that environment. Android and iOS paths had compilation and analysis evidence rather than device-driven drag evidence.

That distinction belongs beside the platform matrix. A compiled bridge and a real user moving a document into another application answer different questions.

This feature joins {{< post-link path="/blog/continuity-restoring-work" text="continuity" >}} in letting an application's work cross a boundary while retaining a clear data contract. It also extends the {{< post-link path="/blog/performance-work-between-benchmarks" text="weekly performance theme" >}}: a canceled drag should not build a file, and accepting a drop should not freeze the UI. Shared payloads let the ports improve those behaviors without multiplying application-specific transfer code.

---

## Discussion

_Which of your application's copy operations would be more useful as a drag into another app?_

{{< giscus >}}
