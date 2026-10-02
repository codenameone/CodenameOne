# Flutter vs Codename One benchmark

Measures **one application built two ways**: the Flutter toolchain's own
release build of the gallery, and the identical Dart source transpiled to Java
and built by Codename One. Same source, same screens, same machine, so a
difference in size, start-up or memory is a difference between the two
runtimes and nothing else.

```
app/prepare.sh      materialises both projects from the installed Flutter SDK
app/build_apps.sh   builds both, release, for one platform
run_bench.py        measures, renders, and gates
flutter_baseline.py the regression gate's baselines (baseline/): check, fold, calibrate
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
Bumping the pin re-baselines the comparison: expect every start-up and memory
ratio to move, and rebaseline them in the same change's own overlay (see
"Baselines" below).

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
does. It writes `result-<platform>.json` and `baseline-<platform>.json` (the
candidate `flutter_baseline.py calibrate` reads).

`run_bench.py --list` reports which platform adapters have been **exercised
end to end**, which is deliberately not the same question as which ones exist.

```bash
python3 scripts/flutter-bench/test_benchlib.py    # the arithmetic and the gate
python3 scripts/flutter-bench/test_platforms.py   # marker timing, artifacts, installs
python3 scripts/flutter-bench/test_flutter_baseline.py  # the regression gate's baselines
```

## Why the numbers are shaped the way they are

Each of these exists because the obvious alternative produced a flattering
result, and several were caught only after being measured the wrong way first.

- **Interleaved rounds, load recorded.** Nine rounds, each one run of each
  side, back to back. A machine that gets busier halfway through then penalizes
  both sides equally. On this project two walkthrough recordings desynchronised
  and looked like a timing regression; the cause was a stray simulator holding
  the machine at load 8.

- **Start-up and memory are judged round by round.** The ratio is the median
  of the per-round ratios, and the figures shown are each side's median. Each
  side's best run let one lucky round decide: on the macOS runner Codename One
  was faster in three of five rounds (1787, 707, 468, 584, 668 ms against
  Flutter's 2512, 617, 522, 356, 769), and best-of reported a Flutter win, 356
  against 468, on the strength of a single round. The median of the per-round
  ratios is 1.11x. Memory is paired for the same reason: the emulator's first
  round read both apps half loaded (33 MB and 61 MB against 45 MB and 89 MB in
  every later round), and best-of compared those. The regression gate reads
  the same paired statistic (see "Baselines").

- **Start-up compares the same event on both sides: the first content frame
  on screen.** Codename One's `FIRSTFRAME` fires once its first form has been
  drawn. On the desktop, Flutter's `RASTERDONE` says how long ago its
  first content frame finished rasterizing, read from the engine's own frame
  timings, and the harness subtracts that from when the line appeared. Neither
  `FIRSTCONTENT` nor the line's own arrival is that event:
  - `FIRSTCONTENT` is a UI-thread callback that runs two frames after the
    content frame, because it waits for the element tree to stay the same size
    across two more frames.
  - A release engine batches timing reports for about a second, so the line
    itself arrives about a second late.

  On the web there is no shared clock to read, so Flutter's figure stays a range
  (`FIRSTCONTENT`..`RASTERDONE`), and the ratio is taken from the end least
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

- **Deadlines are enforced by the clock.** Output is read on a thread with a
  timeout, so a launch that hangs before its marker is abandoned at the launch
  timeout and a healthy application that goes quiet after its last marker is
  let go at the settle time. A blocking `readline()` waited for one more line
  in both cases, and the job sat until the workflow's own timeout.

## Baselines: two gates, and only one of them is a baseline

Every leg run with `--gate` answers two questions, and fails on either.

**Is Codename One at least level with Flutter?** `benchlib.check_behind`: any
measured metric with a ratio under 1.00x, a compute geometric mean under 1.00x,
or a compute workload Codename One got wrong fails the leg. It needs no baseline
and no tolerance: both sides come from the same Dart source and are measured
interleaved on the same runner.

**Did Codename One move against itself?** The regression gate, against
`baseline/`. It uses the ParparVM performance gate's model and its code
(`vm/selfhost/perf_baseline.py`, run by `flutter_baseline.py` with its own
layout), so the rules are the same and are written down once:

- `baseline/policy.json`: the global tolerance and floor per metric, and the
  metrics a platform records without gating, each with its reason (macOS
  start-up: one hosted-runner run spread from 465 to 2588 ms).
- `baseline/base/<platform>[@<cpu-model>].json`: the consolidated rows. Only the
  nightly fold (`.github/workflows/perf-baseline.yml`) writes them; a pull
  request that edits them fails `flutter_baseline.py check --base`.
- `baseline/pr/<number>.json`: one pull request's calibrations and rebaselines.
  No other branch writes that file, so two pull requests never conflict on a
  baseline; two that moved the same row are reported by name.

What is judged, per platform:

| Metric | Value | Keyed by | Policy tolerance |
| --- | --- | --- | --- |
| `code_bytes`, `install_bytes`, `wire_bytes` | Codename One's own size, absolute | platform | 0 |
| `cold_start_ratio` | Codename One / Flutter, median of the per-round ratios | platform, or platform@CPU model | 25% |
| `idle_memory_ratio` | the same, for memory at rest | platform, or platform@CPU model | 15% |

- **Sizes are absolute** because they are deterministic for a source tree, so
  any move is a real change. A row may carry its own tolerance: Android's code
  size has 0.5%, because at 0 every change that added bytes failed the leg
  while Android code stays 4.5x under Flutter's; Android's install and
  download sizes have 0.1%, because two runs of identical sources differ by 4
  bytes of APK packaging. Learned size tolerances round up in 0.1% steps, not
  the timing gate's 5%.
- **Start-up and memory are ratios to Flutter from the same run.** The pinned
  Flutter build is the unit of measure, as JDK 25 is for the ParparVM gate. An
  absolute figure moved with the runner: measured over the branch's seven
  complete runs, Codename One's median start-up ranged -40% to +10% around its
  median on Windows and -24% to +5% on Linux, while the ratio ranged -12% to +4%
  and -11% to +9%. The ratio replaces the one-sided `runner_slowdown_discount`
  and its `flutter_reference` yardstick, which corrected start-up only, only on
  Android, and only when the runner was slower. One cost, measured: on the
  Android emulator Flutter's own resting memory swings 83-93 MB, so the memory
  ratio is noisier there (+12%) than Codename One's absolute memory (within 4%).
- **Ratios fail in either direction; sizes only when they grow.** A ratio past
  its tolerance below the row fails as well as one above it, as in the ParparVM
  gate: an improvement nobody wrote down lets a later change give it back
  unseen. A size that SHRANK past its tolerance is reported in the job and the
  comment as "improved -- rebaseline to tighten", with the calibrate command,
  but does not fail. The reason is the paths filter: core pull requests that
  shrink the app mostly never run this benchmark, so a two-sided size row would
  turn the nightly run on master red for a good change that no pull request can
  rebaseline in its own overlay. Growth still fails, and so does a measured
  metric with no row and a row whose metric the run did not produce.
- **Only our own numbers are gated.** Flutter's enter only as the denominator
  of a ratio; a Flutter SDK that grows its build cannot fail a size row.

When the gate fails, the job prints (and the comment shows) the command that
accepts the move. Download the leg's `baseline-<platform>.json` artifact and:

```bash
# a new row (a platform or CPU model with no row yet)
python3 scripts/flutter-bench/flutter_baseline.py calibrate --pr 5931 baseline-android.json
# a row that moved on purpose
python3 scripts/flutter-bench/flutter_baseline.py calibrate --pr 5931 \
    --reason "why it moved" baseline-*.json
