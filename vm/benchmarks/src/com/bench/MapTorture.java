package com.bench;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Semantic torture for the HashMap/LinkedHashMap implementations, comparable
 * byte-for-byte against HotSpot: every aggregation over plain HashMap content
 * is ORDER-INDEPENDENT (iteration order is unspecified and differs between
 * implementations); LinkedHashMap aggregations are ORDERED (its order is
 * specified: insertion order, or access order when configured).
 * Covers: put/get/remove/containsKey/containsValue/clear/size, null key and
 * null values, growth across several resizes, tombstone churn (interleaved
 * remove/put), putAll, entrySet iteration + setValue write-through,
 * keySet/values views + removal through views, Map.hashCode/equals,
 * insertion-order and access-order LinkedHashMap, and a PRNG op mix.
 */
public class MapTorture {
    static int seed = 0x1234567;
    static int rnd(int bound) {
        seed = seed * 1103515245 + 12345;
        return (seed >>> 16) % bound;
    }

    /** order-independent digest: separate + and ^ accumulators (each commutes). */
    static long digest(Map<Integer, Integer> m) {
        long sum = 0, xor = 0;
        for (Map.Entry<Integer, Integer> e : m.entrySet()) {
            long k = e.getKey() == null ? -7 : e.getKey().intValue();
            long v = e.getValue() == null ? -13 : e.getValue().intValue();
            long mix = (k * 0x9E3779B97F4A7C15L) ^ (v * 0xC2B2AE3D27D4EB4FL) ^ (v + 0x165667B19E3779F9L);
            sum += mix;
            xor ^= mix * 31 + 17;
        }
        return sum * 31 + xor + m.size();
    }

    /** ordered digest (LinkedHashMap only) */
    static long orderedDigest(Map<Integer, Integer> m) {
        long d = 1;
        for (Map.Entry<Integer, Integer> e : m.entrySet()) {
            long k = e.getKey() == null ? -7 : e.getKey().intValue();
            long v = e.getValue() == null ? -13 : e.getValue().intValue();
            d = d * 31 + k;
            d = d * 31 + v;
        }
        return d;
    }

