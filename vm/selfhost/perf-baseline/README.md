# Performance gate baselines

ParparVM / JDK 25 ratios that `vm/selfhost/perf-gate.py` holds every platform build to.

- `policy.json`: the global tolerance per metric and the RAM floor.
- `base/<platform>@<cpu-model>.json`: the consolidated rows. Written only by the nightly
  fold (`.github/workflows/perf-baseline.yml`); a pull request that edits it fails.
- `pr/<number>.json`: one pull request's calibrations and rebaselines, written by
  `calibrate-perf-baseline.py --pr <number>`. It is folded into `base/` after the merge.

The format and the rules for combining overlays are in `../perf_baseline.py`.
