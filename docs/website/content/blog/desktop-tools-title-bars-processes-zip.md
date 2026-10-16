---
title: "Give Your Desktop App a Useful Title Bar and Real Tools"
slug: desktop-tools-title-bars-processes-zip
url: /blog/desktop-tools-title-bars-processes-zip/
date: '2026-10-20'
author: Shai Almog
description: "Put application controls in native desktop title areas, launch tools with the shared Process API, and stream ZIP exports using familiar Java APIs."
feed_html: "<img src=\"https://www.codenameone.com/blog/desktop-tools-title-bars-processes-zip.jpg\" alt=\"A desktop application combines title controls with process and archive tools\" /> Put application controls in native desktop title areas, launch tools with the shared Process API, and stream ZIP exports using familiar Java APIs."
series: ["release-2026-10-16"]
---

![A desktop application combines title controls with process and archive tools](/blog/desktop-tools-title-bars-processes-zip.jpg)

A document editor needs room for the document. Giving an entire row to a title, then adding another row for Save and Search, spends that room quickly.

You can now put Codename One components into the title area of native macOS, Windows and Linux apps. It's a niche capability until you're building an editor. Then the details of window dragging and button hit testing become part of your product.

This [weekly follow-up](/blog/create-an-uber-clone-in-1-minute/) also covers two less visible desktop capabilities: launching external tools and reading or writing ZIP streams. Together they make it easier to build an application that fits into the user's existing workflow.

[![Native macOS editor title area with Save, Undo, document status and command search beside the OS window controls](/blog/release-2026-10-16/native-editor-titlebar.jpg)](/blog/release-2026-10-16/native-editor-titlebar.jpg)

*Our native macOS editor prototype, cropped to its title area and ribbon. Save, Undo, document status and command search use the top row; the document begins below the ribbon.*

## Put Save where the user is already looking

