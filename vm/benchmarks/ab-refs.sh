#!/bin/bash
# A/B the java.lang.ref retention policies (CN1_REF_POLICY) against the behaviour
# they replaced.
#
#   noweak    -DCN1_NO_WEAK_REFS  references are traced strongly and never cleared.
#                                 This is what ParparVM did before references existed,
#                                 and what the iOS port's soft-reference table still
#                                 does between memory warnings.
#   pressure  -DCN1_REF_POLICY=0  keep every soft referent until headroom drops into
#                                 the reserve, then drop all of them at once -- the
#                                 GC-integrated form of didReceiveMemoryWarning ->
#                                 flushSoftRefMap.
#   never     -DCN1_REF_POLICY=1  never clear a soft referent. The upper bound on hit
#                                 rate and on footprint.
#   ranked    -DCN1_REF_POLICY=2  clear by age since the last get(). The default.
#
# THE POINT OF ALL FOUR IS THAT NEITHER AXIS MEANS ANYTHING ALONE. "never" wins the
# hit rate by keeping everything and "pressure" wins the footprint by keeping nothing;
# ranking is worth its one int field and one store per get() only if it holds a higher
# hit rate than "pressure" AT A COMPARABLE FOOTPRINT. The table below prints both for
# every arm at every ceiling so that comparison cannot be made one column at a time.
#
#   ./ab-refs.sh [reps] [ceilingMB ...]
#
# REF_WORKLOAD passes argv to the driver ("keys payloadBytes accesses churn"), and
# REF_TIMEOUT bounds a single run.
#
# CHOOSE CEILINGS WHERE EVERY ARM COMPLETES RELIABLY. Below roughly 1.8x the cache
# size this workload becomes BISTABLE: once the retained set stops the collector
# freeing enough, the process-budget pacing loop parks the mutator and throughput
# collapses by two orders of magnitude, and whether a given run falls into that state
# is timing-sensitive. Measured on this host, the same arm took 936ms and 391s on
# neighbouring workload sizes, and the arms ordered differently each time. Single runs
# in that regime measure the coin, not the policy -- if the tight regime is what you
# want to characterise, count how many of N runs complete rather than timing one.
#
# Requirements: JDK_8_HOME, Maven, clang.
set -e
cd "$(dirname "$0")"
REPS="${1:-5}"; shift || true
CEILINGS="${*:-96 128 192}"
LTO="${CN1_BENCH_LTO--flto=thin}"
mkdir -p target/ab-refs

# CN1_GC_CONFORM is in every arm. It changes no allocator behaviour -- unlike
# CN1_GC_VERIFY, which forces cn1BibopReleaseOffset() to 0 and compiles out page
# release and the major sweep, so a footprint measured in a verifier build is a
# measurement of the verifier. It is what supplies [GCREF], and it is present in ALL
# arms so the arms differ in one thing only.
build() { # name, flags
  CN1_BENCH_CFLAGS="$LTO -DCN1_GC_CONFORM $2" ./translate-and-build.sh RefPolicy \
      "target/ab-refs/$1" >"target/ab-refs/$1.build.log" 2>&1 \
      || { echo "BUILD FAILED: $1"; tail -25 "target/ab-refs/$1.build.log"; exit 1; }
  echo "built $1"
}
build noweak   "-DCN1_NO_WEAK_REFS"
build pressure "-DCN1_REF_POLICY=0"
build never    "-DCN1_REF_POLICY=1"
build ranked   "-DCN1_REF_POLICY=2"

REPS="$REPS" CEILINGS="$CEILINGS" python3 - <<'EOF'
import subprocess, re, os, sys, statistics

ARMS = ["noweak", "pressure", "never", "ranked"]
reps = int(os.environ["REPS"])
ceilings = [int(c) for c in os.environ["CEILINGS"].split()]

