# PR 6019: Windows x64 string-building memory calibration

CPU: `AMD64 Family 26 Model 2 Stepping 1, AuthenticAMD`, four CPUs. Each report contains five interleaved rounds. These are existing hosted measurements; no native workflow was dispatched or rerun.

| Run/job | Artifact | RAM ratio median | Report SHA-256 |
| --- | --- | ---: | --- |
| [38681883624](https://github.com/codenameone/CodenameOne/actions/runs/38681883624/job/116248034828) | `11948899513` / `perf/perf-results.json` | 0.266562214 | `e8550a38168eb9094cc26e7cdd486c9d0d8760c18697843bd6541008693ac0df` |
| [38689265151](https://github.com/codenameone/CodenameOne/actions/runs/38689265151/job/116271143659) | `11952492356` / `perf/perf-results.json` | 0.266736306 | `d43756195d9d207b8e9fe372b6bc29455fe79652b9a215b1de6ea13d1478e1bf` |
| [38688282220](https://github.com/codenameone/CodenameOne/actions/runs/38688282220/job/116277654729) | `11953418308` / `perf/perf-results.json` | 0.268257110 | `75389b42974b4597fd7529a56325784df3ecdd491d988a19f3264885aebda65e` |

The pooled median is **0.267x**, consistent with the independently recorded [PR 5976 calibration](https://github.com/codenameone/CodenameOne/blob/0890827888cba6bb8710b7cf21224112f6221b82/vm/selfhost/perf-baseline/pr/5976.json). The previous 0.175x row came from a single run recorded in PR 6010. Across the three runs above, ParparVM peaks are 79.3–80.7 MB; JDK 25 peaks are 247.2–300.0 MB. The Word title/input changes do not modify the string benchmark, allocator, or collector. This recalibration records repeated measurements; it is not evidence of an intentional memory cost or proof of a particular cause for the older ratio.

Only this CPU/benchmark memory baseline changes. The 35% memory tolerance, 0.05x absolute floor, 1.386x timing baseline and 20% timing tolerance remain unchanged.

## Validation

Replay using `perf-gate.py:verdict` and the resolved `perf_baseline.load()` rows: all 26 metrics in each of the three reports pass. Each string-building memory median fails with the old 0.175x baseline and passes with 0.267x. No benchmark was rerun.

Reproduce the overlay with the three downloaded `perf/perf-results.json` reports:

```sh
python3 vm/selfhost/calibrate-perf-baseline.py --pr 6019 --only stringBuilding --metric memory \
  --reason "Repeated same-CPU measurements; see this evidence file" \
  earlier.json reported-failure.json independent-control.json
python3 vm/selfhost/perf_baseline.py check --pr 6019
```
