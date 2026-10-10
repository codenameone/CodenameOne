# Desktop compatibility benchmarks

How big, how hungry and how quick is a Swing or JavaFX application once it has
been imported with `cn1:import-desktop-project` and built as a native Codename
One executable, next to the same application shipped the way its developer ships
it today?

Everything here measures; nothing estimates. A cell that could not be measured
is printed as `--` and the reason is in the result's `errors` or under
"Failures" in the report.

## What is compared

| | Baseline | Codename One |
|---|---|---|
| Sources | the application's own `src/main`, unmodified | the same directory, imported |
| Compiler | `javac --release 17` (JDK 21) | the generated project's Maven build (JDK 17), then ParparVM to C |
| Runtime | a `jlink` image of exactly the modules `jdeps` names, `--strip-debug --no-header-files --no-man-pages --compress zip-6` | none: one native executable |
| Toolkit | the JDK's Swing, or OpenJFX from jmods | `maven/swing-compat` or `maven/javafx-compat` over the native port |
| Package | `jpackage --type app-image` around that runtime | the builder's output (`-O3`, ThinLTO, `--gc-sections`, stripped) |

Two JVM configurations run, because "the JVM" is not one number:

- **default**: `java -cp ... Main`, with the JDK's default CDS archive
  regenerated in the image (`-Xshare:dump`). A jlink image has none, and
  without it "default" would be slower than the `java` a developer starts.
- **tuned**: `-XX:+UseSerialGC -Xss256k -XX:TieredStopAtLevel=1`, an AppCDS
  archive recorded by one training run through the input script
  (`-XX:ArchiveClassesAtExit`), and the smallest `-Xmx` from
  `8 12 16 24 32 48 64 96 128 192 256 384 512` MB under which the application
  starts, runs the whole input script and logs no more exceptions than it does
  with no limit. (`OutOfMemoryError` alone is not the test: JavaFX under a heap
  too small for its textures paints once and then throws from the renderer on
  every frame.)

Sizes are taken **before** either CDS archive is written, which is the smaller
figure and so the one more favourable to the baseline.

## What is measured, and how

**Size.** Bytes on disk, and bytes as a single deflate level 9 zip, of: the
jar and its dependencies; the jlink image plus the jars; the jpackage
application image; the native build. The staged jar (what any Codename One
builder is handed) is broken down by layer, and the translated C project is
counted the same way, so the table shows how many of the compatibility
runtime's classes dead-code elimination kept.

**Start-up** (Linux). Every launch gets a private Xvfb with a window manager.
The driver polls the X server and records three instants after `exec`:
the application's window is mapped; the window's interior first shows three or
more distinct colours (**first paint** -- the headline figure, since a mapped
but blank window is not a started application); the picture stops changing.
Cold and warm, seven launches each after a discarded primer, reported as
median with minimum and maximum. Cold means the page cache was dropped
(`/proc/sys/vm/drop_caches`, which needs real root) or, failing that, the
application's own files -- its runtime image and jars, or its executable --
were evicted with `posix_fadvise`; the result says which (`cold_method`).
Rootless podman and a GitHub runner both get the second: the system libraries
both variants share stay cached, so "cold" here is "this application has not
run since boot", not "nothing has".

**Memory** (Linux). RSS, PSS and USS summed over the application's process
tree from `/proc/<pid>/smaps_rollup`, ten seconds after first paint and again
after the input script; peak RSS from `/usr/bin/time -v`. Median of three
sessions.

**Idle CPU** (Linux). CPU time of the process tree over the second half of the
idle window, as a share of one core, plus the number of polls at which the
window's pixels changed. A Codename One application idling above 1% is flagged
`DEFECT` in the report: something is repainting or polling with nothing to do.

**Input script.** `input/<name>.txt`, or `input/default.txt`: clicks at
window-relative positions and key presses through XTEST, identical for all
three variants. The scripts are short because they must mean the same thing to
two different toolkits' renderings of one application.

On **macOS** nothing may be launched unattended on a developer's desktop, so
`bench.sh --stage build` only builds and sizes. `run-macos-bench.sh` is the
runtime half, run by hand (or by CI, which has a desktop session): start-up to
the first on-screen window (`firstwindow.swift`; not first paint, which would
need the Screen Recording permission), idle RSS and physical footprint, idle
CPU, peak RSS. No scripted input. **Windows** has `win-measure.ps1`, the same
reduced set, and has only ever run in CI.

## Running it

Linux, in the container the published numbers came from:

```bash
podman build -t bench-linux scripts/desktop-compat-benchmarks      # once
# Codename One itself has to be in the Maven repository the run uses:
#   cd maven && mvn -pl linux,parparvm,swing-compat,javafx-compat,fxml-compiler,\
#     compat-jdk,codenameone-maven-plugin,cn1app-archetype -am -Plocal-dev-javase \
#     -DskipTests install
scripts/desktop-compat-benchmarks/linux-container.sh run \
  bash -c 'ARCH=$(dpkg --print-architecture)
    export JAVA17_HOME=/usr/lib/jvm/java-17-openjdk-$ARCH
    export BENCH_JDK21_HOME=/usr/lib/jvm/java-21-openjdk-$ARCH
    export BENCH_JAVAFX_JMODS=$BENCH_TOOLS/javafx-jmods CN1_CC=$BENCH_TOOLS/cn1-zig-cc
    export PATH=$BENCH_TOOLS/zig:$PATH BENCH_XVFB=1 BENCH_M2=$PWD/.m2-repo
    scripts/desktop-compat-benchmarks/bench-samples.sh --work /work/bench --out /work/out'
```

