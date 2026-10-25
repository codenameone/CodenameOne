# Codename One vs. Compose Multiplatform

One Compose application, **Keynote** (a conference schedule: search, day
filters, a 150-session list of cards, favorites, a detail screen and an animated
chart), built twice from the **same `commonMain` source** and run side by side:

- **Codename One**: the source imported into an archetype application
  (`common/src/main/kmp`), compiled by kotlinc with the Compose compiler against
  `maven/compose-compat`, translated by ParparVM to C, built by Xcode.
- **Compose Multiplatform**: the same source as a CMP module
  (`org.jetbrains.compose`, Kotlin/Native and Skia), linked as an iOS
  executable.

The shared code is in `app/shared/commonMain`. The only per-side files are each
side's entry point (`app/cn1/BenchEntry.kt`, `app/cmp/composeApp/src/iosMain`).
The CMP build adds one compiler opt-in, `ExperimentalMaterial3Api`, which the
real material3 demands for the top app bar. That keeps the shared source
identical on both sides. Keynote is written for this benchmark. It is not a
JetBrains sample.

## Running it

```bash
export JAVA17_HOME=...         # JDK 17: Gradle, kotlinc, the CN1 translation
scripts/kmp-bench/run_bench.sh --work /tmp/kbench --runs 5 \
    --maven-repo "$PWD/.m2-repo"   # the repository the checkout installed into
```

The script needs Xcode, Gradle 8.5 or newer on `PATH`, and the reactor installed
into the repository it is given. It boots one iPhone simulator, shuts it down
when it finishes, and writes `results.json`, `results.md` and a screenshot of
each app into the work directory.

## What is measured

| | |
|---|---|
| Start-up | From `simctl launch` to the app's `KMPBENCH first-frame` line. Both print it on the first main-thread turn after their UI is installed. CMP's marker fires before Skia has drawn anything, so the comparison favours CMP. Run 1 is the first launch after install (cold); the median of the rest is reported as warm. |
| Executable | The app binary after `strip`, which is what an App Store build ships. |
| Bundle | The `.app` directory as built (the CN1 binary in it is not yet stripped). |
| Memory | Physical footprint (`footprint <pid>`) five seconds after launch: the dirty memory the process owns, which is what iOS charges an app and kills it for. Resident size is not used: on the simulator it counts the shared dyld cache, which every process maps, so an empty app already shows 184 MB. |

## Results

iPhone 17 Pro simulator, Xcode 27, Apple silicon, Release builds, 5 runs.
Start-up here is from process start (the kernel's start time) to the marker, so
`simctl`'s own overhead is left out of both.

| | Empty UIKit app | Codename One | Compose Multiplatform |
|---|---|---|---|
| Start-up | 640-750 ms | 596-685 ms | 646-794 ms |
| Executable, stripped | | 16.4 MB | 22.9 MB |
| App bundle | | 27.3 MB | 28.8 MB |
| Physical footprint | 34 MB | 47-48 MB | 48 MB |

**Start-up is the simulator's floor.** An empty UIKit application, with no
code of its own, takes as long to show its first frame as either side, so
neither has much start-up of its own left to lose there. Measure on a device
before quoting a difference.

The Codename One executable includes the whole iOS port and the full Material
icon font, so the 6.5 MB difference is a floor.

**Memory is level.** The Java heap holds
3.2 MB live after start-up, in about 57,000 objects. Loading the theme used to
leave 24,000 strings behind -- one per key and value, plus every colour and
padding built by concatenation, and a cache key per `Font.derive` call. The
theme loader now decodes into reused buffers and shares repeated values
(`ThemeStrings`), and a derived font is found without building a key, which
took 0.6 MB of live heap and about 2 MB of footprint off. Freed pages stay
resident on iOS, so collecting sooner does not help: garbage that is never
created is the only saving. Most of what remains is per thread (two
`Dispatchers.Default` workers that `flowOn` starts) and text rendering (the
glyph atlas and CoreText).

Codename One also targets platforms CMP does not: watchOS, tvOS, and native
Windows and Linux binaries with no JVM. Keynote has not been built for those
here yet.

## Not yet covered

- `LazyColumn` and `LazyRow` with `reverseLayout = true` are refused at run
  time. The lists compose only the items in view.
- Frame time while scrolling is not measured. That needs a device and a driven
  gesture rather than a simulator launch.
