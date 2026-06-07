/**
 * Verifies that the JavaScript-port runtime treats synchronized blocks as
 * actual mutexes -- at most one green thread inside the block at a time.
 *
 * Architecture note: the JS port has exactly one Web Worker (one OS
 * thread) hosting the VM. Inside it, multiple Java green threads run
 * cooperatively -- yielding at sleep / wait / monitor-park points.
 * The contention here is between Java green threads sharing that
 * single Web Worker, not between OS threads. The fixture's
 * ``Contender`` class is named to avoid colliding with the Web
 * Worker terminology -- it's a regular ``java.lang.Thread`` subclass.
 *
 * Earlier revisions of jvm.monitorEnter "stole" the lock on contention
 * (pushed the holder's owner/count onto a stack, took over, unwound on
 * exit). That meant TWO green threads could be inside the same
 * synchronized block at once, mutating shared state with the locking
 * protocol promising they couldn't.
 *
 * The load is intentionally heavy. With only two contenders and a
 * handful of iterations the bug reproduces only sporadically because
 * the steal interleave depends on yield timing. Six contenders
 * looping 25 times produces 150 critical-section entries each with a
 * yield inside; under cooperative scheduling, every yield is a chance
 * for a stealing implementation to interleave another contender into
 * the same block.
 *
 * If the runtime enforces real mutual exclusion, ``maxConcurrent``
 * stays at 1 and the test reports ``result = 511``. With the old
 * lock-stealing path it observed values >= 2 and reports 0.
 */
public class JsMonitorMutexApp {
    static final int CONTENDERS = 6;
    static final int ITERATIONS = 25;

    static final Object LOCK = new Object();
    static volatile int entered;
    static volatile int maxConcurrent;
    static volatile int totalEntries;
    public static volatile int result;

    static class Contender extends Thread {
        public void run() {
            for (int i = 0; i < ITERATIONS; i++) {
                synchronized (LOCK) {
                    int n = ++entered;
                    if (n > maxConcurrent) {
                        maxConcurrent = n;
                    }
                    totalEntries++;
                    // Yield WITHIN the critical section so a stealing
                    // implementation has a chance to context-switch
                    // another contender into the block. Cooperative
                    // monitor semantics keep the others parked on
                    // entrants until this contender exits the block.
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException ignored) {
                    }
                    --entered;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Contender[] contenders = new Contender[CONTENDERS];
        for (int i = 0; i < CONTENDERS; i++) {
            contenders[i] = new Contender();
        }
        for (int i = 0; i < CONTENDERS; i++) {
            contenders[i].start();
        }
        for (int i = 0; i < CONTENDERS; i++) {
            contenders[i].join();
        }

        boolean mutexHeld = (maxConcurrent == 1);
        boolean allRan = (totalEntries == CONTENDERS * ITERATIONS);
        result = (mutexHeld && allRan) ? 511 : 0;
    }
}
