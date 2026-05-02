/**
 * Models the cooperative-scheduling pattern that
 * Display.invokeAndBlock + Dialog body-thread polling rely on. This
 * is the scenario that exposed the original lock-stealing bug:
 *
 *   - The "blocker" (main thread, playing the EDT-like role) loops
 *     on a shared lock, doing ``synchronized(L) { if (cond) break;
 *     L.wait(N); }``.
 *   - A "notifier" thread eventually calls ``synchronized(L) { cond
 *     = true; L.notifyAll(); }``.
 *   - The blocker wakes, observes the condition, exits.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play.
 * The blocker and notifier are Java green threads cooperatively
 * scheduled inside that single Web Worker. The names ``blocker`` /
 * ``notifier`` avoid colliding with the host Web-Worker terminology.
 *
 * If the runtime correctly:
 *   1. Releases the monitor on L.wait (so the notifier can acquire it);
 *   2. Re-acquires on resume (so post-wait reads see the notifier's
 *      update under monitor protection);
 *   3. Doesn't busy-spin or deadlock between the two threads;
 * then the test reports ``result = 511``.
 *
 * Failure modes this catches:
 *   - wait without releasing the monitor would deadlock the notifier
 *     (it can't acquire the lock to set cond / notify).
 *   - notifyAll without re-acquiring on the waiter side would let
 *     the blocker observe stale ``cond``.
 *   - A scheduler that "steals" the lock under the notifier (the old
 *     behavior) could let the blocker observe ``cond=true`` BEFORE
 *     the notifier actually entered the synchronized block, depending
 *     on the steal interleave.
 */
public class JsInvokeAndBlockApp {
    static final Object LOCK = new Object();
    static volatile boolean cond;
    static volatile int loopCount;
    public static volatile int result;

    public static void main(String[] args) throws Exception {
        Thread notifier = new Thread() {
            public void run() {
                // Yield a couple of times so the blocker (main) has
                // actually entered the wait-loop and parked before we
                // try to acquire.
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                }
                synchronized (LOCK) {
                    cond = true;
                    LOCK.notifyAll();
                }
            }
        };
        notifier.start();

        // Main (the blocker) loops, waiting on the lock for cond to be
        // set. Modeled on RunnableWrapper.run / Display.invokeAndBlock's
        // poll body.
        synchronized (LOCK) {
            while (!cond) {
                loopCount++;
                if (loopCount > 200) {
                    // Watchdog: in the bad case (wait doesn't release)
                    // we spin forever; cap iterations so the test fails
                    // visibly instead of hanging the harness.
                    break;
                }
                try {
                    LOCK.wait(50);
                } catch (InterruptedException ignored) {
                }
            }
        }

        notifier.join();

        if (cond) result |= 1;
        if (!notifier.isAlive()) result |= 2;
        // We don't care about exact loopCount, just that we exited
        // because cond got set, not because the watchdog fired.
        if (loopCount <= 200) result |= 4;

        if (result == (1 | 2 | 4)) {
            result = 511;
        }
    }
}
