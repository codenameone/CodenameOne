#!/bin/bash
# A/B the JLS-saturating float/double -> int/long narrowing against the old undefined C
# cast, interleaved inside ONE session because this host cannot resolve a 5% difference
# between two runs minutes apart. Both arms come from the SAME translation; only
# -DCN1_NO_SATURATING_NARROWING differs, so nothing but the conversion changes.
set -e
cd "$(dirname "$0")"
ROUNDS="${1:-8}"
DRIVER="${2:-Bench}"
LTO="${CN1_BENCH_LTO--flto=thin}"
mkdir -p target/ab
CN1_BENCH_CFLAGS="$LTO -DCN1_NO_SATURATING_NARROWING" ./translate-and-build.sh "$DRIVER" target/ab/narrow-off >/dev/null 2>&1
echo "built arm OFF (old undefined cast)"
CN1_BENCH_CFLAGS="$LTO" ./translate-and-build.sh "$DRIVER" target/ab/narrow-on >/dev/null 2>&1
echo "built arm ON (saturating)"

python3 - "$ROUNDS" <<'EOF'
import subprocess, re, sys, math
rounds = int(sys.argv[1])
ARMS = {"OFF": "target/ab/narrow-off", "ON": "target/ab/narrow-on"}

def run(p):
    # A benchmark that dies part way still prints the BENCH lines it reached. Comparing
    # those reports a partial run as a result, and if both arms die at the same point their
    # partial checksums even agree -- so the parity check would pass on nothing.
    proc = subprocess.run([p], capture_output=True, text=True)
    if proc.returncode != 0:
        raise SystemExit("%s exited %d; refusing to compare a partial run:\n%s"
                         % (p, proc.returncode, proc.stdout[-2000:]))
    out = proc.stdout
    r = {}
    for m in re.finditer(r'BENCH (\w+) rep \d+ ns=(\d+) checksum=(-?\d+)', out):
        r.setdefault(m.group(1), {"ns": [], "ck": set()})
        r[m.group(1)]["ns"].append(int(m.group(2)))
        r[m.group(1)]["ck"].add(m.group(3))
    if not r:
        raise SystemExit("%s produced no BENCH lines" % p)
    return r

best = {a: {} for a in ARMS}
cks = {a: {} for a in ARMS}
for i in range(rounds):
    for a, p in ARMS.items():
        for k, v in run(p).items():
            best[a][k] = min(best[a].get(k, 1 << 62), min(v["ns"]))
            cks[a].setdefault(k, set()).update(v["ck"])
    print("round %d/%d" % (i + 1, rounds), flush=True)

if set(best["OFF"]) != set(best["ON"]):
    print("ARMS DISAGREE ON WHICH WORKLOADS RAN: %s vs %s"
          % (sorted(best["OFF"]), sorted(best["ON"]))); sys.exit(1)
bad = [k for k in best["OFF"] if cks["OFF"].get(k) != cks["ON"].get(k)]
if bad:
    print("CHECKSUM MISMATCH between arms: %s" % bad); sys.exit(1)

print("\n%-20s %10s %10s %8s" % ("bench", "OFF ms", "ON ms", "ON/OFF"))
rs = []
for n in best["OFF"]:
    o = best["OFF"][n] / 1e6; w = best["ON"][n] / 1e6
    rs.append(w / o)
    print("%-20s %10.1f %10.1f %8.3f" % (n, o, w, w / o))
print("\ngeomean %.4f (>1 = the saturating conversion is slower)"
      % math.exp(sum(map(math.log, rs)) / len(rs)))
print("(checksums bit-identical across both arms)")
EOF
