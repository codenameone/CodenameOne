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
 * protocol promising they couldn't. This fixture loops two contenders
 * over the same lock; each tracks ``entered`` (incremented inside the
 * block, decremented on exit) and ``maxConcurrent`` (high-water mark).
 *
 * If the runtime enforces real mutual exclusion, ``maxConcurrent``
 * stays at 1 and the test reports ``result = 511``. With the old
 * lock-stealing path it observed values >= 2 and reports 0.
 */
public class JsMonitorMutexApp {
    static final Object LOCK = new Object();
    static volatile int entered;
    static volatile int maxConcurrent;
    public static volatile int result;

    static class Contender extends Thread {
        public void run() {
            for (int i = 0; i < 5; i++) {
                synchronized (LOCK) {
                    int n = ++entered;
                    if (n > maxConcurrent) {
                        maxConcurrent = n;
                    }
                    // Yield WITHIN the critical section so a stealing
                    // implementation has a chance to context-switch the
                    // other contender into the block. Cooperative monitor
                    // semantics keep the other contender parked on
                    // entrants.
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
        Contender t1 = new Contender();
        Contender t2 = new Contender();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        result = (maxConcurrent == 1) ? 511 : 0;
    }
}
