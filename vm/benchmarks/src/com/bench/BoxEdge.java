/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The correctness gate for tagged immediates ("poor man's Valhalla"): Integer, Long, Double,
 * Float, Character and Short are returned by valueOf as an immediate whose low three bits
 * are a type tag, with no object header at all. Every operation that would normally read
 * that header has to be tag-aware, and a mistake there is silent -- the wrong class, or the
 * wrong hash, with nothing thrown.
 *
 * Output must be BYTE-IDENTICAL to a host JVM, so every assertion is printed rather than
 * asserted locally. Two rules keep it comparable:
 *   - never print an identity hash code, a raw reference, or the iteration order of a
 *     HashMap/HashSet: those legitimately differ between VMs;
 *   - hashCode() of a boxed VALUE is specified by the JDK and IS printed, because matching
 *     it is exactly what this scheme has to get right.
 *
 * The cases deliberately include the ones an ordinary benchmark never reaches: heap boxes
 * made with `new` alongside tagged ones, cross-type comparisons, NaN and both zeroes, and
 * Long/Double values chosen to fall OUTSIDE the taggable range so the heap fallback and the
 * mixed tagged/heap comparison paths are exercised rather than assumed.
 */
public final class BoxEdge {

    // Printed as it goes rather than buffered: a native crash on this target is a hard
    // SIGSEGV with no stack, so the last line emitted is the only locator there is.
    static void p(String label, Object v) {
        System.out.println(label + "=" + v);
    }

    // Values that cannot be represented in the 61-bit payload, so valueOf must fall back to
    // the heap. If the taggable range ever widens these stop testing the fallback, which is
    // why the probe below prints whether each one actually took it.
    static final long BIG_LONG = 4611686018427387905L;   // 2^62 + 1, outside [-2^60, 2^60)
    static final long BIG_LONG_NEG = -4611686018427387905L;
    static final double ODD_DOUBLE = 0.1;                // low mantissa bits set
    static final double ODD_DOUBLE2 = 3.14159265358979;

    public static void main(String[] args) throws Exception {
        classIdentity();
        equalsAndHash();
        crossType();
        numberConversions();
        comparisons();
        collections();
        floatingPointEdges();
        stringsAndSwitch();
        synchronization();
        arraysAndSorting();
        reportTagCodes();
        System.out.println("BOXEDGE DONE");
    }

    /**
     * Which tag code each boxed type actually got in THIS build, on stderr.
     *
     * It carries the `[` prefix this tree uses for diagnostics ([GCPROBE], [ALLOC:...]),
     * because that is what `run-gauntlet.sh` filters out of BOTH sides before comparing --
     * stdout has to stay byte-identical to a host JVM and this line is target-specific by
     * construction. Note stderr is NOT a way out: on the clean target System.err also
     * reaches fd 1, so a stderr diagnostic still lands in the compared stream.
     *
     * Its job is to stop the rest of this file being VACUOUS. Every assertion here passes
     * on a build where tagging never happened -- that is the point, the two representations
     * must be indistinguishable -- so without a witness saying which arm ran, a green
     * BoxEdge cannot tell "the tagged path is correct" from "the tagged path never
     * executed". Expect 123456 in a default build and 000000 with -DCN1_DISABLE_TAGGED_INT.
     */
    static void reportTagCodes() {
        StringBuilder sb = new StringBuilder();
        sb.append(System.identityHashCode(Integer.valueOf(1)) & 7);
        sb.append(System.identityHashCode(Long.valueOf(1L)) & 7);
        sb.append(System.identityHashCode(Double.valueOf(1.0)) & 7);
        sb.append(System.identityHashCode(Float.valueOf(1.0f)) & 7);
        sb.append(System.identityHashCode(Character.valueOf('a')) & 7);
        sb.append(System.identityHashCode(Short.valueOf((short) 1)) & 7);
        System.out.println("[TAGCODES] " + sb);
    }

