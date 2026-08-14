---
title: "The Third-Generation GUI Builder: One Workspace for Every Form"
slug: third-generation-gui-builder
url: /blog/third-generation-gui-builder/
date: '2026-08-14'
author: Shai Almog
description: "Codename One's third-generation GUI Builder keeps the guided layout work from the previous editor and rebuilds the workflow around Maven projects, live CSS, protected Java regions, and fast switching between forms."
feed_html: '<img src="https://www.codenameone.com/blog/third-generation-gui-builder.jpg" alt="A visual editor connecting project forms, CSS, Java source, and a live Codename One canvas" /> Codename One&#39;s third-generation GUI Builder keeps the guided layout work from the previous editor and rebuilds the workflow around Maven projects, live CSS, protected Java regions, and fast switching between forms.'
series: ["release-2026-08-14"]
---

![A visual editor connecting project forms, CSS, Java source, and a live Codename One canvas](/blog/third-generation-gui-builder.jpg)

We have rebuilt the Codename One GUI Builder again. This is its third generation. The interesting part is not another drag-and-drop surface. It is what we kept, what Maven broke, and why a visual editor must understand the whole project instead of opening one generated form at a time.

This is a smaller rewrite than the second generation Steve Hannah built. His guided layout work remains the foundation. [PR #5523](https://github.com/codenameone/CodenameOne/pull/5523) replaces the shell around it with a Maven-first Codename One application that moves between forms, CSS, and Java without leaving the workspace.

## This week in one page

- [The third-generation GUI Builder](#three-builders-three-different-projects) is now a project workspace launched with `mvn cn1:guibuilder`.
- [App Hardening](/blog/app-hardening-cross-platform/) applies renaming, string encryption, and selected control-flow transforms before the platform builds split. The follow-up publishes Saturday.
- [The iOS on-device debugger](#a-breakpoint-no-longer-dereferences-a-random-local) no longer crashes when a reused local slot is mistaken for an object.
- [Port status and Linux video](#a-skipped-test-is-not-a-pass) now report skipped tests instead of counting them as passes. The same work fixed three GStreamer defects.
- [Google Sign-In](#google-sign-in-now-runs-in-an-arm64-simulator) moved to version 7.1 so Apple silicon simulators get a real arm64 simulator slice.
- [ParparVM memory reclamation](#small-object-pages-can-return-to-the-os) now returns surplus BiBOP pages to the operating system after a small-object peak.
- [Missing and legacy URLs](#a-retired-url-now-has-somewhere-useful-to-go) now lead to recovery pages instead of a bare 404.

## Three builders, three different projects

The first GUI Builder grew out of tooling we built at Sun Microsystems. It stored forms inside the resource file and generated a central state machine. That model made sense when Codename One still targeted feature phones with roughly 2 MB of RAM.

The second generation separated a form into readable `.gui` XML and companion Java. Steve built auto layout on top of `LayeredLayout`, including smart insets, sibling references, matching sizes, baseline alignment, and multi-selection. It was a much better model for a modern application, but the editor still opened from a selected form.

That last assumption became painful after the Maven migration. A Maven project has forms under `src/main/guibuilder`, Java under `src/main/java`, and styling in `src/main/css/theme.css`. Launching a separate editor for every generated form turned navigation into the slow part of a visual tool.

{{< mermaid >}}
flowchart LR
    A[Generation 1<br/>resource file and state machine] --> B[Generation 2<br/>one .gui form and guided layout]
    B --> C[Generation 3<br/>one Maven project workspace]
    C --> D[Forms]
    C --> E[Live CSS]
    C --> F[Protected Java regions]
    C --> G[Responsive canvas]
{{< /mermaid >}}

The third generation changes the unit of work from a file to a project. It scans every `.gui` file, keeps them in the left panel, renders the selected form in the center, and shows its properties, layout, and events on the right.

![The third-generation GUI Builder with project forms, component palette, live canvas, and inspector](/blog/third-generation-gui-builder/workspace.png)

## One Maven goal opens the project

Create a form and open the editor from the project root:

```bash
mvn cn1:create-gui-form -DclassName=com.example.ProfileForm
mvn cn1:guibuilder -DclassName=com.example.ProfileForm
```

The first goal creates the pair that belongs in version control:

```text
common/src/main/guibuilder/com/example/ProfileForm.gui
common/src/main/java/com/example/ProfileForm.java
```

The second goal resolves `com.codenameone:codenameone-guibuilder` through Maven and passes a project binding to the editor. IntelliJ IDEA, NetBeans, Eclipse, and Visual Studio Code now ship shortcuts that invoke the same goal. The editor itself remains a Java 8 artifact, so it runs on the JDK that already builds the application.

Switching forms no longer starts another process. The Forms tab is backed by a recursive scan of `src/main/guibuilder`, so a project with twenty forms behaves like a project with twenty forms, not twenty unrelated editor sessions.

## Guided layout survived the rewrite

We did not replace Steve's layout model with absolute coordinates. A component dropped in auto layout mode still becomes a `LayeredLayout` child whose insets can refer to the parent or another named component.

Here is a trimmed form from the builder's demo project:

```xml
<component name="GuidedLayoutForm" type="Form" layout="LayeredLayout">
    <component name="heroTitle" type="Label"
        layeredinsets="24px auto auto 24px"
        text="Guided Layout" />
    <component name="description" type="SpanLabel"
        layeredinsets="12px 24px auto 0px"
        guidedreferences="heroTitle|-|-|heroTitle"
        guidedhorizontalsize="fill" />
    <component name="secondary" type="Button"
        guidedreferences="primary|primary|-|primary"
        guidedhorizontalsize="match"
        guidedmatchwidth="primary" />
</component>
```

Those names matter. A guide stored as an object pointer would disappear after a save and reload. A guide stored by component name survives on disk, which means the model must reject duplicate names and update references when you rename, delete, or paste a component.

The canvas adds the relationship while you drag. It can align edges, centers, and text baselines. Resizing can keep a preferred size, fill the parent, stay fixed, or match another component. The same canvas can switch from phone portrait to desktop width, which makes a bad relationship visible before it reaches a device.

![Moving the primary action beside the description updates its guided-layout relationship](/blog/third-generation-gui-builder/guided-layout-drag.gif)

The move above is not stored as a new set of absolute coordinates. It changes the relationship between `primary` and `description`, then lets `LayeredLayout` resolve the result for the current canvas.

![The same guided layout switching between phone portrait and desktop canvases](/blog/third-generation-gui-builder/responsive-canvas.gif)

## CSS belongs beside the canvas

The second-generation builder and the old resource editor were separate tools. A user comment on our 2016 GUI Builder post asked for the editor to read the project CSS and render it directly. The third generation finally treats that as the normal workflow.

Click **CSS** and the project stylesheet opens beside the live form:

![Editing the project CSS beside the live form preview](/blog/third-generation-gui-builder/css-editor.png)

The pane edits the real `src/main/css/theme.css`. After the edit debounce, the CSS compiler installs the new theme and rebuilds the preview. A selector change is visible where it matters:

```css
Button {
    background-color: white;
    color: #2459b8;
    border: 1px solid #315fce;
    border-radius: 2mm;
    padding: 2mm 4mm;
}
```

There is no second theme model to keep in sync. The stylesheet that colors the canvas is the stylesheet Maven compiles for the application.

## Generated Java is visible, but not disposable

The **Code** button opens the companion Java source in the same workspace:

![The companion Java source with generated and user-owned regions](/blog/third-generation-gui-builder/code-editor.png)

Generated code and user code have different ownership:

```java
// <gui-builder-generated>
private Button signIn;

private void buildUI() {
    signIn = new Button("Sign in");
    signIn.setName("signIn");
    add(signIn);
}
// </gui-builder-generated>

// <gui-builder-user-code>
protected void onSignIn(ActionEvent event) {
    authenticate();
}
// </gui-builder-user-code>
```

The embedded editor protects the generated region instead of waiting for Save to overwrite it. Event handlers stay in the user region and survive regeneration. This removes an old timing problem where one IDE generated source on save while another waited for the next build.

The `.gui` file remains plain XML. You can review it in a pull request and recover it without a proprietary database. The current editor does not watch a hand-edited `.gui` file while it is open, so use **Refresh** after an external edit.

## One hardening policy before the platform split

[PR #5527](https://github.com/codenameone/CodenameOne/pull/5527) adds an open-source hardening engine that runs on the merged application before it becomes Android, iOS, JavaScript, Windows, Linux, or JavaSE output. The goal is DexGuard-class resistance without protecting one port and leaving the others exposed.

The policy is intentionally port-aware. Android keeps R8 as its sole renamer. JavaScript skips string encryption because its native bridge can hold live string references. ParparVM skips control-flow transforms that would fight its optimizer. The common pipeline still gives one build hint, one report, and a build-specific mapping connected to Crash Protection.

Saturday's [App Hardening deep dive](/blog/app-hardening-cross-platform/) covers the exact port matrix, string-encryption exclusions, keep rules, retrace lifecycle, and local-build boundary. It also separates hardening from App Shield and the encrypted database work still under review.

## A breakpoint no longer dereferences a random local

[PR #5536](https://github.com/codenameone/CodenameOne/pull/5536) fixes the `signal 11` crash that could take down an iOS app when NetBeans asked for locals at a breakpoint. One debugger table had one address per JVM slot but one row per declared local. Reusing a slot for an `int` and later an object could pair the object row with four bytes of integer storage, then dereference it as an eight-byte object pointer.

The generated table now stores one address per row. Frame entry clears stale debugger side channels, and object references pass through a Darwin memory-read check before native code dereferences them. The same PR adds real thread enumeration, deferred breakpoint replay, scoped locals, and ready-made on-device debug actions.

The PR added 66 tests around the generated C and JDWP proxy. It did not include a tethered-device debugging session, so the merge proves the policies and generated code, not every IDE and device combination.

## A skipped test is not a pass

[PR #5538](https://github.com/codenameone/CodenameOne/pull/5538) found that screenshot skip markers used output names while the report parser looked only at Java class names. The marker disappeared, and the surrounding start and finish lines counted the test as a pass.

Correcting that mapping exposed 11 hidden skips on watchOS, six on tvOS, and two or three on every other port. An unknown marker now fails the reporting contract instead of disappearing.

The same investigation fixed Linux `VideoIO`. The CI image lacked the codec plugins it claimed to test. GStreamer returned a partial pipeline plus an error for a missing element, but the port ignored the error. The reader also asked a paused pipeline for a normal sample when the decoded frame was still the preroll buffer. Linux now installs the codecs, rejects partial pipelines, pulls preroll correctly, and reports only encoders and decoders present in the GStreamer registry.

## Google Sign-In now runs in an arm64 simulator

[PR #5544](https://github.com/codenameone/CodenameOne/pull/5544) moves the iOS Google Sign-In integration from the old 5.x vendored framework to `GoogleSignIn` 7.1. The old framework had an arm64 device slice but no arm64 simulator slice. On an Apple silicon Mac, the linker selected the device slice and rejected it.

Version 7.1 builds from source for the selected SDK. The native bridge now uses completion handlers and the current token API. We stopped at 7.1 because later versions introduce Swift dependencies that would force modular headers into every generated Podfile, including projects unrelated to Google Sign-In.

## Small-object pages can return to the OS

ParparVM's BiBOP allocator segregates small objects by size class. Before [PR #5540](https://github.com/codenameone/CodenameOne/pull/5540), empty pages stayed in that allocator forever. A temporary peak in small objects could therefore crowd out a later image buffer, Metal texture, or glyph atlas even after the small objects were collected.

The collector now keeps a 4 MB warm pool and releases the slot area of surplus pages. On Apple platforms it uses `MADV_FREE_REUSABLE`, which reduces the `phys_footprint` value used for memory pressure. A controlled integration test warmed 192 MB of 256-byte objects and then allocated two identical large-buffer sets. The measured peak fell from 466,224 KB to 287,824 KB, while the benchmark geomean stayed at 1.0031 relative to the no-release control.

Arm64 retains roughly one quarter of each 64 KB allocator page because its 16 KB system page also contains the allocator header. Moving that header would require a larger allocator redesign.

## A retired URL now has somewhere useful to go

[PR #5546](https://github.com/codenameone/CodenameOne/pull/5546) adds a `/download/` compatibility page for old plugin and Ant-era links. New projects go to Initializr and Getting Started. Existing legacy projects go to the Maven migration guide.

The general 404 page now offers search plus direct routes to setup, documentation, demos, pricing, community, and legacy downloads. It is marked `noindex, nofollow`; normal pages remain indexable. The constrained `_redirects` file did not change.

Initializr now generates projects on Codename One 7.0.265 through [PR #5533](https://github.com/codenameone/CodenameOne/pull/5533).

## The work around the app is part of the app

The GUI Builder rewrite is about keeping control close to the artifact you ship. Forms, CSS, generated Java, and responsive behavior now share one inspectable workspace. The debugger, port reports, memory allocator, login bridge, and recovery pages all remove places where the toolchain previously hid the real state.

Security follows the same direction. Last week [App Shield](/blog/app-shield-server-attestation/) moved the final trust decision from the phone to the backend. Saturday's [App Hardening post](/blog/app-hardening-cross-platform/) covers the binary layer. The open [portable encrypted database PR](https://github.com/codenameone/CodenameOne/pull/5526) addresses data at rest and is still under review.

These controls solve different problems. Together they move Codename One toward a secure default at each boundary: readable code in the binary, modified clients calling a server, and plaintext data on disk. That is a more useful security lead than one large checkbox with an impressive name.

---

## Discussion

_Which part of a visual builder costs you more time: placing components, keeping styling in sync, or getting safely back to source?_

{{< giscus >}}
