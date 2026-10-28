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
| Bundle | A copy of the `.app` with its executable stripped, plus the bytes of everything in it except the executable. |
| Recomposition | `KMPBENCH_PERF=1` launches only (`Perf.kt`, shared code). After 30 discarded changes, the day filter is changed 300 times. Each change is timed until a 1 dp marker drawn after the list has been painted, so compose, layout and draw are all included. |
| Scroll | After one discarded pass, all 150 cards (three days of 50) are scrolled one card per frame with `scrollToItem`. compose-compat has no `scrollBy`. Work is the time from the scroll call to the marker drawn. Interval is the time between consecutive markers. Jank is frames longer than twice the median interval. |
| Memory | Physical footprint (`footprint <pid>`) five seconds after launch: the dirty memory the process owns, which is what iOS charges an app and kills it for. Resident size is not used: on the simulator it counts the shared dyld cache, which every process maps, so an empty app already shows 184 MB. |

## Results

iPhone 17 Pro simulator, Xcode 27, Apple silicon, Release builds, 5 runs.
Start-up here is from process start (the kernel's start time) to the marker, so
`simctl`'s own overhead is left out of both.

| | Codename One, Compose | Codename One, Java | Compose Multiplatform |
|---|---|---|---|
| Cold start | 794 ms | 754 ms | 1051 ms |
| Warm start (median) | 555 ms | 560 ms | 597 ms |
| Executable, stripped | 16.8 MB | 8.9 MB | 22.9 MB |
| App bundle, executable stripped | 18.3 MB | 10.2 MB | 22.9 MB |
| Bundle without executable | 1.3 MB | 1.3 MB | 2.5 KB |
| Physical footprint | 48.0 MB | 53.0 MB | 49.0 MB |
| Recomposition, day change to drawn (median) | 5.31 ms | 4.56 ms | 20.37 ms |
| Recomposition, day change to drawn (p95) | 6.79 ms | 5.67 ms | 23.08 ms |
| Scroll, work per frame (median) | 5.60 ms | 1.80 ms | 16.94 ms |
| Scroll, work per frame (p95) | 8.06 ms | 1.94 ms | 18.81 ms |
| Scroll, frame interval (median) | 39.52 ms | 37.91 ms | 33.34 ms |
| Scroll, frame interval (p95) | 45.00 ms | 43.98 ms | 35.05 ms |
| Scroll, frames over 2x median | 0 of 144 | 0 of 144 | 0 of 144 |

Runtime values are the median over 5 launches of each launch's median or p95.
An empty UIKit app measured 640-750 ms start-up and a 34 MB footprint on the
same simulator.

**The bundles differ mostly in resources.** The CMP bundle holds only the
executable, `Info.plist` and its signature, because Skia and the fonts it uses
are linked in. The CN1 bundle holds 1.3 MB besides its executable:
`Assets.car` (570 KB), `material-design-font.ttf` (357 KB),
`iOSModernTheme.res` (213 KB), `default.metallib` (92 KB) and
`kotlin.kotlin_builtins` (29 KB).

**Codename One does each step in a quarter to a third of the time.** A day
change takes 5.3 ms from state change to drawn on Codename One and 20.4 ms on
CMP; one card of scrolling takes 5.6 ms against 16.9 ms. Every side then waits
for the display: `scrollToItem` is issued once per drawn frame, so the frame
interval is about two display frames on each, and none dropped a frame against
its own median. These are simulator numbers on an Apple-silicon Mac. They show
where each runtime spends CPU in identical code. They do not show device frame
rates, GPU cost, or how a finger-driven fling feels.

**The Java column is the same app without Kotlin.** `app/java/JavaBench.java`
is the Keynote screens written against Codename One directly -- the same
programme generator, the same cards, the same steps timed the same way, a 1 px
marker painted after the list -- so it ships no Kotlin standard library, no
Compose runtime and no coroutines. Its executable is 8.9 MB, 39% of CMP's and
about half of the Compose build; a scroll step is 1.8 ms. Memory is the one
row it does not win: it builds all fifty cards of a day as components, where a
lazy list composes only those in view.

Building it found a framework bug every Codename One app on a notched device
paid: a Form with a Toolbar laid itself out again on every EDT cycle, forever,
because the safe-area padding it sets for one measurement announced a style
change that queued another layout. The idle app burned 14-26% CPU and 10-20 MB
of garbage. `Container.snapToSafeAreaInternal` now sets that padding silently.

The first version of compose-compat took 42 ms per day change, and almost none
of it was work: the EDT sat idle for 83% of it. A state change waited for a
dispatched resumption, then for the snapshot to apply, then for frame
callbacks sent after paint -- three EDT cycles. Frames now run at the start of
the layout pass, as Android's Choreographer runs them before traversal, and
the recomposer resumes inline on the EDT, so change, layout and paint share
one cycle.

**Start-up is the simulator's floor.** An empty UIKit application, with no
code of its own, takes as long to show its first frame as either side, so
neither has much start-up of its own left to lose there. Measure on a device
before quoting a difference.

The Codename One executable includes the whole iOS port and the full Material
icon font, so the 6.1 MB difference is a floor.

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
- Scrolling is measured as programmatic `scrollToItem` steps on the simulator,
  not a touch fling on a device.