    /* ---- getClass / instanceof / isInstance, on tagged and heap boxes alike ---- */
    static void classIdentity() {
        Object[] tagged = {
            Integer.valueOf(7), Long.valueOf(7L), Double.valueOf(1.5),
            Float.valueOf(1.5f), Character.valueOf('z'), Short.valueOf((short) 7)
        };
        Object[] heap = {
            new Integer(7), new Long(7L), new Double(1.5),
            new Float(1.5f), new Character('z'), new Short((short) 7)
        };
        Class[] expect = {
            Integer.class, Long.class, Double.class, Float.class, Character.class, Short.class
        };
        for (int i = 0; i < tagged.length; i++) {
            p("cls.tagged." + i, tagged[i].getClass().getName());
            p("cls.heap." + i, heap[i].getClass().getName());
            p("cls.same." + i, tagged[i].getClass() == heap[i].getClass());
            p("cls.expected." + i, tagged[i].getClass() == expect[i]);
            p("isInstance.tagged." + i, expect[i].isInstance(tagged[i]));
            p("isInstance.heap." + i, expect[i].isInstance(heap[i]));
            p("isInstance.null." + i, expect[i].isInstance(null));
            p("isInstance.wrong." + i, expect[i].isInstance("a string"));
            p("isNumber.tagged." + i, tagged[i] instanceof Number);
            p("isComparable.tagged." + i, tagged[i] instanceof Comparable);
            p("isString.tagged." + i, tagged[i] instanceof String);
        }
        // The fallback values must report the same class as the tagged ones.
        p("cls.bigLong", Long.valueOf(BIG_LONG).getClass().getName());
        p("cls.oddDouble", Double.valueOf(ODD_DOUBLE).getClass().getName());
        p("bigLong.isNumber", Long.valueOf(BIG_LONG) instanceof Number);
    }

    /* ---- equals and hashCode across tagged/heap/null/wrong-type ---- */
    static void equalsAndHash() {
        Object[][] pairs = {
            { Integer.valueOf(1234), new Integer(1234), Integer.valueOf(1235) },
            { Long.valueOf(1234L), new Long(1234L), Long.valueOf(1235L) },
            { Long.valueOf(BIG_LONG), new Long(BIG_LONG), Long.valueOf(BIG_LONG - 1) },
            { Double.valueOf(2.5), new Double(2.5), Double.valueOf(2.75) },
            { Double.valueOf(ODD_DOUBLE), new Double(ODD_DOUBLE), Double.valueOf(ODD_DOUBLE2) },
            { Float.valueOf(2.5f), new Float(2.5f), Float.valueOf(2.75f) },
            { Character.valueOf('m'), new Character('m'), Character.valueOf('n') },
            { Short.valueOf((short) 1234), new Short((short) 1234), Short.valueOf((short) 1235) }
        };
        for (int i = 0; i < pairs.length; i++) {
            Object t = pairs[i][0], h = pairs[i][1], other = pairs[i][2];
            p("eq.tt." + i, t.equals(t));
            p("eq.th." + i, t.equals(h));
            p("eq.ht." + i, h.equals(t));
            p("eq.hh." + i, h.equals(h));
            p("eq.to." + i, t.equals(other));
            p("eq.ot." + i, other.equals(t));
            p("eq.null." + i, t.equals(null));
            p("eq.str." + i, t.equals("nope"));
            p("eq.strRev." + i, "nope".equals(t));
            p("hash.t." + i, t.hashCode());
            p("hash.h." + i, h.hashCode());
            p("hash.same." + i, t.hashCode() == h.hashCode());
            p("hash.other." + i, other.hashCode());
        }
    }

    /* ---- a boxed 1 of one type must never equal a boxed 1 of another ---- */
    static void crossType() {
        Object[] ones = {
            Integer.valueOf(1), Long.valueOf(1L), Double.valueOf(1.0),
            Float.valueOf(1.0f), Character.valueOf((char) 1), Short.valueOf((short) 1)
        };
        String[] names = { "Integer", "Long", "Double", "Float", "Character", "Short" };
        for (int i = 0; i < ones.length; i++) {
            for (int j = 0; j < ones.length; j++) {
                p("cross." + names[i] + "." + names[j], ones[i].equals(ones[j]));
            }
        }
        // Same again through a map, which is where a wrong hash actually bites.
        Map m = new HashMap();
        for (int i = 0; i < ones.length; i++) {
            m.put(ones[i], names[i]);
        }
        p("cross.mapSize", m.size());
        for (int i = 0; i < ones.length; i++) {
            p("cross.get." + names[i], m.get(ones[i]));
        }
    }

