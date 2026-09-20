package com.bench;

/**
 * A null array access must throw NullPointerException, and must do so BEFORE any
 * bounds complaint, whichever mechanism produces it.
 *
 * Where the platform allows it the VM omits the null test entirely: the bounds
 * check's load of ->length faults on a null array, and a signal handler turns
 * that into the exception. This pins the observable behaviour so that choice
 * cannot change it -- including the ordering (a null array with an out-of-range
 * index is still NPE, not ArrayIndexOutOfBounds) and the ability to CATCH it and
 * keep running, repeatedly, which is what distinguishes a handled fault from a
 * crash that happens to be survivable once.
 */
public class NullDeref {
    static int[] nullInts;
    static Object[] nullObjs;
    static long sink;

    static String tryIt(String what, Runnable r) {
        try { r.run(); return what + "=NO-THROW"; }
        catch (NullPointerException e) { return what + "=NPE"; }
        catch (ArrayIndexOutOfBoundsException e) { return what + "=AIOOBE"; }
        catch (Throwable t) { return what + "=" + t.getClass().getName(); }
    }

    public static void main(String[] args) {
        System.out.println(tryIt("read0", new Runnable() { public void run() { sink = nullInts[0]; } }));
        System.out.println(tryIt("write0", new Runnable() { public void run() { nullInts[0] = 1; } }));
        System.out.println(tryIt("readNeg", new Runnable() { public void run() { sink = nullInts[-1]; } }));
        System.out.println(tryIt("readBig", new Runnable() { public void run() { sink = nullInts[999999]; } }));
        System.out.println(tryIt("length", new Runnable() { public void run() { sink = nullInts.length; } }));
        System.out.println(tryIt("objRead", new Runnable() { public void run() { sink = nullObjs[0] == null ? 0 : 1; } }));
        System.out.println(tryIt("objWrite", new Runnable() { public void run() { nullObjs[0] = "x"; } }));

        // A real array still works, and a real out-of-range index is still AIOOBE.
        final int[] real = new int[4];
        real[2] = 7;
        System.out.println("real=" + real[2] + " len=" + real.length);
        System.out.println(tryIt("realOob", new Runnable() { public void run() { sink = real[9]; } }));

        // Catch it many times: a handled fault must be repeatable, and the VM must
        // still be healthy afterwards (allocation, GC, ordinary work).
        int npes = 0;
        for (int i = 0; i < 50000; i++) {
            try { sink = nullInts[i & 7]; } catch (NullPointerException e) { npes++; }
            if ((i & 4095) == 0) { System.gc(); }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) { sb.append(i); }
        System.out.println("repeated=" + npes + " healthy=" + sb.length());
        System.out.println("DONE");
    }
}
