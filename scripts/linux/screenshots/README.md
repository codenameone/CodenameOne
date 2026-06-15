# Native Linux port screenshot baselines (x86_64)

Golden PNGs for the native Linux (GTK3/Cairo/Pango) port's hellocodenameone
screenshot suite on **x86_64**, one `<testName>.png` per test, compared pixel-by-
pixel by the `linux-build-run.yml` workflow's `compare-comment` job (via
`scripts/lib/cn1ss.sh`). The arm64 baselines live in `../screenshots-arm`.

## Seeding / updating

The suite renders ~112 screenshots. Baselines are produced by a green run of the
`build-run` job and then committed:

1. Trigger `linux-build-run.yml` (push to a branch touching `Ports/LinuxPort/**`
   or run it manually). The `build-run` (x64) job uploads a
   `linux-screenshot-raw-x64` artifact.
2. Download that artifact and copy its `*.png` into this directory:
   ```bash
   gh run download <run-id> -n linux-screenshot-raw-x64 -D /tmp/lx
   cp /tmp/lx/*.png scripts/linux/screenshots/
   git add scripts/linux/screenshots/*.png && git commit -m "Seed Linux x64 screenshot baselines"
   ```
3. Repeat with `linux-screenshot-raw-arm64` into `../screenshots-arm`.

Rendering is deterministic (Cairo software rasterization), so a given test's PNG
is stable across runs on the same arch; x64 and arm64 can differ slightly in
anti-aliasing/text rasterization, which is why each arch has its own baseline
set (mirroring the iOS GL vs Metal split).

## Gating

The `compare-comment` job posts a PR comment with diffs but does not fail the
build until baselines exist. Once seeded, set `CN1SS_FAIL_ON_MISMATCH=1` in the
compare step to gate regressions (and `CN1SS_ALLOWED_MISSING` for any tests that
legitimately do not render on this port).