    /* ---- every Number accessor, on tagged and on heap-fallback values ---- */
    static void numberConversions() {
        Number[] ns = {
            Integer.valueOf(-129), Long.valueOf(-129L), Long.valueOf(BIG_LONG),
            Long.valueOf(BIG_LONG_NEG), Double.valueOf(-2.75), Double.valueOf(ODD_DOUBLE),
            Float.valueOf(-2.75f), Short.valueOf((short) -129),
            Integer.valueOf(Integer.MAX_VALUE), Integer.valueOf(Integer.MIN_VALUE),
            Long.valueOf(Long.MAX_VALUE), Long.valueOf(Long.MIN_VALUE),
            Short.valueOf(Short.MAX_VALUE), Short.valueOf(Short.MIN_VALUE),
            Double.valueOf(Double.MAX_VALUE), Double.valueOf(Double.MIN_VALUE),
            Float.valueOf(Float.MAX_VALUE), Float.valueOf(Float.MIN_VALUE)
        };
        for (int i = 0; i < ns.length; i++) {
            p("num.byte." + i, ns[i].byteValue());
            p("num.short." + i, ns[i].shortValue());
            p("num.int." + i, ns[i].intValue());
            p("num.long." + i, ns[i].longValue());
            p("num.float." + i, ns[i].floatValue());
            p("num.double." + i, ns[i].doubleValue());
            p("num.str." + i, ns[i].toString());
            p("num.hash." + i, ns[i].hashCode());
        }
        Character c = Character.valueOf(Character.MAX_VALUE);
        p("char.max.value", (int) c.charValue());
        p("char.max.str", c.toString());
        p("char.max.hash", c.hashCode());
        Character cz = Character.valueOf(Character.MIN_VALUE);
        p("char.min.value", (int) cz.charValue());
        p("char.min.hash", cz.hashCode());
    }

    /* ---- compareTo, including tagged vs heap and across the taggable boundary ---- */
    static void comparisons() {
        p("cmp.int", Integer.valueOf(5).compareTo(Integer.valueOf(7)));
        p("cmp.int.heap", Integer.valueOf(5).compareTo(new Integer(7)));
        p("cmp.int.eq", Integer.valueOf(5).compareTo(new Integer(5)));
        p("cmp.long.small", Long.valueOf(5L).compareTo(Long.valueOf(7L)));
        p("cmp.long.mixed", Long.valueOf(5L).compareTo(Long.valueOf(BIG_LONG)));
        p("cmp.long.mixedRev", Long.valueOf(BIG_LONG).compareTo(Long.valueOf(5L)));
        p("cmp.long.bothBig", Long.valueOf(BIG_LONG).compareTo(Long.valueOf(BIG_LONG_NEG)));
        p("cmp.long.extremes", Long.valueOf(Long.MIN_VALUE).compareTo(Long.valueOf(Long.MAX_VALUE)));
        p("cmp.double", Double.valueOf(1.5).compareTo(Double.valueOf(2.5)));
        p("cmp.double.mixed", Double.valueOf(1.5).compareTo(Double.valueOf(ODD_DOUBLE)));
        p("cmp.double.nan", Double.valueOf(Double.NaN).compareTo(Double.valueOf(1.0)));
        p("cmp.double.zeroes", Double.valueOf(0.0).compareTo(Double.valueOf(-0.0)));
        p("cmp.float", Float.valueOf(1.5f).compareTo(Float.valueOf(2.5f)));
        p("cmp.float.nan", Float.valueOf(Float.NaN).compareTo(Float.valueOf(1.0f)));
        p("cmp.char", Character.valueOf('a').compareTo(Character.valueOf('b')));
        p("cmp.char.max", Character.valueOf(Character.MAX_VALUE).compareTo(Character.valueOf('a')));
        p("cmp.short", Short.valueOf((short) -5).compareTo(Short.valueOf((short) 7)));
        p("cmp.short.extremes", Short.valueOf(Short.MIN_VALUE).compareTo(Short.valueOf(Short.MAX_VALUE)));
    }