```

`--all` re-measures every row the files cover (for learning a row's noise from
several runs of unchanged code), `--metric code_bytes,...` limits it, and
`--per-cpu` calibrates ratio rows for the runners' CPU models. Commit the
overlay it writes with the pull request.

The one-file-per-platform `baselines/` this replaced is retired, and `check`
fails a pull request that changes it. A branch with edits to it converts them
with `flutter_baseline.py import-legacy --pr N --legacy <its baselines/>
--original <the baselines/ it branched from>`.

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
  left out of the mean -- two different computations have no ratio.
- **Attributed.** Each leg also runs the same `CommonWorkloads.java` on the host
  JVM. When the two apps disagree, or one prints nothing for a workload, that
  reference says which side failed, and the table shows "wrong result" or "did
  not run" against that side instead of dropping the row. On the web, Flutter
  gets `intArithmetic` and `arrayRandom` wrong (Dart compiled to JavaScript does
  integer arithmetic in doubles) and produces nothing for the 64-bit workloads.
  Two apps that agree are compared whatever the JVM says, since a transcendental
  workload may differ from a desktop libm in the last bit on both.
- **Gated.** A geometric mean under 1.00x fails the platform's leg, like any
  other metric Codename One loses, and so does any workload Codename One gets
  wrong or does not run.
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
- **Ratio rows are not keyed per runner CPU yet.** No run before the CPU was
  recorded (every result now carries `cpu`), so each platform has one plain row
  that judges every runner, with a tolerance learned across the mixed pool.
  Android start-up needs 55% for that. Once runs show a CPU model's ratio sits
  apart, `calibrate --per-cpu` gives it its own row.
