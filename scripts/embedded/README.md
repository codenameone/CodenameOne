# Embedded Linux

Tools for running Codename One applications on embedded Linux boards through the
embedded variant of the native Linux port (`CN1_LINUX_EMBEDDED`: DRM/KMS, fbdev or
memory display, evdev input, no GTK). The user documentation is the developer guide's
"Embedded Linux devices" chapter (`docs/developer-guide/Embedded-Linux.asciidoc`).

| Path | What it is |
|---|---|
| `translate.sh` | Translates compiled application classes into the CMake project the board builds, from this checkout's own build output, with no build service. |
| `build-dist.sh` | Builds that project for `arm64`, `arm`, `amd64` or `riscv64` in a container (`Containerfile.dev`), strips the binary and keeps its symbols beside it. |
| `gen-embedded-stubs.py` | Generates `Ports/LinuxPort/nativeSources/cn1_linux_embedded_stubs.c`, the natives the embedded build leaves out; `--check` is the CI form. |
| `gpu-check/` | Draws every kind of operation with the GPU renderer and the software one, on Mesa's llvmpipe in the dev image, and fails on any differing pixel (`check-gpu.sh`). |
| `demo/` | `EmbeddedDemo`, a small HMI that is also the benchmark workload (`CN1_BENCH_SECONDS`). |
| `bench/` | The same workload in Slint, Qt, JavaFX and a Chromium kiosk, and the harness that runs all of them. |
| `pios/setup-device.sh` | Prepares Raspberry Pi OS Lite to start an application at boot. |
| `yocto/meta-codenameone` | Yocto layer: a `codenameone-app` class and an example recipe. |
| `buildroot/package/codenameone-app` | Buildroot package. |

## Building the demo

Needs `maven/core`, `maven/linux` and `vm/ByteCodeTranslator` built into this
checkout (`mvn -Dmaven.repo.local=$(pwd)/../.m2-repo -Plocal-dev-javase -pl core,linux,parparvm -am install`).

```bash
B=/tmp/demo; mkdir -p $B/classes
javac -source 1.8 -target 1.8 -cp maven/core/target/classes:maven/linux/target/classes \
    -d $B/classes scripts/embedded/demo/src/EmbeddedDemo.java
scripts/embedded/translate.sh $B/classes EmbeddedDemo $B/out
scripts/embedded/build-dist.sh $B/out arm64
CN1_DISPLAY=memory CN1_SIZE=800x480 CN1_BENCH_SECONDS=10 $B/out/build-arm64/EmbeddedDemo   # on the board
```

`CN1_SNAPSHOT=/tmp/shot.ppm` writes frame 60 (or `CN1_SNAPSHOT_FRAME`) to a file,
which is how the memory display is checked without a screen.

For the GPU renderer, build with `CN1_CMAKE_ARGS=-DCN1_EMBEDDED_GPU=ON` and, on a
machine without a GPU, run with `LIBGL_ALWAYS_SOFTWARE=1`; `check-demo-run.py
--reference` then holds its frame to the software build's.

## Benchmarks

```bash
docker build --platform linux/arm64 -t cn1-bench -f scripts/embedded/bench/Containerfile scripts/embedded/bench
docker run --rm --privileged --platform linux/arm64 -v $B/out/build-arm64:/cn1 cn1-bench /bench/run.sh /cn1 10 3
```

Each line is `framework cold|warm first_frame_ms fps cpu_pct pss_kb private_kb rss_kb
mapped_kb mapped_files`. `--privileged` is required: a cold start drops the page
cache first. `run.sh` explains each column; `results.txt` holds the last run. The developer guide's
table (`docs/developer-guide/embedded-benchmarks.adoc`) records the numbers and the
host they were taken on.

`bench/headless/` is the same comparison without a display: one JSON service in the
Codename One backend, Go, Rust, Node.js, Python and Spring Boot, loaded with `oha`.

```bash
docker build --platform linux/arm64 -t cn1-headless -f scripts/embedded/bench/headless/Containerfile scripts/embedded/bench/headless
docker run --rm --privileged --platform linux/arm64 -v <dir holding the backend binary>:/dist cn1-headless /bench/run.sh /dist/<binary> 3
```

The backend binary comes from `vm/backend/package.sh` (`CN1_BACKEND_DEMO=demo/bench`).
`run.sh` explains each column, and `results.txt` holds the last run.

CI holds the backend to these numbers (`.github/workflows/backend-performance.yml`). It runs
the comparison against Rust and Go on an arm64 runner, counts what each request allocates
with `census.sh` (a binary built with `CN1_BACKEND_CFLAGS="-DCN1_ALLOC_CENSUS
-DCN1_HEAP_HISTOGRAM"`), and `gate.py` fails the job when a limit is broken. The limits
are at the top of `gate.py`. The job uploads its results, census and charts, which
`charts.py` draws from the same data as the guide's charts.