    /* ---- the collections, which are where a tagged key is actually used ---- */
    static void collections() {
        // TreeMap: ordered, so iteration order is deterministic on both VMs. It also routes
        // every probe through Comparable.compareTo, i.e. INTERFACE dispatch on an immediate.
        TreeMap tm = new TreeMap();
        long[] keys = { 0L, 1L, -1L, 127L, 128L, -129L, BIG_LONG, BIG_LONG_NEG,
                        Long.MAX_VALUE, Long.MIN_VALUE, 1152921504606846975L };
        for (int i = 0; i < keys.length; i++) {
            tm.put(Long.valueOf(keys[i]), "v" + keys[i]);
        }
        p("tree.size", tm.size());
        StringBuilder order = new StringBuilder();
        for (Iterator it = tm.keySet().iterator(); it.hasNext(); ) {
            order.append(it.next()).append(',');
        }
        p("tree.order", order.toString());
        p("tree.firstKey", tm.firstKey());
        p("tree.lastKey", tm.lastKey());
        for (int i = 0; i < keys.length; i++) {
            p("tree.get." + i, tm.get(Long.valueOf(keys[i])));
            p("tree.getHeap." + i, tm.get(new Long(keys[i])));
        }
        p("tree.missing", tm.get(Long.valueOf(999999L)));

        // HashMap: size and lookups only, never iteration order.
        Map hm = new HashMap();
        for (int i = -300; i < 300; i++) {
            hm.put(Integer.valueOf(i), Integer.valueOf(i * 3));
            hm.put(Long.valueOf(i), Long.valueOf(i * 5L));
            hm.put(Short.valueOf((short) i), Short.valueOf((short) (i * 2)));
            hm.put(Character.valueOf((char) (i + 400)), Character.valueOf((char) (i + 401)));
            hm.put(Double.valueOf(i / 4.0), Double.valueOf(i / 8.0));
            hm.put(Float.valueOf(i / 4.0f), Float.valueOf(i / 8.0f));
        }
        p("hash.size", hm.size());
        long acc = 0;
        for (int i = -300; i < 300; i++) {
            acc += ((Integer) hm.get(Integer.valueOf(i))).intValue();
            acc += ((Long) hm.get(new Long(i))).longValue();          // heap key, tagged entry
            acc += ((Short) hm.get(Short.valueOf((short) i))).shortValue();
            acc += (int) ((Character) hm.get(Character.valueOf((char) (i + 400)))).charValue();
            acc += (long) ((Double) hm.get(Double.valueOf(i / 4.0))).doubleValue();
            acc += (long) ((Float) hm.get(Float.valueOf(i / 4.0f))).floatValue();
        }
        p("hash.acc", acc);
        p("hash.missInt", hm.get(Integer.valueOf(100000)));
        p("hash.missLong", hm.get(Long.valueOf(BIG_LONG)));
        p("hash.containsTagged", hm.containsKey(Integer.valueOf(5)));
        p("hash.containsHeap", hm.containsKey(new Integer(5)));
        p("hash.removed", hm.remove(new Integer(5)));
        p("hash.containsAfterRemove", hm.containsKey(Integer.valueOf(5)));

        // HashSet dedup: a tagged box and its heap twin are ONE element.
        HashSet hs = new HashSet();
        hs.add(Integer.valueOf(42));
        hs.add(new Integer(42));
        hs.add(Long.valueOf(42L));
        hs.add(new Long(42L));
        hs.add(Double.valueOf(42.0));
        hs.add(Short.valueOf((short) 42));
        hs.add(Character.valueOf((char) 42));
        hs.add(Float.valueOf(42.0f));
        p("set.size", hs.size());
        p("set.hasInt", hs.contains(new Integer(42)));
        p("set.hasLong", hs.contains(Long.valueOf(42L)));

        // ArrayList: indexOf/contains/remove all go through equals on an immediate.
        List list = new ArrayList();
        for (int i = 0; i < 50; i++) {
            list.add(Integer.valueOf(i));
            list.add(Long.valueOf(i));
        }
        p("list.size", list.size());
        p("list.indexOfTagged", list.indexOf(Integer.valueOf(30)));
        p("list.indexOfHeap", list.indexOf(new Integer(30)));
        p("list.indexOfLong", list.indexOf(Long.valueOf(30L)));
        p("list.containsMissing", list.contains(Integer.valueOf(1000)));
        p("list.removeObj", list.remove(Integer.valueOf(30)));
        p("list.sizeAfter", list.size());
    }

