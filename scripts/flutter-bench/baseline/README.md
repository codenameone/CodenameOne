# Flutter benchmark regression baselines

The rows `scripts/flutter-bench/flutter_baseline.py` holds every platform's
benchmark leg to: Codename One's own sizes (absolute) and its start-up and
memory as ratios to Flutter from the same run.

- `policy.json`: the global tolerance and floor per metric, and the metrics a
  platform records without gating, each with its reason.
- `base/<platform>[@<cpu-model>].json`: the consolidated rows. Written only by
  the nightly fold (`.github/workflows/perf-baseline.yml`); a pull request that
  edits them fails.
- `pr/<number>.json`: one pull request's calibrations and rebaselines, written
  by `flutter_baseline.py calibrate --pr <number>`. It is folded into `base/`
  after the merge.

The rules are the ParparVM performance gate's (`vm/selfhost/perf_baseline.py`);
what this layout adds is in `../flutter_baseline.py` and `../README.md`.
