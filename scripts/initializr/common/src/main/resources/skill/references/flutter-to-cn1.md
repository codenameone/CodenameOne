# Flutter / Dart -> Codename One

Codename One compiles **Dart widget source to Java at build time** and renders
it through its own pipeline. There is no Dart VM, no embedded engine and no
platform view in the result -- a Dart screen becomes ordinary CN1 components.

That makes this different from the other porting guides in this directory:
`android-to-cn1.md` and `react-to-cn1.md` are about *rewriting* a design in CN1.
This one is mostly about *keeping* the Dart and making the build consume it.
Rewrite only what the transpiler refuses.

> Read alongside `references/build-and-run.md` (Maven goals, JDK matrix) and
> `references/ui-components.md` (for the parts you do hand-write).

## Decide which of the two shapes you are in

| You have | Use | Result |
| --- | --- | --- |
| A Dart widget or screen you want inside an existing CN1 app | `FlutterUI.wrap(widget)` | a `Container` you add anywhere; the surrounding screen keeps its own theme |
| A Dart application you want to run as the app | `FlutterUI.runApp(widget)` | a new `Form` is created, the Material base theme is installed, and it is shown |

Start with `wrap` whenever the answer is not obviously "the whole app". It is
reversible, it touches one screen, and it will surface the unsupported
constructs in that screen before anyone commits to the rest.

## Wiring the build

1. Add the runtime to `common/pom.xml`. Generated projects already contain it
   commented out:

```xml
<dependency>
    <groupId>com.codenameone</groupId>
    <artifactId>codenameone-flutter-runtime</artifactId>
    <version>${cn1.version}</version>
</dependency>
```

2. Put the Dart under `common/src/main/flutter/`.

3. Build normally. The `transcode-flutter` goal is already bound to
   `generate-sources` in the generated `pom.xml`, and is a **silent no-op**
   while `src/main/flutter` does not exist.

**No Dart SDK is needed.** The transpiler is Java; the Dart is input, never
executed. Do not add a Flutter SDK to the project or to CI on account of this.

Useful properties:

| Property | Default |
| --- | --- |
| `cn1.flutter.sourceDir` | `src/main/flutter` |
| `cn1.flutter.outputDir` | `target/generated-sources/flutter` |
| `cn1.flutter.package` | `com.codename1.generated.flutter` |

## The port loop that actually converges

Do not read the whole Dart codebase and plan. Let the build tell you what it
cannot do, because that list is exact and the reading is guesswork.

1. Copy the Dart in.
2. `mvn -pl common install`. The transpiler **fails the build** on a construct
   outside the supported subset and names it with a milestone code.
3. Fix or stub that one construct. Re-run.
4. Repeat until it compiles. Only then run it.
5. Run in the simulator and read the error inventory: widgets that are not
   implemented are **recorded**, not silently skipped, so a screen missing
   something says so rather than rendering blank.

Two failure modes to recognise, because they look alike and are not:

* **Transpile failure** -- a build error with a milestone code. The Dart uses a
  language or library feature that is not supported yet. This is a wall; work
  around it in the Dart.
* **Runtime gap** -- it builds and runs, but a widget is missing or wrong. This
  is in the runtime library, and the error inventory names the widget.

## What tends to need hand-work

* **Anything that is not UI.** Platform channels, plugins, `dart:io`,
  `dart:ffi`. Replace with the CN1 equivalent (`references/native-interfaces.md`
  for native code, the IO/networking section of `references/java-api-subset.md`
  for the rest).
* **Asset declarations.** Flutter's `pubspec.yaml` asset section has no
  counterpart; assets move to the CN1 project's own resources.
* **Packages from pub.dev.** Only the framework subset is transpiled. A
  third-party package has to be replaced or its source brought in and
  transpiled with everything else.
* **Fonts.** A Dart `TextStyle` naming a bundled font needs that font present
  in the CN1 project the normal way.

## Verifying a port, rather than eyeballing it

If you have the original running, compare frames rather than opinions. The
pattern used to build this feature:

* Render the same route on both sides at the **same framebuffer size**, with
  the same safe-area insets. Rescaling either side to match the other blurs one
  of them and invalidates the comparison.
* Diff at absolute position and score the percentage of differing pixels.
* When a screen scores badly, check for a **uniform offset first**. A whole
  page shifted by a few pixels scores like a broken screen while nothing is
  actually wrong with the content; one spacing value upstream is usually the
  cause.

See `references/testing-and-screenshots.md` and
`references/mockup-comparison.md` for the CN1-side tooling that does this.

## Measuring the result

`scripts/flutter-bench/run_bench.py` in the Codename One repository builds one
application both ways and reports installed size, executable code, download
size, cold start and idle memory per platform. If you are asked whether a port
is "worth it", measure it with that rather than asserting a direction -- it
reports the metrics Codename One loses as well as the ones it wins.

Two of its rules are worth copying into any comparison you make yourself:

* **Interleave the runs** and record the machine's load. A ratio measured under
  different load twice is not a ratio.
* **Start-up is a bracket.** The two runtimes do not expose the same event, so
  a single number requires picking one, and picking one is how a comparison
  ends up flattering whoever wrote it.

## Gotchas

* A `wrap`ped subtree deliberately does **not** install the Material base
  theme, so embedding a widget will not restyle the screen around it. If the
  widget looks unstyled, that is why -- style it through the CN1 theme.
* CN1 is single-threaded on the EDT. Dart's `async`/`await` transpiles, but
  anything that assumed a separate isolate does not.
* `FlutterUI.runApp` must be called with the display initialized, i.e. from the
  CN1 lifecycle, not from a static initializer.