def run(arm, ceiling_mb):
    env = dict(os.environ)
    # Scrub every CN1_* the caller may have exported. An inherited CN1_GC_PROBE or
    # CN1_SIMULATE_PROC_MEMORY_LIMIT would silently make two arms incomparable, which
    # reads as a policy difference rather than as a mistake.
    for k in [k for k in env if k.startswith("CN1_")]:
        del env[k]
    env["CN1_SIMULATE_PROC_MEMORY_LIMIT"] = str(ceiling_mb * 1024 * 1024)
    env["CN1_GC_PROBE"] = "1"
    p = subprocess.run([f"target/ab-refs/{arm}"] + os.environ.get("REF_WORKLOAD", "").split(),
                       capture_output=True, text=True, env=env,
                       timeout=float(os.environ.get("REF_TIMEOUT", "600")))
    out = p.stdout
    def num(key, default=None):
        m = re.search(rf'^{key}=(-?\d+)', out, re.M)
        if m: return int(m.group(1))
        if default is not None: return default
        raise SystemExit(f"{arm}@{ceiling_mb}MB: no {key} in output\n{out}\n{p.stderr[-2000:]}")
    refms, cleared, retained = [], 0, 0
    for m in re.finditer(r'\[GCREF\].*?cleared=(\d+).*?refMs=([\d.]+)', p.stderr):
        cleared += int(m.group(1)); refms.append(float(m.group(2)))
    for m in re.finditer(r'\[GCREF\].*?retained=(\d+)', p.stderr):
        retained += int(m.group(1))
    markms = [float(m.group(1)) for m in re.finditer(r'markMs=([\d.]+)', p.stderr)]
    return {
        "hit_ppm": num("HIT_RATE_PPM"), "fp_kb": num("FINAL_FOOTPRINT_KB"),
        "checksum": num("RESULT"),
        "weak_cleared": out.split("WEAK_DEAD_CLEARED=")[1].split("\n")[0] if "WEAK_DEAD_CLEARED=" in out else "?",
        "refms": sum(refms), "markms": sum(markms), "cleared": cleared, "retained": retained,
    }

results = {(a, c): [] for a in ARMS for c in ceilings}
for rep in range(reps):
    # INTERLEAVED. Physical footprint moves with the host's own memory pressure, so two
    # soaks taken minutes apart measure the machine; every arm has to see the same
    # machine state, which only holds if they alternate inside one session.
    for c in ceilings:
        for a in ARMS:
            results[(a, c)].append(run(a, c))
    print(f"rep {rep+1}/{reps}", flush=True)

# Checksum parity. A retention policy decides WHEN a payload is rebuilt, never what it
# contains, and the driver accumulates what it read rather than what it rebuilt -- so a
# checksum that moves across arms is a correctness bug, not a policy difference.
sums = {(a, c): {r["checksum"] for r in rs} for (a, c), rs in results.items()}
allsums = set().union(*sums.values())
if len(allsums) != 1:
    print("\nCHECKSUM MISMATCH across arms (correctness bug):")
    for k, v in sorted(sums.items()):
        print(f"  {k}: {sorted(v)}")
    sys.exit(1)

med = lambda vals: statistics.median(vals)
print(f"\nchecksum {allsums.pop()} identical across every arm and ceiling"
      f"   ({reps} interleaved reps, medians below)")
for c in ceilings:
    print(f"\n--- process ceiling {c} MB " + "-" * 46)
    # refMs and markMs are TOTALS over the run's cycles, so their ratio is the share
    # of collector time the reference phase costs -- which is the question. The
    # absolute ms is kept beside it only so a suspiciously round ratio can be checked.
    print(f"{'arm':<10}{'hit rate %':>12}{'footprint MB':>14}{'refMs total':>13}"
          f"{'% of mark':>11}{'softCleared':>12}{'weak':>10}")
    for a in ARMS:
        rs = results[(a, c)]
        hit = med([r["hit_ppm"] for r in rs]) / 10000.0
        fp = med([r["fp_kb"] for r in rs]) / 1024.0
        ref = med([r["refms"] for r in rs])
        mark = med([r["markms"] for r in rs])
        share = (100.0 * ref / mark) if mark > 0 else 0.0
        print(f"{a:<10}{hit:>11.2f}%{fp:>14.1f}{ref:>13.3f}{share:>10.2f}%"
              f"{med([r['cleared'] for r in rs]):>12.0f}{rs[0]['weak_cleared']:>10}")
EOF
