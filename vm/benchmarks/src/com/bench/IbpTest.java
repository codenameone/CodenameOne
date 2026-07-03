package com.bench;

/**
 * Torture test for the init-before-publish (memset elimination) inline-ctor path.
 * Every hazard the arg-temp hoisting must protect:
 *   1. Java left-to-right argument evaluation when args are side-effectful calls
 *      (the ctor stores fields in a DIFFERENT order than the args).
 *   2. Single evaluation of an arg the ctor stores into TWO fields.
 *   3. A throwing arg expression -- must throw BEFORE any allocation effect,
 *      and the VM must stay consistent (no half-built object anywhere).
 *      (Explicit throw from a called method: raw null-field derefs are
 *      compile-gated off in this VM config -- pre-existing, unrelated.)
 *   4. Unwritten fields of a partial ctor read as 0 / null / 0L / 0.0.
 *   5. Long/double args (two-slot locals) mixed with calls.
 *   6. Allocation churn with call-args so GC safepoints land inside the callee
 *      while a marked new-site is mid-construction (conservative-scan window).
 * Output is a deterministic event log + checksum, compared verbatim to HotSpot.
 */
public class IbpTest {
    static final StringBuilder LOG = new StringBuilder();
    static int seq = 0;

    static int f() { LOG.append("f").append(++seq).append(";"); return seq * 3; }
    static int g() { LOG.append("g").append(++seq).append(";"); return seq * 7; }
    static long fl() { LOG.append("L").append(++seq).append(";"); return seq * 11L; }
    static double fd() { LOG.append("D").append(++seq).append(";"); return seq * 0.5; }

    /** stores REVERSED relative to arg order */
    static class Rev {
        int a; int b;
        Rev(int a, int b) { this.b = b; this.a = a; }
    }

    /** one param stored into two fields */
    static class Twice {
        int x; int y;
        Twice(int v) { this.x = v; this.y = v; }
    }

    /** partial ctor: leaves half the fields unwritten (must read as defaults) */
    static class Partial {
        int w; Object o1; long l1; double d1; Object o2; int z;
        Partial(int w, Object o1) { this.w = w; this.o1 = o1; }
    }

    /** wide args mixed with narrow */
    static class Wide {
        long l; int i; double d;
        Wide(long l, int i, double d) { this.l = l; this.i = i; this.d = d; }
    }

    static class Holder { int v; Holder(int v) { this.v = v; } }

    /** call-arg that allocates enough to force GC cycles mid-argument */
    static int churnCall(int i) {
        Object[] garbage = new Object[16];
        for (int k = 0; k < 16; k++) {
            garbage[k] = new Holder(i + k);
        }
        int s = 0;
        for (int k = 0; k < 16; k++) {
            s += ((Holder) garbage[k]).v;
        }
        return s;
    }

    static int boom() { LOG.append("boom").append(++seq).append(";"); throw new IllegalStateException("boom"); }

    public static void main(String[] args) {
        long ck = 0;

        // 1+2: eval order with reversed stores; double-store single eval
        Rev r = new Rev(f(), g());
        ck = ck * 31 + r.a + r.b * 1000L;
        Twice t = new Twice(f());
        ck = ck * 31 + t.x + t.y * 1000L;

        // 3: throwing arg -- must precede any construction effect (f runs, boom
        // throws, no object is built; the earlier f() side effect is kept)
        try {
            Rev bad = new Rev(f(), boom());
            ck += bad.a; // unreachable
        } catch (IllegalStateException e) {
            LOG.append("ISE;");
        }

        // 4: unwritten fields are Java defaults
        Partial p = new Partial(f(), new Object());
        ck = ck * 31 + p.w + p.z + p.l1 + (long) p.d1 + (p.o1 == null ? 1 : 2) + (p.o2 == null ? 10 : 20);

        // 5: wide args with calls
        Wide w = new Wide(fl(), f(), fd());
        ck = ck * 31 + w.l + w.i + (long) (w.d * 2);

        // 6: churn -- marked new-sites whose args call into allocating code
        long churn = 0;
        Holder head = null;
        for (int i = 0; i < 400000; i++) {
            Holder h = new Holder(churnCall(i));
            churn += h.v;
            // and a linked chain so some survive
            if ((i & 1023) == 0) {
                head = new Holder(head == null ? i : head.v + i);
            }
        }
        ck = ck * 31 + churn + (head == null ? 0 : head.v);

        System.out.println("LOG " + LOG);
        System.out.println("CK " + ck);
        System.out.println("DONE");
    }
}