One application, anywhere:

```bash
scripts/desktop-compat-benchmarks/bench.sh --app path/to/project --name myapp \
  --kind swing --main com.example.Main --work /some/dir
scripts/desktop-compat-benchmarks/report.py /some/dir/myapp/result.json
```

`bench.sh --help` lists the environment it reads. Every file it writes is below
`--work`; set `BENCH_USER_HOME` to keep the Codename One plugin out of the real
`~/.codenameone` as well. A native build takes a quarter of an hour per
application on four cores, most of it the ThinLTO link.

macOS:

```bash
scripts/desktop-compat-benchmarks/javafx-jmods.sh "$BENCH_JDK21_HOME" /some/dir/jmods
BENCH_JAVAFX_JMODS=/some/dir/jmods scripts/desktop-compat-benchmarks/bench.sh --stage build ...
scripts/desktop-compat-benchmarks/run-macos-bench.sh --work /some/dir --name myapp   # opens windows
```

iOS and Android, size only (no simulator, no emulator, nothing installed):

```bash
scripts/desktop-compat-benchmarks/mobile-size.sh --target ios \
  --project /some/dir/myapp/cn1/app --work /some/dir/myapp-ios
ANDROID_HOME=... scripts/desktop-compat-benchmarks/mobile-size.sh --target android \
  --project /some/dir/myapp/cn1/app --work /some/dir/myapp-android
```

iOS is an unsigned Release build for a generic device (`CODE_SIGNING_ALLOWED=NO`);
the JSON gives the `.app`, its zip, and the executable before and after
`strip -x`. Android is `assembleRelease` of the generated Gradle project with
R8 on, signed with the throwaway key that project carries.

Every Maven invocation here passes `-Dopen=false`. Without it the Codename One
plugin opens a generated Xcode or Android Studio project in the IDE.

CI: `.github/workflows/desktop-compat-benchmarks.yml` runs the two in-tree
samples on Linux, macOS and Windows and uploads `benchmarks-<os>.json` and
`benchmarks-<os>.md`.

## Files

| File | |
|---|---|
| `bench.sh` | one application: both builds, sizes, and on Linux the measurements |
| `bench-samples.sh` | the in-tree samples through `bench.sh`, then the two files CI uploads |
| `x11bench.py` | the Linux driver: Xvfb, first-paint detection, input, memory, CPU |
| `benchutil.py` | sizes, the layer breakdown, environment facts, result assembly |
| `report.py` | `result.json` files to markdown or AsciiDoc tables |
| `input/` | input scripts |
| `Containerfile`, `install-linux-deps.sh`, `linux-container.sh` | the Linux environment |
| `javafx-jmods.sh` | OpenJFX jmods from the platform jars on Maven Central |
| `run-macos-bench.sh`, `firstwindow.swift` | the macOS runtime half |
| `win-measure.ps1` | the Windows runtime half (CI only) |
| `mobile-size.sh` | iOS `.app` and Android APK sizes of an imported application |

## Reading the numbers honestly

- **Ratios are baseline / Codename One.** Above 1 the native build is smaller
  or faster by that factor; below 1 it lost, and the report prints it the same
  way.
- **The baseline is not handicapped.** It gets a minimal runtime image, and the
  tuned variant gets every start-up and footprint switch a developer would
  reasonably ship. If a flag would help the JVM and is missing, that is a bug in
  this harness.
- **Xvfb is a software framebuffer.** Neither toolkit has a GPU here. That
  takes hardware-accelerated pipelines out of both columns; it does not favour
  one, but absolute paint times on real hardware will differ.
- **A window that paints is not an application that works.** The driver saves
  a screenshot of the idle and the after-script state of the first session
  (`logs/run-*/session-0-*.png`). Look at them before quoting a number: a
  native build that shows an error dialog starts very quickly.
- **The X server has had a GTK program on it.** The first GTK 3 program on an
  X server initialises GL to choose its visuals and records the choice on the
  root window; every later one reads it and loads no GL driver. A private Xvfb
  made each launch that first program, and Mesa's llvmpipe was 53 MB of a
  native Notepad's 108 MB resident set -- 56 MB when launched second. A desktop
  session is past that point before any application starts, so the driver runs
  one bare `gtk_init` on each new server first (`gtk_session` in every sample
  says whether it could). A Swing baseline never loads GTK and is unaffected;
  a JavaFX one loads GTK itself, and starts from the same server as the
  native build it is compared with.
- **RSS counts shared pages in full.** PSS is the fairer single-process figure;
  USS is what quitting the application gives back.
- **OpenJFX version.** The jmods default to OpenJFX 23.0.2, the oldest release
  that both runs on JDK 21 and publishes a linux-aarch64 build, whatever
  version the application's pom names.
