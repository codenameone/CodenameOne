---
title: "Debug Your Dart in a Java Debugger, and JFX Central on a Phone"
slug: debug-dart-in-a-java-debugger
url: /blog/debug-dart-in-a-java-debugger/
date: '2026-11-12'
author: Shai Almog
description: "Flutter apps built by Codename One now debug in Dart: breakpoints by Dart line and stack traces that name your .dart files, in the standard Java debugger. Plus Flutter fixes from a real port, a faster JavaScript port that still gets the right answers, and JavaFX improvements that run JFX Central on an iPhone."
feed_html: "<img src=\"https://www.codenameone.com/blog/debug-dart-in-a-java-debugger.jpg\" alt=\"JFX Central running on an iPhone\" /> Flutter apps built by Codename One now debug in Dart, in the standard Java debugger. Plus Flutter fixes from a real port, a faster web port and JFX Central on an iPhone."
series: ["release-2026-11-06"]
---

![JFX Central running on an iPhone](/blog/debug-dart-in-a-java-debugger.jpg)

Two weeks ago we showed that Codename One can [build a Flutter app from its Dart source](/blog/flutter-better-in-java/) and run it faster than Flutter's own build on most of our benchmarks. The first question from people who tried it was a fair one: what happens when something breaks? You'd be staring at a stack trace full of generated Java. That's fixed, and this post also rounds up the rest of the week's work on code you already have.

## Dart stack traces in a Java debugger

The Dart transpiler turns your `.dart` files into Java, and javac compiles that. Normally the class files would point at the generated Java, which you never wrote and shouldn't have to read.

Now they point at your Dart. While it translates, the emitter records which Dart line produced each Java line. After javac runs, the build rewrites the debug information in every class file: line numbers become Dart line numbers, the source file becomes your `.dart` file, and a JSR-45 source map is added with Dart as the default stratum. The mapping table itself is stripped, so nothing extra ships in your app.

{{< mermaid >}}
flowchart LR
    D[lib/main.dart] -->|transpiler marks each line| J[Generated Java]
    J -->|javac| C[Class files pointing at Java]
    C -->|DartLineMapper rewrites debug info| R[Class files pointing at main.dart<br/>line numbers + SMAP, stratum Dart]
    R --> X[Stack traces and debuggers show Dart]
{{< /mermaid >}}

We didn't have to build a debugger. There's no Dart debugger and no IDE plugin. The Java debugger already knows how to read line tables, source file attributes and JSR-45 source maps, which is how JSP debugging has worked for twenty years. So an exception's stack trace names `main.dart` and the Dart line, and a debugger attached to the simulator resolves breakpoints and steps through your Dart source. It happens automatically in Maven and Gradle builds of any project with `src/main/flutter`.

## Flutter fixes from a real port

We took several real Flutter apps through the import, and each one found something. The fixes landed in the transpiler and the runtime rather than as workarounds:

- Generics substitution, closures that capture and modify locals, local recursive functions, tear-offs, constructor redirects and functions that return `Never`.
- An isolate event loop, so timers and queued callbacks run in the order Dart promises.
- Lists that remember their element type, so a wrong downcast fails where Dart says it should.
- An `async` main that waits on `async*` streams, `String.runes`, and a long list of Flutter API and `dart:*` gaps.

Three clean-room apps written for this, a club hub, a field notes app and a pet adoption app, now run as acceptance tests on every change, and Gradle builds can import Flutter projects too.

## A faster JavaScript port that still gets the right answer

Last time I mentioned that Flutter's web build is faster than ours on some workloads, partly because it gets some of them wrong. We wanted speed without giving that up. The JavaScript port now inlines getters and setters, fuses `StringBuilder` chains that don't escape into plain string operations, keeps every array packed so the browser's JIT never sees holes, and turns virtual calls into direct ones when type flow proves there's only one target.

In our compute suite, string building dropped from 99 to 68 ms, quicksort by 6% and sequential array access by 5%, and the checksums still match the JVM exactly. We also measured and dropped three ideas that sounded good: scalar replacement (V8 already does it), bounds-check elimination (it was slower) and removing repeated casts (they're free). And integer division by zero now throws `ArithmeticException` on the web, as Java says it should, instead of quietly returning zero.

## JFX Central on an iPhone

[JFX Central](https://www.jfx-central.com/) is the JavaFX community's hub: people, libraries, apps, books and videos, written in JavaFX by people who know JavaFX very well. We ported it to phones on top of our JavaFX compatibility layer, and this is it running on an iPhone:

{{< guide-block >}}
<video controls playsinline muted preload="metadata" poster="/blog/release-2026-11-06/JFXCentral-demo-poster.jpg" width="420" height="913" style="width:100%;height:auto;max-width:300px;display:block;margin:16px auto" aria-label="JFX Central running on an iPhone: home, people, showcases and libraries"><source src="/blog/release-2026-11-06/JFXCentral-demo.mp4" type="video/mp4"><a href="/blog/release-2026-11-06/JFXCentral-demo.mp4">Download the JFX Central video</a>.</video>
{{< /guide-block >}}

It doesn't feel as polished as an app written for Codename One from the start. But it's a real JavaFX codebase, with bindings, cells, CSS and remote images everywhere, running on a phone, and getting there filled real gaps in the layer:

- `ListProperty` listeners fire on content changes, and `ObservableValue.map`, `flatMap` and `orElse` work.
- Remote images load in the background through the same cache as `URLImage`, and the layout updates when they arrive.
- `Label` text wraps onto several lines, and `ImageView` honors `-fx-fit-width`, `-fx-fit-height` and `-fx-preserve-ratio` from CSS.
- Cells no longer try to set a bound `visible` property, which used to throw.

The [desktop compatibility chapter](/developer-guide/desktop-interop/) covers what carries over from JavaFX and Swing, and the [Flutter chapter](/developer-guide/flutter-interop/) covers the import.

That's the week. Friday starts the next one.

{{< giscus >}}
