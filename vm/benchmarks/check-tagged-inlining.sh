#!/bin/bash
# A tagged immediate must not pay a CALL to reach bits it already carries.
#
#   check-tagged-inlining.sh [binary]         default: vm/selfhost/target/parpar-O3
#   check-tagged-inlining.sh --write-baseline
#
# WHY THIS EXISTS. Integer.valueOf is the whole construction of a tagged box and
# should compile to a shift and an OR. It was instead an out-of-line call
# carrying an atomic acquire load, and a dense profile put it at 17% of mutator
# time in a HashMap workload calling it 6M times per rep -- for a value type that
# is supposed to be free. HotSpot ALLOCATES a heap Integer for those keys and
# still won.
#
# The cause was invisible in the source: CN1_FORCE_BOX_CLINIT declared its flag
# as a STATIC LOCAL, and a function holding a mutable static local cannot be
# duplicated, so ThinLTO declines to inline it across modules. Nothing in the C
# says "this will not inline"; only the disassembly does. The same shape appeared
# three times in one session -- cn1HmMarker reaching a tagged hash through two
# tag resolves and a virtual dispatch, valueOf here, and cn1InlSbAppendStr grown
# too large to inline -- so it is a pattern, not an accident, and it is worth a
# gate rather than another profile.
#
# The check is a RATCHET on out-of-line CALL SITES, not a demand for zero. Some
# sites legitimately keep a call (cold paths, address-taken uses). What must
# never happen is the count JUMPING, which is exactly what "it stopped inlining
# everywhere" looks like.
set -e
cd "$(dirname "$0")"
REPO="$(cd ../.. && pwd)"
BIN="${1:-$REPO/vm/selfhost/target/parpar-O3}"
BASE="tagged-inlining-baseline.txt"
WRITE=0
[ "$1" = "--write-baseline" ] && { WRITE=1; BIN="$REPO/vm/selfhost/target/parpar-O3"; }
if [ ! -x "$BIN" ]; then
    echo "check-tagged-inlining: $BIN missing -- run vm/selfhost/build-selfhost.sh -O3 first" >&2
    exit 1
fi
DIS=$(mktemp -t taginl); trap 'rm -f "$DIS"' EXIT
otool -tv "$BIN" > "$DIS" 2>/dev/null
[ -s "$DIS" ] || { echo "check-tagged-inlining: otool produced nothing for $BIN" >&2; exit 1; }

python3 - "$DIS" "$BASE" "$WRITE" <<'PY'
import re,sys,os
dis,basefile,write=sys.argv[1],sys.argv[2],sys.argv[3]=="1"
d=open(dis).read()
# Operations on tagged immediates that must stay inlined. valueOf CONSTRUCTS a
# box, cn1Value/intValue READ one, hashCode/equals are what a Map calls on it.
syms=["java_lang_Integer_valueOf___int_R_java_lang_Integer",
      "java_lang_Short_valueOf___short_R_java_lang_Short",
      "java_lang_Character_valueOf___char_R_java_lang_Character",
      "java_lang_Float_valueOf___float_R_java_lang_Float",
      "java_lang_Long_valueOf___long_R_java_lang_Long",
      "java_lang_Double_valueOf___double_R_java_lang_Double",
      "java_lang_Integer_cn1Value___R_int",
      "java_lang_Long_cn1Value___R_long",
      "java_lang_Integer_intValue___R_int",
      "java_lang_Integer_hashCode___R_int",
      "java_lang_Integer_equals___java_lang_Object_R_boolean"]
cur={s:len(re.findall(r'\tbl\t_'+re.escape(s)+r'\n',d)) for s in syms}
if write:
    with open(basefile,'w') as f:
        f.write("# out-of-line call sites per tagged-box operation; see check-tagged-inlining.sh\n")
        for s in syms: f.write(f"{s} {cur[s]}\n")
    print("wrote "+basefile)
    for s in syms: print(f"  {cur[s]:5d}  {s}")
    sys.exit(0)
if not os.path.exists(basefile):
    print(f"check-tagged-inlining: {basefile} missing -- run --write-baseline",file=sys.stderr); sys.exit(1)
base={}
for line in open(basefile):
    if line.startswith('#') or not line.strip(): continue
    k,v=line.split(); base[k]=int(v)
bad=[]
for s in syms:
    b=base.get(s)
    if b is None: bad.append((s,None,cur[s])); continue
    if cur[s]>b: bad.append((s,b,cur[s]))
if bad:
    print("TAGGED INLINING REGRESSED -- a value type is paying a call for bits it carries:")
    for s,b,c in bad:
        print(f"  {s}\n      baseline {b if b is not None else 'absent'} -> now {c}")
    print("\nA jump here means something stopped inlining. The usual cause is a new")
    print("static local or a body grown past the inliner's budget; check the")
    print("disassembly rather than the C, which will look unchanged.")
    sys.exit(1)
improved=[(s,base[s],cur[s]) for s in syms if s in base and cur[s]<base[s]]
print(f"tagged inlining OK ({len(syms)} operations checked)")
for s,b,c in improved: print(f"  improved: {s} {b} -> {c}  (re-run --write-baseline to lock it in)")
PY
