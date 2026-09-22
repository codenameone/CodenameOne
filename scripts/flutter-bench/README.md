# Flutter vs Codename One benchmark

Measures **one application built two ways**: the Flutter toolchain's own
release build of the gallery, and the identical Dart source transpiled to Java
and built by Codename One. Same source, same screens, same machine, so a
difference in size, start-up or memory is a difference between the two
runtimes and nothing else.

```
app/prepare.sh      materialises both projects from the installed Flutter SDK
app/build_apps.sh   builds both, release, for one platform
run_bench.py        measures, renders, and gates against a baseline
port_status.py      folds the results into the website's data
```

## Where the application comes from

Neither side is vendored. `prepare.sh` copies the gallery out of the Flutter
SDK (`$FLUTTER_ROOT/dev/integration_tests/new_gallery`) into **both** project
trees, so there is no second copy for an edit to land on. The Codename One
project is generated from the shipping `cn1app-archetype` with the
`codenameone-flutter-runtime` dependency enabled -- the same two steps the
developer guide tells a user to take -- which means a change that breaks the
documented Flutter wiring breaks this benchmark too.

What *is* committed here is the two entry points, which are ours:
`app/cn1/Bench.java` and `app/flutter/main_bench.dart`. Each prints the
start-up marker the harness times against.

## Running it locally

```bash
scripts/flutter-bench/app/prepare.sh --work /tmp/fbench \
    --maven-repo "$PWD/.m2-repo"          # per-checkout repo; see CLAUDE.md

scripts/flutter-bench/app/build_apps.sh --work /tmp/fbench --platform macos

python3 scripts/flutter-bench/run_bench.py --platform macos \
    --cn1-app     /tmp/fbench/cn1/javase/target/....app \
    --flutter-app /tmp/fbench/flutter/build/macos/Build/Products/Release/gallery.app \
    --json /tmp/macos.json
```

`run_bench.py --list` reports which platform adapters have been **exercised
end to end**, which is deliberately not the same question as which ones exist.

```bash
python3 scripts/flutter-bench/test_benchlib.py   # the arithmetic, 21 tests
```

## Why the numbers are shaped the way they are

Each of these exists because the obvious alternative produced a flattering
result, and several were caught only after being measured the wrong way first.

- **Interleaved runs, best of N, load recorded.** One run of each side,
  alternating. A machine that gets busier halfway through then penalises both
  sides equally. On this project two walkthrough recordings desynchronised and
  looked like a timing regression; the cause was a stray simulator holding the
  machine at load 8.

- **Start-up is a bracket, not a point.** The runtimes do not expose the same
  event. Flutter's `FIRSTCONTENT` is a UI-thread callback that runs *before*
  that frame is rasterised, while Codename One's `FIRSTFRAME` fires once the
  form is on screen. Comparing those two charges one runtime for rasterising
  its first screen and not the other. The harness reports Flutter's figure as
  a range (`FIRSTCONTENT`..`RASTERDONE`) and takes the ratio from the end least
  favourable to Codename One.

- **Executable code is every Mach-O in the bundle**, not the main executable.
  On iOS a Flutter application's own code is not in the executable at all --
  `Runner` is a thin launcher and the Dart image lives in
  `Frameworks/App.framework`. Sizing the executable alone compared our whole
  runtime against their stub and reported a loss that did not exist.

- **Assets are staged from Flutter's own bundle.** Staging the whole
  `flutter_gallery_assets` package ships files Flutter tree-shakes away, which
  inflates our installed size; staging only the 1x images ships fewer than
  Flutter does, which flatters us by tens of megabytes. Reading the built
  bundle removes the judgement call entirely.

- **iOS reports sizes only.** `flutter build ios --simulator --release` is
  refused by the Flutter tool, because Dart cannot AOT-compile for the
  simulator. A simulator run would time Flutter's JIT debug engine against a
  Codename One release build, so start-up and memory are reported as not
  measured rather than substituted. Sizes come from release device bundles,
  which need no signing.

- **Only our own numbers are gated.** Flutter's are recorded for the ratio and
  are outside our control, so a Flutter SDK upgrade that grows their build
  must not turn our build red.

## Nothing here uses the cloud builder

Every recipe in `app/build_apps.sh` builds on the machine it runs on. The
generated `build.sh` offers targets that send the build to the Codename One
build server -- `ios-device`, `android-device`, `mac-os-x-desktop` -- and those
are credentialed, billed, and apply a 100MB artifact limit that this
application exceeds. The `*-source` targets generate an Xcode or Gradle
project locally instead, and the script compiles those. The 100MB limit is a
property of the cloud build, not of a local one.

## Desktop means the NATIVE ports, not JavaSE

Codename One has two desktop stories and only one of them is a fair comparison
here. The `javase` targets bundle a JVM and ship an executable jar beside its
dependencies; the native targets compile through ParparVM to a real binary --
AppKit on macOS (`local-mac-device`), clang-cl on Windows
(`local-windows-device`), CMake/Ninja against GTK3/Cairo on Linux
(`local-linux-device`). Flutter's desktop builds are native binaries, so the
native ports are the like-for-like artifact and the javase ones are not
comparable on size at all.

Those three targets need a real toolchain on the runner -- Xcode, clang-cl,
and the GTK3 development packages respectively. Where one is missing the
Codename One half fails loudly rather than falling back to a JVM build that
would quietly produce an incomparable number.

## Known constraints

- **No adapter has been exercised end to end yet.** The Codename One side of
  the `macos` recipe has been built from a prepared tree, and `prepare.sh`
  itself is verified; the native compile steps and every other platform are
  not. `run_bench.py --list` reports this rather than implying otherwise.
