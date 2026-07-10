package com.bench;

/**
 * Targeted reproducer for the generational-adoption "matured object dies" path that the
 * other tortures miss (they churn mostly leaf objects, or drop trees before they survive
 * the TWO GC cycles tenure needs to mature). Here each round:
 *   1. builds many DEEP non-leaf trees (Node has child refs -> markFunction != 0, the
 *      only shape that gets matured),
 *   2. forces two GCs while holding them, so they graduate into the legacy mark/sweep
 *      (heapPosition -4),
 *   3. drops them and forces another GC, so the matured objects DIE and flow through the
 *      legacy sweep + freeAndFinalize -- the path that corrupted the heap.
 * Folds node values into a checksum so nothing is eliminated. Prints DONE; a crash
 * (SIGSEGV / glibc "corrupted unsorted chunks" abort) is the failure.
 */
public class AdoptDeath {
    static final class Node {
        int v;
        Node a, b, c;   // multiple ref fields => non-leaf, deep tree
        Node(int v) { this.v = v; }
    }

    static Node build(int depth, int seed) {
        if (depth <= 0) return null;
        Node n = new Node(seed);
        n.a = build(depth - 1, seed * 3 + 1);
        n.b = build(depth - 1, seed * 3 + 2);
        n.c = build(depth - 1, seed * 3 + 3);
        return n;
    }

    static long walk(Node n) {
        if (n == null) return 0;
        return n.v + walk(n.a) + walk(n.b) + walk(n.c);
    }

    public static void main(String[] args) {
        long checksum = 0;
        int rounds = 40;
        int held = 60;
        for (int r = 0; r < rounds; r++) {
            Node[] hold = new Node[held];
            for (int i = 0; i < held; i++) {
                hold[i] = build(6, r * 131 + i);   // ~1093 nodes each
            }
            // survive two cycles -> tenure/mature the whole surviving subtree
            System.gc();
            System.gc();
            for (int i = 0; i < held; i++) {
                checksum += walk(hold[i]);
            }
            // now kill them: matured (-4) objects die -> legacy sweep path
            for (int i = 0; i < held; i++) {
                hold[i] = null;
            }
            System.gc();
            System.gc();
            // reallocate different-sized garbage so freed slots get reused/churned
            long s = 0;
            for (int i = 0; i < 5000; i++) {
                s += ("x" + i + "y" + (i * 7)).length();
            }
            checksum += s;
        }
        System.out.println("DONE " + checksum);
    }
}