    /* ---- NaN, both zeroes, infinities: where bit patterns and equals diverge ---- */
    static void floatingPointEdges() {
        Double nan1 = Double.valueOf(Double.NaN);
        Double nan2 = Double.valueOf(0.0 / 0.0);
        Double pz = Double.valueOf(0.0), nz = Double.valueOf(-0.0);
        p("d.nanEquals", nan1.equals(nan2));
        p("d.nanEqualsSelf", nan1.equals(nan1));
        p("d.nanPrimitiveEq", nan1.doubleValue() == nan2.doubleValue());
        p("d.nanIsNaN", nan1.isNaN());
        p("d.nanHash", nan1.hashCode() == nan2.hashCode());
        p("d.zeroEquals", pz.equals(nz));
        p("d.zeroPrimitiveEq", pz.doubleValue() == nz.doubleValue());
        p("d.zeroHashDiffers", pz.hashCode() != nz.hashCode());
        p("d.posInf", Double.valueOf(Double.POSITIVE_INFINITY).toString());
        p("d.negInf", Double.valueOf(Double.NEGATIVE_INFINITY).toString());
        p("d.infIsInfinite", Double.valueOf(Double.POSITIVE_INFINITY).isInfinite());
        p("d.infEquals", Double.valueOf(Double.POSITIVE_INFINITY)
                .equals(Double.valueOf(Double.POSITIVE_INFINITY)));
        p("d.infCross", Double.valueOf(Double.POSITIVE_INFINITY)
                .equals(Double.valueOf(Double.NEGATIVE_INFINITY)));
        p("d.bitsNaN", Double.doubleToLongBits(nan1.doubleValue()));
        p("d.bitsNegZero", Double.doubleToLongBits(nz.doubleValue()));

        Float fnan = Float.valueOf(Float.NaN);
        Float fpz = Float.valueOf(0.0f), fnz = Float.valueOf(-0.0f);
        p("f.nanEquals", fnan.equals(Float.valueOf(Float.NaN)));
        p("f.nanIsNaN", fnan.isNaN());
        p("f.zeroEquals", fpz.equals(fnz));
        p("f.zeroPrimitiveEq", fpz.floatValue() == fnz.floatValue());
        p("f.zeroHashDiffers", fpz.hashCode() != fnz.hashCode());
        p("f.bitsNaN", Float.floatToIntBits(fnan.floatValue()));
        p("f.bitsNegZero", Float.floatToIntBits(fnz.floatValue()));
        p("f.posInf", Float.valueOf(Float.POSITIVE_INFINITY).toString());
        p("f.negInf", Float.valueOf(Float.NEGATIVE_INFINITY).toString());
        p("f.nanStr", fnan.toString());
        p("f.nanStrStatic", Float.toString(Float.NaN));
        p("f.negZeroStr", fnz.toString());
        p("f.posZeroStr", fpz.toString());
        p("f.negZeroStrHeap", new Float(-0.0f).toString());
        p("d.nanStr", nan1.toString());
        p("d.negZeroStr", nz.toString());
        p("d.posZeroStr", pz.toString());

        // A NaN key must be findable in a map even though NaN != NaN.
        Map m = new HashMap();
        m.put(nan1, "nan");
        m.put(pz, "pz");
        m.put(nz, "nz");
        p("fp.mapSize", m.size());
        p("fp.getNaN", m.get(nan2));
        p("fp.getPz", m.get(Double.valueOf(0.0)));
        p("fp.getNz", m.get(Double.valueOf(-0.0)));
    }

    /* ---- toString, concat and switch on a boxed value ---- */
    static void stringsAndSwitch() {
        Object[] vs = {
            Integer.valueOf(-42), Long.valueOf(-42L), Long.valueOf(BIG_LONG),
            Double.valueOf(-42.5), Double.valueOf(ODD_DOUBLE), Float.valueOf(-42.5f),
            Character.valueOf('Q'), Short.valueOf((short) -42)
        };
        for (int i = 0; i < vs.length; i++) {
            p("str.toString." + i, vs[i].toString());
            p("str.concat." + i, "v=" + vs[i] + "!");
            p("str.valueOf." + i, String.valueOf(vs[i]));
            p("str.sbAppend." + i, new StringBuilder().append(vs[i]).toString());
            p("str.equalsStr." + i, vs[i].toString().equals(String.valueOf(vs[i])));
        }
        // Unboxing into a switch: the tag has to survive the round trip through Object.
        Object boxed = Integer.valueOf(3);
        int unboxed = ((Integer) boxed).intValue();
        String r;
        switch (unboxed) {
            case 1: r = "one"; break;
            case 3: r = "three"; break;
            default: r = "other";
        }
        p("switch.int", r);
        Object cboxed = Character.valueOf('b');
        switch (((Character) cboxed).charValue()) {
            case 'a': r = "a"; break;
            case 'b': r = "b"; break;
            default: r = "other";
        }
        p("switch.char", r);
        // Autoboxing round trip through an Object-typed field.
        Object o = Integer.valueOf(77);
        p("autobox.round", ((Integer) o).intValue() + 1);
    }

