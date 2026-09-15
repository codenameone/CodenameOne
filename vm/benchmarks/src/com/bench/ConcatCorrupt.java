package com.bench;

/**
 * Heap integrity ACROSS the fused string-concat natives.
 *
 * String.cn1ConcatN (cn1FusedConcat2..5) reads raw interior pointers into its
 * source Strings' byte[]s and then ALLOCATES, which can collect. If the sources
 * were not rooted across that allocation, the collector could reclaim the very
 * bytes the copy reads and hand the block to another allocation -- and the
 * damage would surface somewhere else entirely, as an unrelated array whose
 * header has been overwritten.
 *
 * So this does not check the strings. It checks EVERYTHING ELSE: a population of
 * int[] with known lengths and known contents is held live across heavy concat
 * traffic, and every element is re-verified each round. A wrong length or a
 * wrong element is the corruption, reported at the round that produced it.
 *
 * WHAT THIS IS AND IS NOT. It is a REGRESSION GUARD, not a proof. Under the
 * shipping configuration the hazard above does not exist: the source Strings are
 * C parameters of the native, so conservative root scanning of that frame's
 * native stack keeps them and their fused byte[] alive across the allocation,
 * and the interior pointers resolve to the owning block. That was established
 * the hard way -- a GC bracket was added to these natives on the strength of the
 * hazard and then withdrawn, because the premise was wrong.
 *
 * Which means this driver passing proves the property still holds, and cannot by
 * itself prove the natives root anything themselves. Two changes would make it
 * bite, and it is here for both: turning conservative roots off
 * (-DCN1_DISABLE_CONSERVATIVE_GC_ROOTS, which also needs the translator run with
 * -Dcn1.frameless.objects=false -Dcn1.frameless.instance=false), or rewriting
 * cn1FusedConcatN so the sources stop being live C locals across the allocation.
 * Do not quote a green run here as evidence that the natives are correct in
 * isolation; quote it as evidence that conservative rooting still covers them.
 *
 * Output must match HotSpot byte-for-byte.
 */
public class ConcatCorrupt {
    static final int ARRAYS = 256;
    static final int LEN = 1000;

    static int[][] live = new int[ARRAYS][];
    static String[] keep = new String[64];

    static void fill() {
        for (int i = 0; i < ARRAYS; i++) {
            live[i] = new int[LEN];
            for (int j = 0; j < LEN; j++) {
                live[i][j] = (i * 31) + j;
            }
        }
    }

    /** returns the number of corrupted observations */
    static int verify(int round) {
        int bad = 0;
        for (int i = 0; i < ARRAYS; i++) {
            int[] a = live[i];
            if (a.length != LEN) {
                System.out.println("CORRUPT round=" + round + " array=" + i
                        + " length=" + a.length + " expected=" + LEN);
                bad++;
                continue;
            }
            for (int j = 0; j < LEN; j++) {
                if (a[j] != (i * 31) + j) {
                    System.out.println("CORRUPT round=" + round + " array=" + i
                            + " index=" + j + " value=" + a[j]
                            + " expected=" + ((i * 31) + j));
                    bad++;
                    break;
                }
            }
        }
        return bad;
    }

    public static void main(String[] args) {
        long ck = 0;
        int bad = 0;
        fill();
        // Operands are built at runtime so javac cannot constant-fold the
        // concatenations away; each is a Latin-1 String, which is what routes the
        // expression through the fused path rather than the two-object fallback.
        for (int round = 0; round < 200; round++) {
            String p = "p" + round;
            String q = "q" + (round * 7);
            String r = "r" + (round * 13);
            String s = "s" + (round * 17);
            for (int i = 0; i < 2000; i++) {
                String two = p + q;
                String three = p + q + r;
                String four = p + q + r + s;
                String five = p + q + r + s + two;
                ck += two.length() + three.length() + four.length() + five.length();
                keep[i & 63] = five;
                if ((i & 255) == 0) {
                    ck += keep[i & 63].charAt(0);
                }
            }
            bad += verify(round);
            if (bad > 0) {
                break;
            }
        }
        System.out.println("checksum=" + ck);
        System.out.println("corrupt=" + bad);
    }
}
