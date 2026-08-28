package com.bench;

/**
 * Bulk object-array copies while a collection is in progress.
 *
 * <p>The SATB barrier on {@code System.arraycopy} and {@code Object[].clone()} has to log
 * the references a bulk copy moves, and the per-store barrier's enqueue takes the SATB
 * mutex once per reference. This driver exists to price that: a large array of OLD objects
 * copied over and over while a second thread keeps the collector busy, so the barrier is
 * armed for most of the run.</p>
 */
public class BulkCopyCost {
    private static final int ARRAY = 200000;
    private static final int COPIES = 400;
    private static final int CHURN = 60000;

    static final class Node { int v; Node peer; }

    static Object[] source;
    static Object[] dest;
    static Object sink;
    static volatile boolean stop;
    static long checksum;

    public static void main(String[] args) {
        source = new Object[ARRAY];
        for (int i = 0; i < ARRAY; i++) {
            Node n = new Node();
            n.v = i;
            source[i] = n;
        }
        dest = new Object[ARRAY];

        Thread churn = new Thread() {
            public void run() {
                Object last = null;
                while (!stop) {
                    for (int i = 0; i < CHURN; i++) {
                        Node n = new Node();
                        n.peer = (Node) last;
                        if ((i & 127) == 0) { last = n; }
                    }
                    sink = last;
                }
            }
        };
        churn.start();

        long t0 = System.currentTimeMillis();
        for (int r = 0; r < COPIES; r++) {
            System.arraycopy(source, 0, dest, 0, ARRAY);
            Object[] c = (Object[]) source.clone();
            checksum += ((Node) c[r % ARRAY]).v;
        }
        long elapsed = System.currentTimeMillis() - t0;
        stop = true;
        try { churn.join(); } catch (InterruptedException e) { }

        System.out.println("COPIES=" + COPIES + " ARRAY=" + ARRAY);
        System.out.println("COPY_MS=" + elapsed);
        System.out.println("RESULT=" + checksum);
        System.out.println("BULK_COPY_COST_DONE");
    }
}
