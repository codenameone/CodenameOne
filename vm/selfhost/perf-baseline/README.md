# Performance gate baselines

ParparVM / JDK 25 ratios that `vm/selfhost/perf-gate.py` holds every platform build to.

- `policy.json`: the global tolerance per metric and the RAM floor.
- `base/<platform>@<cpu-model>.json`: the consolidated rows. Written only by the nightly
  fold (`.github/workflows/perf-baseline.yml`); a pull request that edits it fails.
- `pr/<number>.json`: one pull request's calibrations and rebaselines, written by
  `calibrate-perf-baseline.py --pr <number>`. It is folded into `base/` after the merge.

A ratio past its tolerance BELOW the baseline is reported but does not fail the gate.
The same nightly job then runs `../tighten-perf-baseline.py` over the `perf-results-*`
artifacts of master's last seven days of gate runs: a row that at least three of them
beat by more than half its tolerance (and by the RAM floor) is lowered to the worst of
those runs, so a later change that gives the win back fails as a regression.

The format and the rules for combining overlays are in `../perf_baseline.py`.

The same rules, run with a second layout, keep the Flutter benchmark's regression
baselines in `scripts/flutter-bench/baseline/` (`scripts/flutter-bench/flutter_baseline.py`);
the nightly fold and the pull request check cover both.
