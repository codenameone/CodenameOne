---
title: "The Codename One JavaScript Port Is Now Free and Open Source"
slug: javascript-free-open-source
url: /blog/javascript-free-open-source/
date: '2026-07-24'
author: Shai Almog
description: "Codename One's JavaScript port is now open source, available on every plan, and buildable locally without an account. ParparVM is the default, while TeaVM remains available through a build hint."
feed_html: '<img src="https://www.codenameone.com/blog/javascript-free-open-source.jpg" alt="The Codename One JavaScript port is free and open source" /> Codename One''s JavaScript port is now open source, available on every plan, and buildable locally without an account.'
series: ["release-2026-07-24"]
---

![The Codename One JavaScript Port Is Now Free and Open Source](/blog/javascript-free-open-source.jpg)

The Codename One JavaScript port is now open source and available on every plan, including Free. You can also build the JavaScript target locally without a Codename One account.

[PR #5423](https://github.com/codenameone/CodenameOne/pull/5423) makes our ParparVM-based port the default and moves its source under the same GPLv2 with Classpath Exception license as the rest of Codename One. TeaVM remains available as a fallback.

## TL;DR

- [JavaScript](#why-we-built-the-parparvm-javascript-port): The ParparVM JavaScript port is now open source, free on every plan, and buildable locally without an account.
- [Android API level 36](#android-api-36-becomes-the-default-next-week): API 36 becomes the default next week, and you can test or temporarily override it with build hints.
- [ParparVM garbage collection](#issue-5425-dave-was-right-about-our-collector): A real application exposed a benchmark gap, redundant collection cycles, and a broken first fix.
- [Calendar](#calendar-api-local-calendars-cloud-sync-and-conflicts): The new API covers device and cloud calendars, recurrence, tasks, incremental sync, offline mutations, and conflict handling.
- [Bluetooth](#bluetooth-is-now-part-of-the-core): Bluetooth moved into the core with BLE, GATT, L2CAP, RFCOMM, Web Bluetooth, desktop radios, and a scriptable simulator.
- [Text editing](#pure-codename-one-text-editing): `EditField`, `RichTextArea`, and `CodeEditor` can edit and paint text without placing a native field over the component. The new clipboard model carries rich text, images, and file references alongside a plain-text fallback.
- [Rich text](#lightweight-rich-text-without-a-web-view): `RichTextComponent` renders HTML, Markdown, AsciiDoc, RTF, and styled Java runs without a web view.
- [Compact strings](#compact-strings-in-parparvm): Strings now use a `byte[]` when every character fits, cutting their character storage in half without adding a second backing-array pointer.
- [Account, website, and videos](#account-website-and-video-updates): The authorization migration is complete, the redesigned site is live, and the weekly posts now generate explainer videos.

## Why we built the ParparVM JavaScript port

Our original web port used [TeaVM](https://teavm.org/), which is a fantastic project. TeaVM took Java bytecode, supported the threading model Codename One needed, and produced a browser application at a time when that combination looked close to impossible.

ParparVM already powered other Codename One targets when we built a second JavaScript route on top of it. If we had understood how much work this would require, we might have spent that effort improving the TeaVM integration instead. The finished port gives us direct control over the VM, runtime, browser integration, and test infrastructure.

VM-level APIs can now reach the web target through the same translator and Java runtime work used by our native ports. We no longer need a separate implementation in a third-party runtime before the browser can use a new VM API.

Shared framework and runtime changes also trigger the ParparVM browser suite. Pull requests render and compare the Codename One test application in Chromium. A nightly job starts generated applications in Chromium, Firefox, and WebKit.

The port also keeps Java and browser code separate:

The old route ran translated Java beside its JavaScript integration on the browser's main thread. The ParparVM route puts the translated Java application in a background Web Worker instead. The Java code has no `document`, no DOM, and no dependency on global browser objects.

A thin message bridge connects the worker to the standard front-end `JavaScriptPort`, which runs on the browser's main thread. The port handles the canvas, DOM, clipboard, media, Web Bluetooth, and other browser APIs. Application-specific browser code stays under `native/JavaScript`.

Browser APIs still impose browser rules. Web Bluetooth requires HTTPS and a user gesture, for example. Those requirements stay in the port layer instead of affecting ordinary Java code.

## Build the web target without our cloud

Generated Maven projects now include a local JavaScript command:

```bash
mvn -pl javascript package \
  -Dcodename1.platform=javascript \
  -Dcodename1.buildTarget=local-javascript
```

The build uses the bundled ParparVM compiler and JavaScriptPort resources. It produces a static browser application on your machine. The same command is wired into generated IntelliJ, Eclipse, NetBeans, and VS Code projects.

Cloud builds also use ParparVM by default. If you encounter a regression, compare it with the original TeaVM route by adding one build hint:

```properties
javascript.port=teavm
```

The full selector is:

| Build hint | Result |
| --- | --- |
| `javascript.port=parparvm` | Current JavaScriptPort builder and the default. |
| `javascript.port=teavm` | Original TeaVM builder for compatibility and diagnosis. |

TeaVM is not being removed. It remains useful for compatibility testing while ParparVM gains more production use.

## The proxy is part of deployment now

A browser application can call its own origin directly. Calls to another origin depend on that server's CORS policy. Codename One has traditionally handled the second case with a servlet proxy bundled into the TeaVM WAR.

That proxy came from an older Java EE stack. It calls a servlet API removed from current Jakarta containers, so changing `javax.servlet` imports to `jakarta.servlet` would not fix it.

The ParparVM builder now creates a proxy wrapper for the deployment target you choose:

```properties
javascript.proxy.target=cloudflare-workers
javascript.proxy.allowedTargets=https://api.example.com,*.services.example.org
```

Available targets are `jakarta-servlet`, `javax-servlet`, `node`, `php`, `aws-lambda`, `google-cloud-functions`, `cloudflare-workers`, and `none`. Jakarta Servlet is the default.

{{< mermaid >}}
flowchart TB
    A["Codename One web app requests /cn1-cors-proxy"] --> B["Same-origin proxy checks the URL scheme and allowlist"]
    B --> C["Proxy forwards the request to the approved remote API"]
    C --> D["Remote API returns status, headers, and body"]
    D --> E["Proxy returns a browser-readable response to the app"]
{{< /mermaid >}}

Set `javascript.proxy.allowedTargets` before public deployment. An omitted allowlist produces a build warning because an open forwarding proxy is not a safe default for production. You can also point at an existing proxy with `javascript.proxy.url` or disable generation with `javascript.proxy.target=none`.

## Why we are making JavaScript available to everyone

JavaScript builds were previously an Enterprise feature and a significant reason to purchase that plan. We expect this change to reduce some of that revenue. For a small company, that matters, and this was a difficult decision for me to make.

Paid plans continue to fund the cloud build machines, push service, Crash Protection, support, version retention, and the people doing this work. If your team relies on the paid services, keep supporting the work through the plan that fits your needs.

We do not know whether wider adoption will replace the revenue we lose. We do know that making the port easier to use and test is the right technical direction.

## Android API 36 becomes the default next week

Some developers are already receiving Google Play notices about Android 16 and API level 36. [Issue #4466](https://github.com/codenameone/CodenameOne/issues/4466) gave us time to test edge-to-edge behavior, resizing, permissions, and native integrations before changing the default.

Try the new target now:

```properties
android.targetSDKVersion=36
```

Please send a build and exercise the parts of your application that touch system UI, background behavior, Bluetooth, notifications, and native libraries. Once 36 becomes the default, you can temporarily pin the previous toolchain while a regression is investigated:

```properties
android.buildToolsVersion=35
android.targetSDKVersion=35
```

Issues like #4466 are how a small team keeps pace with Apple, Google, browser vendors, desktop platforms, and hardware changes. If you see a platform announcement that could affect Codename One, open an issue early. A short warning months ahead is far cheaper than an emergency migration after a store deadline.

## Issue #5425: Dave was right about our collector

In [issue #5425](https://github.com/codenameone/CodenameOne/issues/5425), Dave reported an iOS workload that took roughly five minutes while the same code finished in seconds on Android and on the older ParparVM collector. I focused on the allocation-heavy application code and dismissed the regression. Dave reduced the temporary allocation, kept measuring, and showed that my explanation did not fit the result.

He was right. Our recent BiBOP collector work made short-lived small objects much cheaper, but the benchmark set did not cover a large retained population followed by larger allocations. The fixed trigger could run collection far too often even when the retained set gave it little to reclaim.

[PR #5436](https://github.com/codenameone/CodenameOne/pull/5436) added an issue-shaped benchmark and adaptive pacing. The first fix also introduced a fresh-object tracing bug that could free a live child and leave its parent pointing at reused memory. Dave's repeated build against `master` exposed corrupted dictionary words and an impossible null pointer. [PR #5442](https://github.com/codenameone/CodenameOne/pull/5442) fixed that correctness regression and added a focused audit.

Versioned builds against the current development head made that feedback loop possible:

```properties
build.cn1Version=master
```

Dave could test each collector revision in his real application while we changed it.

Thanks, Dave. I needed the bump on the head.

## Calendar API: local calendars, cloud sync, and conflicts

> **TL;DR:** `com.codename1.calendar` provides one API for device calendars, Google, Microsoft, CalDAV, and `.ics` files. It includes recurrence, tasks, provider versions, incremental sync, offline mutation queues, and conflict handling.

![Calendar API with local and cloud synchronization](/blog/calendar-is-not-add-event.jpg)

A request to add an event often grows into recurrence, edits from another device, offline changes, and a stale write that could overwrite a newer provider copy. The new API keeps those cases in one model and exposes the operations supported by each source.

```java
LocalCalendarSource source = LocalCalendarSource.getInstance();
CalendarCapabilities capabilities = source.getCapabilities();
if (capabilities.supports(CalendarCapability.READ_EVENTS)) {
    source.queryEvents(query).ready(page ->
            page.getItems().forEach(System.out::println));
}
```

The simulator uses an isolated in-memory calendar, so tests never touch the developer's real schedule. [PR #5413](https://github.com/codenameone/CodenameOne/pull/5413) contains the implementation. {{< post-link path="/blog/calendar-is-not-add-event" text="Read the calendar API article for recurrence, OAuth boundaries, conflict resolution, and provider synchronization." >}}

## Bluetooth is now part of the core

> **TL;DR:** The new core Bluetooth API supports every Codename One target, including JavaScript. It covers BLE central and peripheral roles, GATT, L2CAP streams, classic RFCOMM, browser Web Bluetooth, real desktop radios, and a scriptable simulator.

![Bluetooth simulator with a virtual device tree and characteristic editor](/blog/bluetooth-beyond-ble/bluetooth-simulator-devices.png)

Bluetooth is larger than BLE scanning. A medical sensor might need notifications over GATT. A scanner or printer might use classic RFCOMM. Tests need to reproduce a failed connection or a callback that never arrives.

The API reports capabilities instead of pretending every role works everywhere. Browsers provide central GATT through a user-controlled chooser. iOS does not expose arbitrary RFCOMM. Applications that do not reference Bluetooth receive none of its permissions or native code.

[PR #5399](https://github.com/codenameone/CodenameOne/pull/5399) is a ground-up replacement for the Cordova-derived cn1lib. {{< post-link path="/blog/bluetooth-beyond-ble" text="Read the Bluetooth article for the role model, platform matrix, GATT queue, simulator, and builder behavior." >}}

## Pure Codename One text editing

> **TL;DR:** `EditField`, `RichTextArea`, and `CodeEditor` can now edit and paint text entirely inside the Codename One component layer. The operating system supplies keyboard and input-method operations without placing a native field over the component.

![Rich text and code editors painted by the lightweight editing engine](/blog/text-input-without-native-overlay/editors-overview.png)

Codename One traditionally places a native text field over a lightweight field while the user edits. That route remains the default for `TextField` and `TextArea`. It works well for ordinary forms, but it cannot paint syntax colors, inline objects, application-defined masks, or custom selections inside the Codename One surface.

The new `TextInputClient` contract sends semantic operations such as `commitText(...)`, `setComposingText(...)`, and `deleteSurroundingText(...)`. The portable editor owns the document, caret, selection, bidirectional layout, undo state, and pixels.

```java
EditField notes = new EditField();
notes.setSingleLineTextArea(false);
notes.setRows(5);
form.add(notes);
```

This work also replaces the text-only clipboard assumption. A `ClipboardContent` can carry several representations of one item: plain text, HTML, RTF, Markdown, AsciiDoc, PNG, JPEG, GIF, and file references. The port publishes the formats its system clipboard supports while retaining plain text for older code and applications that do not understand the richer payload.

`RichTextArea` uses that negotiation directly. Copying a formatted selection publishes plain text, HTML, RTF, Markdown, and AsciiDoc together. Pasting chooses the richest text representation it understands, while a clipboard image becomes an inline image instead of a filename or discarded payload.

[PR #5386](https://github.com/codenameone/CodenameOne/pull/5386) adds the port bindings and portable editor engine. {{< post-link path="/blog/text-input-without-native-overlay" text="Read the text-input article for composition, UTF-16 offsets, bidirectional hit testing, rich clipboard negotiation, and the native-overlay tradeoff." >}}

## Lightweight rich text without a web view

> **TL;DR:** `RichTextComponent` renders HTML, Markdown, AsciiDoc, RTF, and styled Java runs inside ordinary Codename One layouts. It shares its document model and painter with `RichTextArea` but has no editing session or browser peer.

![Rich text with headings, emphasis, and a list](/blog/rich-text-without-webview/editors-richtext.png)

A `SpanLabel` gives wrapped text one style. A `BrowserComponent` provides a complete browser. `RichTextComponent` fills the space between them for formatted application content.

```java
RichTextComponent view = new RichTextComponent();
view.setMarkdown("# Trip summary\n\n"
        + "Departs **09:40**. See the [itinerary](app://itinerary).");
form.add(view);
```

The component measures its content inside the Codename One layout. Link handling, image loading, authentication, and caching remain under application control. The HTML importer handles document markup but does not execute scripts or implement a CSS page layout.

[PR #5421](https://github.com/codenameone/CodenameOne/pull/5421) contains the viewer and shared painter. {{< post-link path="/blog/rich-text-without-webview" text="Read the rich-text article for supported formats, link and image policies, clipboard negotiation, and when to keep BrowserComponent." >}}

## Compact strings in ParparVM

> **TL;DR:** Strings now store their characters in a `byte[]` when every character fits. Text that needs wider code units continues to use `char[]`. This halves the character storage for common strings without adding a second array pointer to every `String`.

![Compact strings cut character storage in half](/blog/compact-strings-parparvm.jpg)

URLs, JSON keys, class names, numbers, log messages, and much Western European text need one byte per character. ParparVM previously stored all of them in a two-byte `char[]`.

```java
String compact = "Résumé 2026"; // byte[] backing
String wide = "שלום";           // char[] backing
```

The implementation keeps one `Object value` field whose concrete array type identifies the representation. The translator also preserves fused allocation, where the `String` fields and backing storage occupy one BiBOP block. Application code does not change.

{{< post-link path="/blog/compact-strings-parparvm" text="Read the compact-strings article for the object layout, fused-allocation changes, native-operation audit, and Unicode behavior." >}}

## Account, website, and video updates

### Authorization migration

We finished the account authorization migration started last week. We shut the old authorization server down too early, brought it back while we fixed the remaining paths, then shut it down again.

You may need to sign in again the next time you send a build. If a fresh login does not work, contact us through the website chat. It is still the fastest route to the team.

### New website design

[PR #5402](https://github.com/codenameone/CodenameOne/pull/5402) shipped a new website design and rewrote several core pages. Tell us what works, what gets in the way, and what we should improve.

### Videos generated from these posts

The [Codename One YouTube channel](https://www.youtube.com/@CodenameOne) now has explainer videos generated from these posts. [PR #5441](https://github.com/codenameone/CodenameOne/pull/5441) adds the video builder, written in Codename One, that turns a reviewed script into timed scenes, narration, code, subtitles, and a final video.

Video editing is not my cup of tea. The generated voice does not bother me, and removing hours of narration and timeline work means we can cover more of what ships. The question is whether those videos help you. Should we invest in this format and use it to replace the aging courses, or would you rather see fewer videos with human narration? Please tell us.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