    public static void main(String[] args) {
        long ck = 0;

        // 1: growth + replace + get across resizes
        HashMap<Integer, Integer> m = new HashMap<Integer, Integer>();
        for (int i = 0; i < 40000; i++) {
            ck += m.put(i & 8191, i) == null ? 1 : 2;
        }
        for (int i = 0; i < 8192; i++) {
            Integer v = m.get(i);
            ck += v == null ? 0 : v.intValue();
        }
        ck = ck * 31 + digest(m) + m.hashCode();

        System.out.println("CK1 " + ck);
        // 2: null key + null values
        m.put(null, 424242);
        m.put(77, null);
        ck += m.get(null) + (m.get(77) == null ? 5 : 6) + (m.containsKey(null) ? 7 : 8)
                + (m.containsKey(77) ? 9 : 10) + (m.containsValue(null) ? 11 : 12)
                + (m.containsValue(424242) ? 13 : 14);
        ck += m.remove(null);
        ck += m.containsKey(null) ? 15 : 16;
        ck = ck * 31 + digest(m);

        System.out.println("CK2 " + ck);
        // 3: tombstone churn -- interleaved remove/put over the same band
        for (int round = 0; round < 6; round++) {
            for (int i = 0; i < 4096; i++) {
                if (((i + round) & 3) == 0) {
                    Integer r = m.remove(i);
                    ck += r == null ? 1 : r.intValue() & 0xFF;
                } else {
                    m.put(i, i * round);
                }
            }
        }
        ck = ck * 31 + digest(m);

        System.out.println("CK3 " + ck);
        // 4: clear + refill
        m.clear();
        ck += m.size() + (m.isEmpty() ? 21 : 22) + (m.get(5) == null ? 23 : 24);
        for (int i = 0; i < 300; i++) m.put(i, i * i);
        ck = ck * 31 + digest(m);

        System.out.println("CK4 " + ck);
        // 5: putAll + equals/hashCode
        HashMap<Integer, Integer> m2 = new HashMap<Integer, Integer>(4);
        m2.putAll(m);
        ck += (m2.equals(m) ? 31 : 32) + (m2.hashCode() == m.hashCode() ? 33 : 34);
        m2.put(9999, 1);
        ck += m2.equals(m) ? 35 : 36;

        System.out.println("CK5 " + ck);
        // 6: entrySet setValue write-through + view removal
        for (Map.Entry<Integer, Integer> e : m.entrySet()) {
            if ((e.getKey().intValue() & 7) == 3) {
                e.setValue(e.getValue() + 1000);
            }
        }
        ck = ck * 31 + digest(m);
        Iterator<Integer> ki = m.keySet().iterator();
        int removedByView = 0;
        while (ki.hasNext()) {
            int k = ki.next();
            if ((k & 7) == 5) { ki.remove(); removedByView++; }
        }
        ck += removedByView * 41 + m.size();
        ck += (m.keySet().contains(3) ? 43 : 44) + (m.values().contains(4) ? 45 : 46);
        ck = ck * 31 + digest(m);

        System.out.println("CK6 " + ck);
        // 7: PRNG op mix
        HashMap<Integer, Integer> pm = new HashMap<Integer, Integer>();
        for (int i = 0; i < 200000; i++) {
            int op = rnd(10);
            int k = rnd(3000);
            if (op < 5) {
                Integer old = pm.put(k, i);
                ck += old == null ? 1 : (old.intValue() & 3);
            } else if (op < 7) {
                Integer v = pm.get(k);
                ck += v == null ? 0 : (v.intValue() & 7);
            } else if (op < 9) {
                Integer r = pm.remove(k);
                ck += r == null ? 2 : 3;
            } else if (op == 9 && (i & 8191) == 0) {
                pm.clear();
                ck += 47;
            } else {
                ck += pm.containsKey(k) ? 48 : 49;
            }
        }
        ck = ck * 31 + digest(pm);

        System.out.println("CK7 " + ck);
        // 8: LinkedHashMap insertion order (ORDERED digests are comparable)
        LinkedHashMap<Integer, Integer> lm = new LinkedHashMap<Integer, Integer>();
        for (int i = 0; i < 5000; i++) lm.put((i * 37) & 1023, i);
        ck = ck * 31 + orderedDigest(lm);
        lm.remove(37); lm.remove(74); lm.put(37, -1);   // re-insert goes to the END
        ck = ck * 31 + orderedDigest(lm);
        for (int i = 0; i < 512; i++) lm.put(2000 + i, i);   // force resize, keep order
        ck = ck * 31 + orderedDigest(lm);
        lm.clear();
        ck += lm.size() + (lm.isEmpty() ? 51 : 52);
        for (int i = 0; i < 40; i++) lm.put(i, i);
        ck = ck * 31 + orderedDigest(lm);

        System.out.println("CK8 " + ck);
        // 9: LinkedHashMap access order
        LinkedHashMap<Integer, Integer> am = new LinkedHashMap<Integer, Integer>(16, 0.75f, true);
        for (int i = 0; i < 100; i++) am.put(i, i);
        for (int i = 0; i < 100; i += 7) am.get(i);      // touched keys move to the end
        am.put(3, 33);                                    // access-order put also moves
        ck = ck * 31 + orderedDigest(am);

        System.out.println("CK9 " + ck);
        // 10: keys with colliding hashCodes (worst-case probing)
        HashMap<Integer, Integer> cm = new HashMap<Integer, Integer>();
        for (int i = 0; i < 64; i++) cm.put(i << 16, i);  // low bits identical
        for (int i = 0; i < 64; i++) ck += cm.get(i << 16);
        for (int i = 0; i < 64; i += 2) cm.remove(i << 16);
        for (int i = 0; i < 64; i++) {
            Integer v = cm.get(i << 16);
            ck += v == null ? 1 : v.intValue();
        }
        ck = ck * 31 + digest(cm);

        System.out.println("CK " + ck);
        System.out.println("DONE");
    }
}
