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

The SDK version is **pinned** in the workflow (`FLUTTER_REF`), not tracked from
`stable`. A benchmark whose reference moves on its own is not one -- the
gallery, its dependency resolution and Flutter's own code generation all change
between releases, so a number from last week and a number from today would
differ for reasons unrelated to this repository. It is not hypothetical:
tracking `stable` broke outright when 3.47.5's tree resolved `google_fonts` to
a version without `robotoCondensed` and three studies stopped compiling.
Bumping the pin re-baselines the comparison, so expect every number to move and
re-record `baselines/` in the same change.

Lifting the gallery out of the SDK's pub workspace takes two things with it.
Its **lockfile**, because the gallery pins almost nothing (`google_fonts: any`)
and the workspace root is the only thing holding its dependencies at tested
versions. And its **asset packages** -- `flutter_gallery_assets`,
`rally_assets`, `shrine_images` -- which the workspace root declares once for
every member, so a lifted package loses them entirely. Those are derived from
the pubspec's own asset paths rather than listed, so a fourth would be picked
up on its own.

## Running it locally

```bash
scripts/flutter-bench/app/prepare.sh --work /tmp/fbench \
    --maven-repo "$PWD/.m2-repo"          # per-checkout repo; see CLAUDE.md

scripts/flutter-bench/app/build_apps.sh --work /tmp/fbench --platform macos

# the cn1= and flutter= paths build_apps.sh printed
scripts/flutter-bench/app/measure.sh macos <cn1-artifact> <flutter-artifact> /tmp/fbench-out
```

`measure.sh` is what every CI leg runs, so a local run measures exactly what CI
does. It writes `result-<platform>.json` and `baseline-<platform>.json`.

`run_bench.py --list` reports which platform adapters have been **exercised
end to end**, which is deliberately not the same question as which ones exist.

```bash
python3 scripts/flutter-bench/test_benchlib.py    # the arithmetic and the gate
python3 scripts/flutter-bench/test_platforms.py   # marker timing, artifacts, installs
```

## Why the numbers are shaped the way they are

Each of these exists because the obvious alternative produced a flattering
result, and several were caught only after being measured the wrong way first.

- **Interleaved runs, best of N, load recorded.** One run of each side,
  alternating. A machine that gets busier halfway through then penalizes both
  sides equally. On this project two walkthrough recordings desynchronised and
  looked like a timing regression; the cause was a stray simulator holding the
  machine at load 8.

- **Start-up is a bracket, not a point.** The runtimes do not expose the same
  event. Flutter's `FIRSTCONTENT` is a UI-thread callback that runs *before*
  that frame is rasterized, while Codename One's `FIRSTFRAME` fires once the
  form is on screen. Comparing those two charges one runtime for rasterizing
  its first screen and not the other. The harness reports Flutter's figure as
  a range (`FIRSTCONTENT`..`RASTERDONE`) and takes the ratio from the end least
  favorable to Codename One.

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

- **A platform with no baseline FAILS.** The gate compares against
  `baselines/<platform>.json`. Without one there is nothing to compare, and a
  gate that passes in that state is indistinguishable from one that checked --
  so the run fails, says the gate is not armed, and leaves the baseline it
  recorded (`baseline-<platform>.json`) in the workflow artifact. Committing
  a baseline arms the gate -- but take its timing and memory figures as the
  UPPER edge across several runs' candidates, not one run's best: Android's
  best-of-five moved 282-418 ms across runs of unchanged code, and a baseline
  taken from a low run fired on noise; re-committing it after a deliberate change (a
  Flutter SDK bump moves every number) re-baselines it. Each baseline carries
  its own tolerances: 2% for sizes, 25% for start-up, 15% for memory. A metric
  with no tolerance is recorded but not gated, and the file says why: macOS
  start-up is one, because the hosted runner spread a single run from 465 to
  2588 ms at a load of 12-32, and no band wide enough to ignore that catches
  anything.

- **Deadlines are enforced by the clock.** Output is read on a thread with a
  timeout, so a launch that hangs before its marker is abandoned at the launch
  timeout and a healthy application that goes quiet after its last marker is
  let go at the settle time. A blocking `readline()` waited for one more line
  in both cases, and the job sat until the workflow's own timeout.

## Compute: the VM workloads on every platform

Beside the application metrics, each platform also compares raw compute: the
eleven workloads of `vm/benchmarks` (`CommonWorkloads.java` and its Dart port,
`vm/benchmarks/dart/common_workloads.dart`), which the VM suite already holds to
bit-identical checksums between the two languages.

- **Same binaries.** Both benchmark apps carry the workloads and switch to them
  when asked, so no second pair of apps is built. `prepare.sh` copies the sources
  in from `vm/benchmarks`, their only home.
- **How each app is asked:** a marker file `/tmp/nat/BENCH_COMPUTE` on the
  desktop (ParparVM has no `getenv`), the launch intent on Android (a data URI
  for Codename One, the `dart_entrypoint_args` extra for Flutter), and
  `?benchCompute=1` on the web.
- **Scoring.** Each app runs every workload twice to warm up and five times
  timed, and prints the best. The ratio is Flutter's time over ours, and the
  headline is the geometric mean. A workload whose checksums differ is shown and
  left out of the mean -- two different computations have no ratio. On the web
  that is expected for the 64-bit workloads, since a JavaScript number cannot
  hold one.
- **Gated.** A geometric mean under 1.00x fails the platform's leg, like any
  other metric Codename One loses.
- **iOS is not measured**: a device build needs signed hardware, and on the
  simulator Flutter runs its JIT debug engine. The macOS row runs the same two
  compilers on the same Apple silicon.

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

## Per-platform mechanics

- **Desktop artifacts are directories.** Flutter's Linux bundle and Windows
  Release folder, and the result folder of Codename One's native builders, are
  handed over whole; the adapter launches the one executable at the top level
  and refuses to guess if there is more than one. The Codename One path comes
  from what the native builder logs ("Built native Linux executable: ..."),
  not from a guessed directory name.
- **Code size is every native image**, as on Apple platforms: all ELF files on
  Linux and all PE images on Windows. Flutter's Dart image is lib/libapp.so and
  its engine a separate library, so the launcher alone would be a fraction of
  its code.
- **Linux measures under Xvfb.** Both applications are GTK programs. The
  xvfb-run around the Maven build ends with that build, so the measurement
  starts its own.
- **Android runs on an emulator** (the same API level and image as the Android
  port's instrumentation leg), and each APK is installed fresh -- uninstalled
  first -- so the build that was sized is the build that is launched. Codename
  One's `assembleRelease` output is unsigned, which `adb` refuses to install;
  it is signed with the debug key, as Flutter's release template already is,
  so both sides carry a signature. Its launcher activity is `.BenchStub`, not
  `.MainActivity`.

## Where the numbers go

Every run posts the comment on the pull request. A nightly or dispatched run
on master also publishes the folded results to the `port-status-data` branch
as `benchmarks/flutter.json`, and the website build resolves that into
`data/port_status_flutter_benchmark.json`, which the Port Status page renders
as its own section. That section is the only place the page may name another
framework: `scripts/website/validate_port_status.mjs` still fails any Flutter
mention outside it, and fails the section if it reuses the compliance matrix's
counted attributes.

## Known constraints

- **No adapter has been exercised end to end yet.** The Codename One side of
  the `macos` recipe has been built from a prepared tree, and `prepare.sh`
  itself is verified; the native compile steps and every other platform are
  not. `run_bench.py --list` reports this rather than implying otherwise.
- **No baseline is committed yet**, so every measured platform currently fails
  its gate by design until its first recorded baseline is committed.
