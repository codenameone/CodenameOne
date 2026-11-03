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
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
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
// Differential torture: long arithmetic at every representation boundary
// (2^31, 2^32, 2^53, 2^63), loop accumulators that cross them, int wrapping
// through IINC/IADD/narrowing, StringBuilder with latin1 and wide chars past
// the 4096-char chunk, and identity hashes. The test holds the hash to what a
// JVM computes. To find the line that differs, print each line in p() and diff
// the two runs.
public class JsArithmeticTortureApp {
    static long lf = Long.MAX_VALUE - 5;
    static int lines;
    static long[] la = new long[8];
    long instLong = 1L << 52;

    static int result;
    static int hash = 0x811c9dc5;

    // FNV-1a over every "name=value" line, the same bytes a JVM folds.
    static void p(String k, long v) { p(k, String.valueOf(v)); }
    static void p(String k, String v) {
        String line = k + "=" + v + "\n";
        for (int i = 0; i < line.length(); i++) {
            hash = (hash ^ line.charAt(i)) * 0x01000193;
        }
        lines++;
    }

    static long id(long x) { return x; }
    static int idi(int x) { return x; }

    public static void main(String[] args) {
        long[] edges = {0, 1, -1, 2, -2, 7, -7, 0x7fffffffL, 0x80000000L, -0x80000000L, 0xffffffffL, 0x100000000L,
            (1L << 52) - 1, 1L << 52, (1L << 53) - 1, 1L << 53, (1L << 53) + 1, -(1L << 53), -(1L << 53) - 1,
            1L << 62, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE - 1, Long.MIN_VALUE + 1, 123456789012345L,
            -987654321098765L, 0x5DEECE66DL, 6364136223846793005L};
        int[] shifts = {0, 1, 31, 32, 33, 52, 53, 63, 64, 65, -1};
        for (int i = 0; i < edges.length; i++) {
            long a = id(edges[i]);
            p("l2i" + i, (int) a);
            p("l2d" + i, Double.doubleToLongBits((double) a));
            p("hash" + i, Long.valueOf(a).hashCode());
            p("hex" + i, Long.toHexString(a));
            p("neg" + i, -a);
            for (int s : shifts) {
                p("shl" + i + "_" + s, a << s);
                p("shr" + i + "_" + s, a >> s);
                p("ushr" + i + "_" + s, a >>> s);
            }
            for (int j = 0; j < edges.length; j++) {
                long b = id(edges[j]);
                String k = i + "_" + j;
                p("add" + k, a + b);
                p("sub" + k, a - b);
                p("mul" + k, a * b);
                p("and" + k, a & b);
                p("or" + k, a | b);
                p("xor" + k, a ^ b);
                p("cmp" + k, Long.compare(a, b) + (a < b ? 10 : 0) + (a == b ? 100 : 0) + (a >= b ? 1000 : 0));
                if (b != 0) {
                    p("div" + k, a / b);
                    p("rem" + k, a % b);
                }
            }
        }
        double[] ds = {0.5, -0.5, 1e18, -1e18, 9.3e18, -9.3e18, 1e300, Double.NaN, 9007199254740993.0, 4.5035996273704955E15};
        for (int i = 0; i < ds.length; i++) { p("d2l" + i, (long) ds[i]); p("d2i" + i, (int) ds[i]); }

        // accumulators crossing 2^53 and 2^63 inside loops
        long acc = (1L << 53) - 50;
        for (int i = 0; i < 100; i++) { acc += i; }
        p("accA", acc);
        acc = Long.MAX_VALUE - 1000;
        for (int i = 0; i < 2000; i++) { acc += 3; }
        p("accB", acc);
        acc = 0;
        for (int i = 0; i < 100000; i++) { acc += (long) i * i * i; }
        p("accC", acc);
        acc = 1;
        for (int i = 0; i < 70; i++) { acc *= 3; acc -= i; }
        p("accD", acc);
        acc = -(1L << 53) + 10;
        for (int i = 0; i < 30; i++) { acc -= i; }
        p("accE", acc);
        long x = 0x5DEECE66DL;
        for (int i = 0; i < 1000; i++) { x = (x * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1); }
        p("lcg", x);
        long h = 1125899906842597L;
        String hs = "the quick brown fox";
        for (int i = 0; i < hs.length(); i++) { h = 31 * h + hs.charAt(i); }
        p("strhash64", h);
        for (int i = 0; i < 8; i++) { la[i] = (1L << 50) * i; }
        long sum = 0;
        for (int r = 0; r < 20; r++) { for (int i = 0; i < 8; i++) { sum += la[i]; } }
        p("arrsum", sum);
        for (int i = 0; i < 20; i++) { lf += 1; }
        p("fieldwrap", lf);
        JsArithmeticTortureApp t = new JsArithmeticTortureApp();
        for (int i = 0; i < 5000; i++) { t.instLong += i * 1000003L; }
        p("inst", t.instLong);
        long mixed = 0;
        for (int i = 0; i < 1000; i++) { mixed += i; if ((i & 7) == 0) { mixed = mixed * 1000003L; } }
        p("mixed", mixed);
        long cnt = 0;
        for (long q = Long.MAX_VALUE - 10; q > 0 && cnt < 20; q++) { cnt++; }
        p("loopwrap", cnt);

        // int wrap
        int ii = Integer.MAX_VALUE - 5;
        for (int k = 0; k < 10; k++) { ii++; }
        p("iinc", ii);
        ii = 0;
        for (int k = 0; k < 5000; k++) { ii += 1000003; }
        p("iinc2", ii);
        int[] ia = new int[16];
        for (int k = 0; k < 16; k++) { ia[k] = Integer.MAX_VALUE - 3 + k; }
        long isum = 0;
        for (int k = 0; k < 16; k++) { isum += ia[k]; isum += ia[k] + ia[k]; }
        p("iarr", isum);
        int a1 = idi(Integer.MIN_VALUE);
        p("mindiv", a1 / idi(-1));
        p("minrem", a1 % idi(-1));
        p("ineg", -a1);
        p("i2l", (long) (idi(Integer.MAX_VALUE) + 1));
        p("i2l2", (long) (idi(-5) - Integer.MAX_VALUE));
        p("i2b", (byte) (idi(200) + 100));
        p("i2c", (int) (char) (idi(-1) + 0));
        p("i2s", (short) (idi(40000) + 1));
        int ih = 0;
        for (int k = 0; k < 1000; k++) { ih = ih * 31 + k; }
        p("ihash", ih);
        int sumi = 0;
        for (int k = 0; k < 100000; k++) { sumi += k * 7; sumi -= k >> 1; }
        p("isum", sumi);
        int cmp = (idi(Integer.MAX_VALUE) + 1) < 0 ? 1 : 0;
        p("icmp", cmp);
        char c = 65535; c++;
        p("cinc", c);
        byte bb = 127; bb++;
        p("binc", bb);
        short sh = 32767; sh = (short) (sh + 1);
        p("sinc", sh);

        // strings
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 10000; k++) { sb.append((char) ('a' + k % 26)); if (k % 997 == 0) { sb.append(k); } }
        String big = sb.toString();
        p("biglen", big.length());
        p("bighash", big.hashCode());
        p("bigchar", big.charAt(5000));
        sb.append('\u20ac').append("x\u00e9y").append(new String(Character.toChars(0x1F600)));
        String uni = sb.toString();
        p("unilen", uni.length());
        p("unihash", uni.hashCode());
        p("unitail", uni.substring(uni.length() - 6));
        p("unicp", uni.codePointAt(uni.length() - 2));
        p("uniidx", uni.indexOf('\u20ac'));
        StringBuilder s2 = new StringBuilder("hello\u00ff");
        s2.insert(0, 42L).reverse();
        s2.setCharAt(1, '\u0100');
        s2.deleteCharAt(0);
        p("s2", s2.toString());
        p("s2h", s2.toString().hashCode());
        s2.setLength(3);
        p("s2len", s2.toString() + "|" + s2.length());
        char[] buf = new char[5];
        "abcdefgh".getChars(2, 7, buf, 0);
        p("getchars", new String(buf));
        p("tochars", new String("z\u00e9\u4e2d".toCharArray(), 1, 2).hashCode());
        String e1 = new StringBuilder().append("ab").append('c').toString();
        p("eq", (e1.equals("abc") ? 1 : 0) + (e1.hashCode() == "abc".hashCode() ? 2 : 0) + (e1.compareTo("abd") < 0 ? 4 : 0)
                + ("abc".equals(e1) ? 8 : 0));
        p("lat1", new String(new char[]{'\u00ff', 'a'}).toUpperCase().length());
        StringBuilder s3 = new StringBuilder();
        s3.append(Long.MIN_VALUE).append(' ').append("").append(' ').append(15).append(' ').append(true).append((Object) null);
        p("s3", s3.toString());
        String concat = "";
        for (int k = 0; k < 50; k++) { concat += (char) (0x3b1 + k % 3); }
        p("concat", concat.hashCode());
        StringBuilder s4 = new StringBuilder();
        for (int k = 0; k < 9000; k++) { s4.append(k % 2 == 0 ? 'q' : '\u0434'); }
        String s4s = s4.toString();
        p("s4", s4s.hashCode() + ":" + s4s.charAt(8999) + ":" + s4s.lastIndexOf('q'));