Use `desktop.titleBar=custom`, then supply a title component through the ordinary toolbar API. This is the [guide's editor example](https://github.com/codenameone/CodenameOne/blob/master/docs/demos/common/src/main/java/com/codenameone/developerguide/snippets/DesktopTitleContent.java):

```java
public static Form createEditor(Command save, Command search) {
    Display.getInstance().setProperty("desktop.titleBar", "custom");
    Toolbar.setGlobalToolbar(true);
    Form editor = new Form("Quarterly report", new BorderLayout());
    Container title = new Container(new BorderLayout());
    title.add(BorderLayout.WEST, new Button(save));
    title.add(BorderLayout.CENTER, new Label("Quarterly report"));
    title.add(BorderLayout.EAST, new Button(search));
    editor.getToolbar().setTitleComponent(title);
    return editor;
}
```

Call it from the application's normal lifecycle, after framework initialization and before constructing other forms that need this mode. The `Command` arguments are your actions. You can also select the mode with `codename1.arg.desktop.titleBar=custom` in the build settings.

Don't add fixed padding for macOS traffic lights or Windows caption buttons. The framework reserves space for the real OS controls, accounting for changes such as window DPI. Your components stay in the Java component tree while the port handles window gestures.

| Native desktop | Who owns the window behavior? |
| --- | --- |
| macOS | AppKit keeps the traffic lights, resizing and system menu. Empty title space follows native dragging and the user's double-click preference. |
| Windows | The Desktop Window Manager keeps caption controls. Native hit testing handles empty title space and resize edges. |
| Linux | A GTK header bar places rendered application content beside real window controls and follows desktop decoration settings. |

Buttons, editable fields and other interactive descendants keep their input. Disabled buttons are excluded from dragging too. Plain labels and passive container backgrounds can move the window. For a custom painted background, `Toolbar.setTitleBarDragRegion(component, true)` opts it into dragging while preserving interactive children.

{{< mermaid >}}
flowchart TD
    Pointer[Pointer in the title area] --> Target{What is under it?}
    Target -->|Application control| Java[Deliver component input]
    Target -->|Passive title background| OS[Native window gesture]
    Target -->|Window control or edge| Native[OS command or resize]
{{< /mermaid >}}

There are platform differences. Windows custom mode omits the separate Win32 menu row; provide visible access through your toolbar, side menu or ribbon. Linux keeps its menu row below the header, and macOS keeps the system menu bar. This applies to the main form in native desktop builds. Separate dialogs retain their decorations, and browser pages don't gain control of browser chrome.

The [desktop integration guide](/developer-guide/desktop-integration/) has the full contract, introduced in [PR #6019](https://github.com/codenameone/CodenameOne/pull/6019). The useful feature is the complete interaction, including the parts that keep a click on Search from moving the window.

## Call the tool your user already has

We originally wanted a Process API for backend work. It made sense for desktop applications too: an editor can invoke a converter, inspect a tool's version or run an existing command-line utility.

The shared classes are `com.codename1.io.ProcessBuilder` and `com.codename1.io.Process`. Check support before offering the action:

```java
boolean canRunTools = com.codename1.io.ProcessBuilder.isSupported();
```

The API works in JavaSE desktop builds, the native Windows/macOS/Linux ports, the JVM backend and the packaged Linux ParparVM backend. Android, iOS and the browser don't support process launching through this API.

Here is the launch portion of a worker task, assuming `toolPath` names an installed executable:

```java
try (com.codename1.io.Process child =
        new com.codename1.io.ProcessBuilder(toolPath, "--version")
                .redirectErrorStream(true)
                .start()) {
    child.getOutputStream().close(); // This command needs no stdin.
    // Start a reader for child.getInputStream() before waiting.
    // Apply a timeout, inspect the exit code, and handle the output.
}
```

This is a launch excerpt, not a complete runner. The [complete guide example](/developer-guide/processes/) drains output on a separate thread and uses a timed wait. Waiting first can deadlock if the child fills its output pipe. Keep both process I/O and waits off the UI event thread.

Arguments are passed literally; the API doesn't insert a shell. Working directories are native path strings. File redirection, pipelines and `ProcessHandle` aren't part of this subset. Closing the process requests termination and closes its streams, but doesn't kill descendants or wait for exit. [PR #6010](https://github.com/codenameone/CodenameOne/pull/6010) contains the implementation.

## Give users an archive they can use

ParparVM now implements the streaming subset of `java.util.zip`. That includes ZIP entry streams, GZIP, deflate/inflate and checksums. JVM and Android builds use their platform implementations; ParparVM uses the existing Java compression engine.

Writing a small UTF-8 entry looks familiar. Here `destination` is the output stream your caller supplies; closing the ZIP also closes it:

```java
try (java.util.zip.ZipOutputStream zip =
        new java.util.zip.ZipOutputStream(destination)) {
    zip.putNextEntry(new java.util.zip.ZipEntry("report.txt"));
    zip.write("Completed rides\n".getBytes("UTF-8"));
    zip.closeEntry();
}
```

A project export has an extra requirement: its launcher should still be executable after extraction. `ZipArchiveOutputStream` adds a permission argument to the familiar entry operation:

```java
ZipEntry launcher = new ZipEntry("project/run.sh");
launcher.setTime(0);
zip.putNextEntry(launcher, 0755);
// Copy the launcher bytes to zip, then call closeEntry().
```

Here `zip` is a `com.codename1.io.zip.ZipArchiveOutputStream`. The [server download example](/developer-guide/backend-operations/#backend-zip) streams directly to an HTTP response and includes the full copy loop. This avoids building an entire archive in memory, although entry metadata still takes space.

This isn't every Java SE archive feature. ParparVM's subset excludes random-access `ZipFile`, ZIP64 and encrypted archives. Reading to the end of an entry verifies its CRC and size; closing an archive early doesn't verify unread entries. An importer still needs to validate paths and bound extracted data. [PR #6006](https://github.com/codenameone/CodenameOne/pull/6006) introduced the streaming support.

A useful title area, an external tool and an export users can open are modest features individually. They are also the things that decide whether a desktop app can become part of someone's daily work.

## Discussion

_What would you put in your app's title area if the window controls were already handled?_

{{< giscus >}}
