/**
 * Regression test for the ``wait()`` entrant-promotion bug that hung
 * lifecycle.start() in production.
 *
 * Pattern (this is the EDT + invokeAndBlock body shape):
 *   1. Thread A acquires LOCK.
 *   2. Thread B tries to acquire LOCK -- contended, parks on
 *      ``LOCK.__monitor.entrants``.
 *   3. Thread A calls ``LOCK.wait(timeout)``.
 *
 * ``waitOn`` clears ``monitor.owner`` / ``monitor.count`` so Thread A
 * is no longer the holder, then yields a ``wait`` op. The bug: that
 * release path used to NOT promote the head entrant, so the monitor
 * sat with ``owner=null, count=0, entrants=[B]`` forever. B is
 * parked on entrants, only ``monitorExit`` can promote it, but
 * monitorExit was never called (A went through waitOn instead).
 * Result: B stays parked forever. In the JS port that surfaced as
 * lifecycle.start hanging because the Initializr's first
 * synchronized(Display.lock) on main raced the EDT releasing
 * Display.lock via wait() exactly once, and main was the entrant
 * stranded by the missing promotion.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play;
 * A and B are Java green threads cooperatively scheduled inside it.
 *
 * If the runtime correctly promotes the entrant when wait() releases
 * the monitor, B acquires the lock while A is in wait(), bumps a
 * progress flag, then notifies A. A wakes, observes the flag, exits.
 * result == 511 means both threads completed cleanly.
 */
public class JsMonitorWaitPromotesEntrantApp {
    static final Object LOCK = new Object();
    static volatile boolean bAcquired;
    static volatile boolean signal;
    static volatile int aPostWaitState;
    public static volatile int result;

    static class Holder extends Thread {
        public void run() {
            synchronized (LOCK) {
                // Give B time to start and try monitorEnter (it will park on
                // entrants because A holds the lock).
                long deadline = System.currentTimeMillis() + 1000;
                while (!bAcquired && System.currentTimeMillis() < deadline) {
                    try {
                        // wait() releases the lock *and* must promote the
                        // head entrant (B). With the bug, B never gets
                        // promoted, never runs, never sets bAcquired,
                        // so this loop times out at deadline.
                        LOCK.wait(50);
                    } catch (InterruptedException ignored) {
                    }
                }
                aPostWaitState = bAcquired ? 1 : 0;
            }
        }
    }

    static class Entrant extends Thread {
        public void run() {
            synchronized (LOCK) {
                bAcquired = true;
                signal = true;
                LOCK.notifyAll();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Holder a = new Holder();
        Entrant b = new Entrant();
        a.start();
        // Sleep briefly so A reaches synchronized(LOCK) before B does;
        // that way B becomes a contended entrant rather than the
        // owner.
        Thread.sleep(2);
        b.start();

        a.join();
        b.join();

        if (bAcquired) result |= 1;
        if (signal) result |= 2;
        if (aPostWaitState == 1) result |= 4;
        if (!a.isAlive() && !b.isAlive()) result |= 8;

        if (result == (1 | 2 | 4 | 8)) {
            result = 511;
        }
    }
}