        // StringBuilder edits that change count without touching characters
        StringBuilder m1 = new StringBuilder("abcdef");
        m1.setLength(3);
        m1.append("XY");
        p("sbShrinkAppend", m1.toString() + "|" + m1.length() + "|" + m1.charAt(4) + "|" + m1.capacity());
        m1.setLength(0);
        m1.append(12).append('z');
        p("sbReuse", m1.toString() + "|" + m1.capacity());
        StringBuilder m2 = new StringBuilder();
        m2.append("x\u0101yz");
        m2.insert(1, "Q");
        p("sbInsertWide", m2.toString() + "|" + (int) m2.charAt(2));
        StringBuilder m3 = new StringBuilder("hello world");
        m3.delete(2, 5).append('!');
        m3.setLength(m3.length() + 2);
        p("sbDeleteGrow", m3.toString().replace('\u0000', '_') + "|" + m3.length());
        StringBuilder m4 = new StringBuilder();
        for (int k = 0; k < 40; k++) { m4.append(k); p("sbCap" + k, m4.capacity()); }
        char[] got = new char[4];
        m4.getChars(3, 7, got, 0);
        p("sbGetChars", new String(got) + "|" + m4.indexOf("39") + "|" + m4.lastIndexOf("1"));
        m4.setCharAt(0, '\u00e9');
        p("sbAfterSet", m4.substring(0, 5) + "|" + m4.toString().hashCode());
        m4.append("tail").reverse();
        p("sbReverse", m4.toString().hashCode());
        StringBuilder m5 = new StringBuilder("ab");
        m5.append(new char[]{'c', '\u0434', 'e'}, 0, 3).append(new char[]{'f'});
        m5.trimToSize();
        p("sbChars", m5.toString() + "|" + m5.capacity());
        StringBuffer bf = new StringBuffer();
        bf.append(1).append("two").append('3').insert(0, 'x').reverse();
        p("sbuf", bf.toString());


