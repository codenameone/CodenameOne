package com.bench;

/**
 * Stress for the dead-thread pending-migration path and allObjectsInHeap growth:
 * short-lived threads allocate ARRAYS (legacy-heap objects that go through
 * pendingHeapAllocations -> placeObjectInHeapCollection) and die, while the main
 * thread keeps >30000 arrays live so the global table must GROW (the old code
 * grew it on the DYING thread concurrently with the GC's lock-free sweep and
 * snapshot walks -- lost slot-NULLs / double-growth use-after-free). The final
 * sum is order-independent, so the checksum is deterministic despite scheduling.
 */
public class ThreadChurn {
    static final int SURVIVORS = 42000;   // > initial table size (30000) => growth
    static long[] workerSum = new long[8];
    static int[][] survivors = new int[SURVIVORS][];

    public static void main(String[] args) throws Exception {
        long ck = 0;
        int survivorIdx = 0;

        // Build the live set that forces table growth, interleaved with GC pressure.
        for (int i = 0; i < SURVIVORS; i++) {
            int[] a = new int[4 + (i & 15)];
            a[0] = i;
            a[a.length - 1] = i * 3;
            survivors[i] = a;
        }

        for (int round = 0; round < 12; round++) {
            final int r = round;
            Thread[] ts = new Thread[8];
            for (int w = 0; w < 8; w++) {
                final int wi = w;
                ts[w] = new Thread(new Runnable() {
                    public void run() {
                        long s = 0;
                        // arrays => legacy heap => pendingHeapAllocations; the thread
                        // dies with many entries still pending migration
                        for (int i = 0; i < 3000; i++) {
                            int[] a = new int[3 + ((r + i) & 31)];
                            a[0] = wi + i;
                            a[a.length - 1] = i;
                            s += a[0] + a[a.length - 1];
                        }
                        // churn small objects too so GC cycles run concurrently
                        StringBuilder b = new StringBuilder();
                        for (int i = 0; i < 2000; i++) {
                            b.append(i & 7);
                            if (b.length() > 64) b.setLength(0);
                        }
                        s += b.length();
                        workerSum[wi] = s;
                    }
                });
            }
            for (Thread t : ts) t.start();
            // main keeps mutating the survivor set (sweep + table writes)
            for (int i = 0; i < 4000; i++) {
                int idx = (round * 4000 + i) % SURVIVORS;
                int[] a = new int[4 + (i & 15)];
                a[0] = idx;
                a[a.length - 1] = round;
                survivors[idx] = a;
            }
            for (Thread t : ts) t.join();
            for (int w = 0; w < 8; w++) ck += workerSum[w];
            System.gc();
        }

        for (int i = 0; i < SURVIVORS; i += 97) {
            int[] a = survivors[i];
            ck = ck * 31 + a[0] + a[a.length - 1];
        }
        System.out.println("DONE " + ck);
    }
}
