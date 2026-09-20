/*
 * EXHAUSTIVE proof for cn1SatbMoveLostRange (cn1_globals.h).
 *
 * The narrowed SATB deletion barrier claims that a move WITHIN one reference
 * block only owes the snapshot the slots whose old contents do not survive the
 * move, and that this set is contiguous. Everything downstream rests on that
 * claim, and the GC gates do not cover it: with the barrier removed entirely,
 * run-gc-verify.sh and run-gauntlet.sh are both GREEN. So it is proved here
 * instead, by brute force against a model, over every shape in range.
 *
 * The function under test is EXTRACTED FROM THE HEADER at build time (see
 * test_satb_range.sh) rather than copied, so this cannot pass against a stale
 * duplicate of the code it is meant to check.
 *
 * Model: label each slot with a distinct id, perform the move on the model, and
 * ask which slots' OLD values are absent from the whole block afterwards. That
 * set is what the barrier owes. The test asserts the reported range covers it
 * exactly-or-conservatively (a superset is safe, a subset is heap corruption)
 * and separately reports whether it was exact.
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

typedef int JAVA_INT;
#include "satb_extracted.h"

#define MAXN 24

int main(void) {
    long shapes = 0, exact = 0, superset = 0;
    int worstOver = 0;

    for(int n = 1 ; n <= MAXN ; n++) {
        int before[MAXN], after[MAXN];
        for(int from = 0 ; from < n ; from++) {
            for(int to = 0 ; to < n ; to++) {
                for(int count = 1 ; count <= n ; count++) {
                    if(from + count > n || to + count > n) continue;
                    shapes++;

                    /* model: slot j holds id j+1 (0 = empty/none) */
                    for(int j = 0 ; j < n ; j++) before[j] = j + 1;
                    memcpy(after, before, sizeof(int) * n);
                    memmove(after + to, before + from, sizeof(int) * (size_t)count);

                    /* an id survives if it appears ANYWHERE in the block after */
                    int survives[MAXN + 1];
                    memset(survives, 0, sizeof(survives));
                    for(int j = 0 ; j < n ; j++) survives[after[j]] = 1;

                    /* truth: overwritten slots whose OLD id is gone from the block */
                    int owed[MAXN]; int owedCount = 0;
                    memset(owed, 0, sizeof(owed));
                    for(int j = to ; j < to + count ; j++) {
                        if(!survives[before[j]]) { owed[j] = 1; owedCount++; }
                    }

                    JAVA_INT lostStart = -1;
                    JAVA_INT lostLen = cn1SatbMoveLostRange(from, to, count, &lostStart);

                    if(lostLen < 0 || (lostLen > 0 && (lostStart < 0 || lostStart + lostLen > n))) {
                        printf("FAIL out-of-range n=%d from=%d to=%d count=%d -> start=%d len=%d\n",
                               n, from, to, count, lostStart, lostLen);
                        return 1;
                    }
                    /* SAFETY: every owed slot must be inside the reported range */
                    for(int j = 0 ; j < n ; j++) {
                        if(owed[j] && !(j >= lostStart && j < lostStart + lostLen)) {
                            printf("FAIL MISSED owed slot %d  n=%d from=%d to=%d count=%d"
                                   "  -> start=%d len=%d (owed=%d)\n",
                                   j, n, from, to, count, lostStart, lostLen, owedCount);
                            return 1;
                        }
                    }
                    if(lostLen == owedCount) exact++;
                    else { superset++; if(lostLen - owedCount > worstOver) worstOver = lostLen - owedCount; }
                }
            }
        }
    }
    printf("PASS  shapes=%ld  exact=%ld  conservative-superset=%ld  worst over-report=%d slot(s)\n",
           shapes, exact, superset, worstOver);
    printf("      no shape under-reported: a missed slot is heap corruption and would have failed above.\n");
    return 0;
}