        // fused StringBuilders (JavascriptBuilderFusion): every shape it accepts,
        // and the ones it must leave alone, against the JVM
        String nul = null;
        Object onul = null;
        p("fNull", "a" + nul + 'b' + onul);
        p("fBool", "t" + true + false + (idi(3) > 2) + (idi(1) > 2));
        p("fLong", "L" + Long.MIN_VALUE + "|" + Long.MAX_VALUE + "|" + (1L << 53) + "|" + ((1L << 53) + 1) + "|" + (-(1L << 60) - 7));
        p("fInt", "I" + Integer.MIN_VALUE + "|" + Integer.MAX_VALUE + "|" + (idi(Integer.MAX_VALUE) + 1) + "|" + (short) idi(40000) + "|" + (byte) idi(200));
        p("fChar", "C" + '\u0000' + '\u00e9' + '\u4e2d' + '\ud83d' + '\ude00' + (char) idi(65535) + (char) idi(65));
        p("fDbl", "D" + 1.5 + "|" + 0.1f + "|" + (-0.0) + "|" + Double.NaN + "|" + 1e21 + "|" + 1e-7);
        p("fObj", "O" + Integer.valueOf(7) + new StringBuilder("x").append(1) + Long.valueOf(-3));
        for (int k = 0; k < 6; k++) {
            p("fTern" + k, "t" + (k % 3 == 0 ? "zero" : k % 3 == 1 ? "one" : nul) + "/" + (k > 2 ? k : -k) + (k == 4 ? '!' : '?'));
        }
        p("fLoop", fusedLoop(7));
        p("fAlias", fusedAlias(true) + "|" + fusedAlias(false));
        p("fMixed", fusedMixed(true) + "|" + fusedMixed(false));
        p("fLen", fusedLength(5));
        p("fInitNull", fusedInitNull());
        p("fCtor", new StringBuilder("seed\u00ff").append('+').append(12L).toString());
        p("fEsc", fusedEscapes());
        p("fArg", fusedAsArgument());
        p("fCatch", fusedCatchEscape(0) + "|" + fusedCatchEscape(1));
        p("divZero", divisionByZero(0) + "|" + divisionByZero(1));
        p("foldNull", foldedGetterOnNull(null) + "|" + foldedGetterOnNull(new Held(5)));
        p("fHash", ("h" + idi(12345) + ":" + idi(-1)).hashCode());

