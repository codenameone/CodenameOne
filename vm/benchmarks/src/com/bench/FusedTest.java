package com.bench;

/**
 * Correctness for FUSED strings (String + value char[] in one block):
 *   1. substring/trim/valueOf produce fused strings -- verify content, length,
 *      charAt, hashCode, equals, compareTo, getChars, getBytes vs plain ctors.
 *   2. Donor death: derive strings from a parent, drop the parent, GC hard,
 *      then read the derivations (the old sharing ctor would leave them
 *      pointing into a freed donor; fused+copy semantics must not).
 *   3. String(String) copies (fused source dies, copy survives).
 *   4. Churn: hold every 100th of 200k fused substrings across GC cycles.
 *   5. Oversize fallback: > 512-byte total goes through the two-object path.
 *   6. Interning/equality between fused and non-fused representations.
 * Output must match HotSpot byte-for-byte.
 */
public class FusedTest {
    static final StringBuilder LOG = new StringBuilder();

    static String makeParent(int seed) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            b.append((char) ('a' + ((seed + i) % 26)));
        }
        return b.toString();
    }

    public static void main(String[] args) {
        long ck = 0;

        // 1: basic equivalences
        char[] src = "the quick brown fox jumps over the lazy dog".toCharArray();
        String fused = String.valueOf(src, 4, 11);           // "quick brown"
        String plain = new String(src, 4, 11);
        ck += fused.equals(plain) ? 1 : 1000;
        ck += fused.hashCode() == plain.hashCode() ? 2 : 2000;
        ck += fused.compareTo(plain) == 0 ? 3 : 3000;
        ck += fused.charAt(6);
        ck += fused.length() * 7;
        char[] out = new char[11];
        fused.getChars(0, 11, out, 0);
        for (char c : out) ck += c;
        byte[] bytes = fused.getBytes();
        for (byte b : bytes) ck += b;
        ck += fused.substring(6).equals("brown") ? 5 : 5000;
        ck += ("  padded  ".trim()).equals("padded") ? 6 : 6000;
        ck += fused.indexOf("brown") * 11;
        ck += fused.toUpperCase().hashCode();

        System.out.println("CK1 " + ck);
        // 2: donor death -- derivations must be self-contained
        String[] derived = new String[64];
        for (int i = 0; i < 64; i++) {
            String parent = makeParent(i);
            derived[i] = parent.substring(5, 25);
            parent = null;
        }
        churn();
        for (int i = 0; i < 64; i++) {
            ck = ck * 31 + derived[i].hashCode() + derived[i].charAt(3);
        }

        System.out.println("CK2 " + ck);
        // 3: String(String) from a fused source
        String copy;
        {
            String fusedSrc = makeParent(7).substring(1, 30);
            copy = new String(fusedSrc);
            fusedSrc = null;
        }
        churn();
        ck = ck * 31 + copy.hashCode() + copy.length();

        System.out.println("CK3 " + ck);
        // 4: fused churn with survivors across GC
        String[] keep = new String[2000];
        String base = makeParent(3);
        for (int i = 0; i < 200000; i++) {
            String s = base.substring(i % 20, (i % 20) + 15);
            if (i % 100 == 0) keep[i / 100] = s;
        }
        churn();
        for (int i = 0; i < 2000; i++) {
            ck = ck * 31 + keep[i].charAt(i % 15);
        }

        System.out.println("CK4 " + ck);
        // 5: oversize fallback (600 chars > any BiBOP class)
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 600; i++) big.append((char) ('A' + (i % 26)));
        String bigParent = big.toString();
        String bigSub = bigParent.substring(10, 590);   // 580 chars, oversize for fusion
        ck = ck * 31 + bigSub.hashCode() + bigSub.length() + bigSub.charAt(500);

        System.out.println("CK5 " + ck);
        // 6: mixed-representation equality
        String a = String.valueOf(new char[] {'h','e','l','l','o'});
        String b = "hello";
        String c = new StringBuilder("hel").append("lo").toString();
        ck += a.equals(b) && b.equals(c) && a.hashCode() == c.hashCode() ? 17 : 17000;

        // 7: general @Fused user class (Image-like, two fused children)
        ck = ck * 31 + userFused();

        System.out.println("CK " + ck);
        System.out.println("DONE");
    }

    /** Image-like user class: owns its pixel and flag buffers (fused). */
    @com.codename1.annotations.Fused
    static class Pixels {
        final int[] argb;
        final byte[] flags;
        final int n;
        Pixels(int n) {
            if (n < 0) throw new IllegalArgumentException();
            this.argb = new int[n];
            this.flags = new byte[16];
            this.n = n;
            for (int i = 0; i < n; i++) argb[i] = i * 0x9E3779B1;
            flags[0] = 1;
        }
        long sum() {
            long s = flags[0];
            for (int i = 0; i < n; i++) s += argb[i];
            return s;
        }
    }

    /** Image-like with a COMPUTED fused size: new int[w * h]. */
    @com.codename1.annotations.Fused
    static class Img {
        final int[] pix;
        final int w, h;
        Img(int w, int h) {
            if (w < 0 || h < 0) throw new IllegalArgumentException();
            this.pix = new int[w * h];
            this.w = w;
            this.h = h;
            for (int i = 0; i < pix.length; i++) pix[i] = (i * 31) ^ w ^ (h << 8);
        }
        long sum() {
            long s = 0;
            for (int i = 0; i < pix.length; i++) s += pix[i];
            return s;
        }
    }

    static long userFused() {
        long ck = 0;
        // param-sized + const-sized children, survivors across GC
        Pixels[] keep = new Pixels[64];
        for (int i = 0; i < 30000; i++) {
            Pixels p = new Pixels(3 + (i & 31));
            if ((i & 511) == 0) keep[(i >> 9) & 63] = p;
            ck += p.flags[0];
        }
        churn();
        for (int i = 0; i < 64; i++) {
            if (keep[i] != null) ck = ck * 31 + keep[i].sum();
        }
        // oversize fallback: 4000 ints > any BiBOP class -> ordinary path via keep-if-null
        Pixels big = new Pixels(4000);
        ck = ck * 31 + big.sum();
        // guard path: negative size must throw before any construction effect
        try {
            Pixels bad = new Pixels(-1);
            ck += bad.n;
        } catch (IllegalArgumentException e) {
            ck += 23;
        }
        // computed size (w*h): fused when it fits, ordinary when oversize
        Img[] keepImg = new Img[16];
        for (int i = 0; i < 20000; i++) {
            Img m = new Img(2 + (i & 7), 3 + (i & 3));
            if ((i & 2047) == 0) keepImg[(i >> 11) & 15] = m;
            ck += m.pix.length;
        }
        churn();
        for (int i = 0; i < 16; i++) {
            if (keepImg[i] != null) ck = ck * 31 + keepImg[i].sum();
        }
        Img bigImg = new Img(80, 50);   // 4000 ints: oversize -> ordinary path
        ck = ck * 31 + bigImg.sum() + bigImg.pix.length;
        return ck;
    }

    /** enough garbage to force several GC cycles */
    static void churn() {
        long s = 0;
        Object[] g = new Object[64];
        for (int i = 0; i < 200000; i++) {
            g[i & 63] = new int[8 + (i & 15)];
            s += ((int[]) g[i & 63]).length;
        }
        System.gc();
        if (s == -1) System.out.println(s);
    }
}
