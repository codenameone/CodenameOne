# PR 6027: Linux translation performance calibration

Ratios are ParparVM / JDK 25. Each report contains five interleaved paired rounds on four CPUs, with verified output. These measure translation time and peak RSS, not editor startup or typing latency. Existing archived reports were used; this calibration did not dispatch a benchmark.

## Measurements

| Run | Source | CPU | Benchmark | Time median | RAM median | Report SHA-256 |
| --- | --- | --- | --- | ---: | ---: | --- |
| [38848075973](https://github.com/codenameone/CodenameOne/actions/runs/38848075973) | master | Neoverse-N2 | hello | 1.012598446 | 0.920985756 | `0c1e0de75f9f47f250da2235175b3a78098deabce13e4ca58cb8b81a31b8f024` |
| [38861108588](https://github.com/codenameone/CodenameOne/actions/runs/38861108588) | master | Neoverse-N2 | hello | 1.021270247 | 0.940316573 | `05857acb028225ee085d3d01f2c8a427fba57f50100263a7391879f62a7906a3` |
| [38848287858](https://github.com/codenameone/CodenameOne/actions/runs/38848287858) | PR 6026 | Neoverse-N2 | hello | 1.030690121 | 0.931268660 | `115238b0cad2ca71e885a0f515c31b05c7224c2d45fa3153c2778e23567e0c32` |
| [38876165702](https://github.com/codenameone/CodenameOne/actions/runs/38876165702) | PR 6026 | Neoverse-N2 | hello | 1.078412961 | 0.907708115 | `94eb1578d7c7d8e93ba05803f5232b27e9f845ac0777dc0b556422357030cb18` |
| [38878637047](https://github.com/codenameone/CodenameOne/actions/runs/38878637047) | PR 6027 | Neoverse-N2 | hello | 1.017838131 | 0.931618386 | `3c107c051f5d23b30bf149b869a74f86a95ddfac141d9518f0c161804ddb0c56` |
| [38876165702](https://github.com/codenameone/CodenameOne/actions/runs/38876165702) | PR 6026 | Xeon Platinum 8573C | translator | 0.666364164 | 0.444110455 | `b179bad6159e6f3fa257fb4f873b805a77568f9ac70a3cdaaf1bac0a8445a68c` |
| [38878637047](https://github.com/codenameone/CodenameOne/actions/runs/38878637047) | PR 6027 | Xeon Platinum 8573C | translator | 0.663889267 | 0.436177843 | `59f1a23b699adfed43f65c86021da5b5b3f632e7517ada35417494f1c509eb6b` |

Download `linux-screenshot-raw-arm64` or `linux-screenshot-raw-x64` from each run and read `perf/perf-results.json`. Run 38876165702 uses the completed first-attempt artifacts; its later retry was canceled after the Linux PR merged.

## Source provenance

The two master runs checked out `aa31e41d3fe7599163407d5547c99c5923629abe`. The PR runs checked out GitHub merge commits `82903452e17e73d7b1a9769b44759fa37c817ce2` (38848287858), `6982fbc6edd5d9e7fa2280af64bb519682ccd4f4` (38876165702), and `fa0a3baf8a0c2f94d92d326ce430b5367cee698a` (38878637047). Their source tree hashes agree:

- `vm/ByteCodeTranslator`: `bddec40acc84f649773b256e460da612a4487b21`.
- `vm/JavaAPI`: `af99698584dd0cdb62bc6e98cea807df7c0519bb`.
- `vm/benchmarks`: `670af49d9ceff3cea55c4184390637623264545c`.

The `hello` application inputs evolve with the framework and port; identical VM trees do not establish an identical application corpus. The ARM64 sample includes two master controls and three editor-branch measurements. The x64 translator benchmark translates the translator itself and does not execute editor UI code.

## Changes and limits

- ARM64 `hello` time: **0.931x to 1.021x**, the median of five run medians (range 1.013x–1.078x). Keep the 15% tolerance and 0.868x memory baseline. One of these five reports fails the former timing baseline.
- Xeon 8573C `translator`: **0.505x to 0.665x time**, **0.517x to 0.440x memory**, medians of two independent run medians. The former row came from one run. Both newer reports fail its time threshold, and one fails its memory improvement threshold. Keep the existing 20% time and 15% memory tolerances, plus the 0.05x absolute memory floor. Two reports are limited evidence; no narrower tolerance or intentional runtime performance change is inferred.
- Other CPU models, benchmarks, metrics, and the shared policy are unchanged. This records measured ratios; it does not identify the cause of the older calibration.

## Validation

All **182 metric verdicts** across these seven archived reports pass when replayed through `perf-gate.py:verdict` against `perf_baseline.load()` with the PR overlay. The old baseline fails four verdicts: one ARM64 time regression, two x64 time regressions, and one x64 memory improvement.

Reproduce from the downloaded reports (ARM filenames in table order, then x64 filenames in table order):

```sh
python3 vm/selfhost/calibrate-perf-baseline.py --pr 6027 --all --only hello --metric time \
  --reason "Five matching-CPU reports; see evidence/pr-6027-linux.md" \
  arm-master-1.json arm-master-2.json arm-pr-1.json arm-pr-2.json arm-pr-3.json
python3 vm/selfhost/calibrate-perf-baseline.py --pr 6027 --all --only translator \
  --reason "Two matching-CPU reports; see evidence/pr-6027-linux.md" \
  x64-pr-1.json x64-pr-2.json
python3 -m unittest discover -s vm/selfhost -p test_perf_gate.py
python3 vm/selfhost/perf_baseline.py check --pr 6027 --base origin/master
```
