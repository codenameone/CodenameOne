/**
 * Verifies that ``Object.wait()`` correctly releases the monitor so
 * another thread can ENTER the same synchronized block, and that the
 * waiting thread re-acquires the monitor before resuming.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play.
 * Multiple Java green threads (named ``Waiter`` to avoid any host
 * Web-Worker name collision) park on the lock's wait set; the
 * cooperative scheduler must move them to the entrants queue on
 * notify and release the lock on each wait so the wait set is
 * actually unparked.
 *
 * Heavy load matters because notifyAll has to promote N waiters from
 * the wait set into the entrants queue; each then re-acquires the
 * lock one at a time as the previous waiter exits. Eight waiters
 * exercise this cascade -- with one waiter the test would not
 * distinguish "moved to entrants" from "woke directly".
 *
 * Sequence:
 *   1. Waiters enter synchronized(LOCK), bump readyCount under the
 *      lock, then LOCK.wait().
 *   2. Main spins until readyCount == WAITERS (all are parked).
 *   3. Main enters synchronized(LOCK) -- if wait didn't release the
 *      monitor on every waiter, this would block forever.
 *   4. Main sets ``signal``, calls LOCK.notifyAll(), exits the block.
 *   5. Waiters wake, re-acquire LOCK one at a time, observe ``signal``,
 *      bump workDone, exit.
 *
 * If the runtime implements wait/notify correctly the test reports
 * ``result = 511``.
 */
public class JsMonitorWaitReleaseApp {
    static final int WAITERS = 8;

    static final Object LOCK = new Object();
    static volatile int readyCount;
    static volatile boolean signal;
    static volatile int workDone;
    public static volatile int result;

    static class Waiter extends Thread {
        public void run() {
            synchronized (LOCK) {
                readyCount++;
                while (!signal) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                workDone++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Waiter[] waiters = new Waiter[WAITERS];
        for (int i = 0; i < WAITERS; i++) {
            waiters[i] = new Waiter();
        }
        for (int i = 0; i < WAITERS; i++) {
            waiters[i].start();
        }

        // Spin until every waiter has entered the synchronized block,
        // bumped readyCount, and called wait() (which releases the
        // monitor). If wait failed to release for ANY waiter,
        // readyCount would not hit WAITERS because subsequent waiters
        // would block on monitorEnter.
        long deadline = System.currentTimeMillis() + 5000;
        while (readyCount < WAITERS) {
            if (System.currentTimeMillis() > deadline) {
                break;  // watchdog -- test will fail visibly
            }
            Thread.sleep(1);
        }
        // Give the last waiter a chance to actually reach LOCK.wait()
        // and park (readyCount++ happens before wait, so a brief sleep
        // is enough in the cooperative scheduler).
        Thread.sleep(2);

        synchronized (LOCK) {
            // Main must be able to acquire LOCK while every waiter is
            // in wait() -- if wait didn't release the monitor, this
            // would deadlock the test (translateAndRunFixture would
            // time out).
            signal = true;
            LOCK.notifyAll();
        }

        for (int i = 0; i < WAITERS; i++) {
            waiters[i].join();
        }

        if (readyCount == WAITERS) result |= 1;
        if (workDone == WAITERS) result |= 2;
        if (signal) result |= 4;
        boolean allDead = true;
        for (int i = 0; i < WAITERS; i++) {
            if (waiters[i].isAlive()) {
                allDead = false;
                break;
            }
        }
        if (allDead) result |= 8;

        if (result == (1 | 2 | 4 | 8)) {
            result = 511;
        }
    }
}