        // identity
        Object o = new Object();
        int h1 = o.hashCode();
        int h2 = o.hashCode();
        p("idstable", (h1 == h2 ? 1 : 0) + (System.identityHashCode(o) == h1 ? 2 : 0)
                + (o.toString().equals("java.lang.Object@" + Integer.toHexString(h1)) ? 4 : 0));
        int distinct = 0;
        java.util.HashSet<Integer> set = new java.util.HashSet<Integer>();
        for (int k = 0; k < 1000; k++) { set.add(new Object().hashCode()); }
        p("iddistinct", set.size() > 990 ? 1 : 0);
        java.util.HashMap<Object, Integer> m = new java.util.HashMap<Object, Integer>();
        Object[] keys = new Object[200];
        for (int k = 0; k < 200; k++) { keys[k] = new Object(); m.put(keys[k], k); }
        int ok = 0;
        for (int k = 0; k < 200; k++) { if (m.get(keys[k]) == k) { ok++; } }
        p("idmap", ok);
        int[] arr = new int[3];
        p("arrid", arr.hashCode() == arr.hashCode() ? 1 : 0);
        result = hash ^ lines;
    }

    static String fusedLoop(int n) {
        String acc = "";
        StringBuilder outer = new StringBuilder();
        for (int i = 0; i < n; i++) {
            StringBuilder b = new StringBuilder();
            b.append(i).append(',');
            if ((i & 1) == 0) {
                b.append("even");
            } else {
                b.append(i * 1000000007L);
            }
            acc = acc + b.toString() + ";";
            outer.append(b.length()).append(' ');
        }
        return acc + "#" + outer;
    }

    static String fusedAlias(boolean c) {
        StringBuilder b = new StringBuilder("q");
        StringBuilder alias = b;
        alias.append(1);
        StringBuilder chained = b.append(c ? "yes" : "no").append('.');
        chained.append(b.length());
        return b.toString() + "/" + alias.toString() + "/" + chained.toString();
    }

    static StringBuilder shared = new StringBuilder("shared");

    static String fusedMixed(boolean c) {
        // Might be the new builder, might be a field's: must not be fused.
        StringBuilder b = c ? new StringBuilder("fresh") : shared;
        b.append('*');
        return b.toString();
    }

    static String fusedLength(int n) {
        StringBuilder b = new StringBuilder();
        int total = 0;
        for (int i = 0; i < n; i++) {
            b.append("\u00e9x");
            total += b.length();
        }
        return b.length() + ":" + total;
    }

    static String fusedInitNull() {
        String nothing = null;
        try {
            return new StringBuilder(nothing).append("unreached").toString();
        } catch (NullPointerException expected) {
            return "npe";
        }
    }

    static String fusedEscapes() {
        return "a\"b\\c" + '\n' + "\t" + idi(1) + "</script>" + '\'' + "${x}" + '`';
    }

    static int sink;

    static String fusedAsArgument() {
        // Escapes as an argument: must not be fused.
        StringBuilder b = new StringBuilder("arg");
        consume(b);
        return b.append('!').toString();
    }

    static String divisionByZero(int z) {
        // Every integer division and remainder by zero throws ArithmeticException.
        String r = "";
        long lz = z;
        try { r += 7 / z; } catch (ArithmeticException e) { r += "i/"; }
        try { r += 7 % z; } catch (ArithmeticException e) { r += "i%"; }
        try { r += 7L / lz; } catch (ArithmeticException e) { r += "l/"; }
        try { r += 7L % lz; } catch (ArithmeticException e) { r += "l%"; }
        return r;
    }

    static final class Held {
        private int v;
        Held(int v) { this.v = v; }
        int getV() { return v; }
        void setV(int v) { this.v = v; }
    }

    static String foldedGetterOnNull(Held h) {
        // The trivial getter is folded into a field read; on null it must still
        // throw NullPointerException, not a host error no Java handler catches.
        try {
            return "v" + h.getV();
        } catch (NullPointerException e) {
            return "npe" + foldedSetterOnNull(h);
        }
    }

    static String foldedSetterOnNull(Held h) {
        try {
            h.setV(1);
            return "set";
        } catch (NullPointerException e) {
            return "Snpe";
        }
    }

    static String fusedCatchEscape(int fail) {
        // Live into a handler that hands it on: the handler's use must reject it.
        StringBuilder b = new StringBuilder("c");
        try {
            b.append(10 / (1 - fail));
        } catch (ArithmeticException e) {
            consume(b);
        }
        return b.toString();
    }

    static void consume(StringBuilder b) {
        b.append(b.length());
        sink += b.length();
    }

}