    /* ---- monitors on immediates: the side table is keyed by the word itself ---- */
    static void synchronization() throws Exception {
        final Object[] locks = {
            Integer.valueOf(101), Long.valueOf(102L), Double.valueOf(103.0),
            Float.valueOf(104.0f), Character.valueOf('e'), Short.valueOf((short) 106)
        };
        final int[] counter = new int[1];
        final int threads = 4, iters = 20000;
        for (int l = 0; l < locks.length; l++) {
            final Object lock = locks[l];
            counter[0] = 0;
            Thread[] ts = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                ts[t] = new Thread(new Runnable() {
                    public void run() {
                        for (int i = 0; i < iters; i++) {
                            synchronized (lock) {
                                counter[0]++;
                            }
                        }
                    }
                });
            }
            for (int t = 0; t < threads; t++) ts[t].start();
            for (int t = 0; t < threads; t++) ts[t].join();
            p("sync.guarded." + l, counter[0] == threads * iters);
        }

        // wait/notify on an immediate must actually park and wake.
        final Object sig = Double.valueOf(2.0);
        final boolean[] woke = new boolean[1];
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                synchronized (sig) {
                    try {
                        sig.wait(10000);
                        woke[0] = true;
                    } catch (InterruptedException e) {
                        // reported as not woken
                    }
                }
            }
        });
        waiter.start();
        Thread.sleep(200);
        synchronized (sig) {
            sig.notifyAll();
        }
        waiter.join(10000);
        p("sync.woke", woke[0]);
    }

    /* ---- Arrays.sort over boxed values, i.e. interface dispatch on immediates ---- */
    static void arraysAndSorting() {
        Long[] ls = {
            Long.valueOf(5L), Long.valueOf(BIG_LONG), Long.valueOf(-1L),
            Long.valueOf(BIG_LONG_NEG), Long.valueOf(0L), new Long(3L),
            Long.valueOf(Long.MAX_VALUE), Long.valueOf(Long.MIN_VALUE)
        };
        Arrays.sort(ls);
        p("sort.long", Arrays.toString(ls));

        Integer[] is = new Integer[200];
        for (int i = 0; i < is.length; i++) {
            is[i] = Integer.valueOf((i * 37) % 200 - 100);
        }
        Arrays.sort(is);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < is.length; i++) sb.append(is[i]).append(',');
        p("sort.int", sb.toString());

        Double[] ds = {
            Double.valueOf(1.5), Double.valueOf(ODD_DOUBLE), Double.valueOf(-0.0),
            Double.valueOf(0.0), Double.valueOf(ODD_DOUBLE2), new Double(2.0),
            Double.valueOf(Double.MAX_VALUE), Double.valueOf(-1.0)
        };
        Arrays.sort(ds);
        p("sort.double", Arrays.toString(ds));

        Character[] cs = {
            Character.valueOf('z'), Character.valueOf('a'), Character.valueOf(Character.MAX_VALUE),
            new Character('m'), Character.valueOf(Character.MIN_VALUE)
        };
        Arrays.sort(cs);
        StringBuilder cb = new StringBuilder();
        for (int i = 0; i < cs.length; i++) cb.append((int) cs[i].charValue()).append(',');
        p("sort.char", cb.toString());

        Short[] ss = {
            Short.valueOf((short) 5), Short.valueOf(Short.MIN_VALUE),
            new Short((short) -1), Short.valueOf(Short.MAX_VALUE), Short.valueOf((short) 0)
        };
        Arrays.sort(ss);
        p("sort.short", Arrays.toString(ss));

        Float[] fs = {
            Float.valueOf(1.5f), Float.valueOf(-0.0f), Float.valueOf(0.0f),
            new Float(2.5f), Float.valueOf(Float.MAX_VALUE), Float.valueOf(-1.0f)
        };
        Arrays.sort(fs);
        p("sort.float", Arrays.toString(fs));
    }
}
