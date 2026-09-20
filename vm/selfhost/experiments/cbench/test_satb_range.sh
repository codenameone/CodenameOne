#!/bin/bash
# Proves cn1SatbMoveLostRange (cn1_globals.h) by exhaustion, then proves the
# proof can FAIL.
#
# The second half is not ceremony. The GC gates that ought to have covered this
# barrier do not: with the deletion barrier removed outright, run-gc-verify.sh
# and run-gauntlet.sh both report GREEN. A check that cannot fail reports
# success for code it never examined, so this one injects three faults and
# requires each to be caught.
#
# The function is EXTRACTED from the header rather than copied, so the test
# cannot pass against a stale duplicate while the real code drifts.
set -e
cd "$(dirname "$0")"
H=../../../ByteCodeTranslator/src/cn1_globals.h

extract() {
    awk '/^static inline JAVA_INT cn1SatbMoveLostRange/,/^}/' "$H" > satb_extracted.h
    local lines; lines=$(wc -l < satb_extracted.h)
    if [ "$lines" -lt 8 ]; then
        echo "EXTRACTION FAILED: $lines lines from $H -- signature changed?" >&2
        exit 1
    fi
}

echo "== real source =="
extract
cc -O2 -Wall -Wextra -Werror -o test_satb_range test_satb_range.c
./test_satb_range

echo
echo "== non-vacuity: each injected fault must be caught =="
fail=0
inject() {
    local name=$1 sedexpr=$2
    extract
    sed -i '' "$sedexpr" satb_extracted.h
    cc -O2 -w -o test_satb_range_bad test_satb_range.c
    if ./test_satb_range_bad > /dev/null 2>&1; then
        echo "  NOT CAUGHT: $name   <-- the test is vacuous"
        fail=1
    else
        echo "  caught: $name"
    fi
    rm -f test_satb_range_bad
}
# 1. the whole barrier removed -- what the GC gates failed to notice
inject "lost range always empty"      's/JAVA_INT d = to - from;/JAVA_INT d = to - from; if(1) { *lostStart = to; return 0; }/'
# 2. off-by-one in the shift-right start
inject "shift-right start off by one" 's/(from + count)/(from + count + 1)/'
# 3. the two directions swapped
inject "length off by one (right)"    's/return (count < d) ? count : d;/return ((count < d) ? count : d) - 1;/'
extract   # leave the real extraction in place
rm -f test_satb_range_bad
if [ "$fail" != "0" ]; then
    echo "TEST IS VACUOUS -- fix it before trusting the PASS above" >&2
    exit 1
fi
echo "all injected faults caught: the PASS above is meaningful"
