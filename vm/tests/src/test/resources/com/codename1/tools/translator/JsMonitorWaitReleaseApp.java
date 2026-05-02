/**
 * Verifies that ``Object.wait()`` correctly releases the monitor so
 * another thread can ENTER the same synchronized block, and that the
 * waiting thread re-acquires the monitor before resuming.
 *
 * Sequence:
 *   1. Worker enters synchronized(LOCK), then LOCK.wait().
 *   2. Main waits until worker has parked (worker sets ``ready``
 *      under the lock before waiting).
 *   3. Main enters synchronized(LOCK) -- if wait didn't release the
 *      monitor, this would block forever.
 *   4. Main sets ``signal``, calls LOCK.notifyAll(), exits the block.
 *   5. Worker wakes, re-acquires LOCK, observes ``signal``, exits.
 *
 * If the runtime implements wait/notify correctly the test reports
 * ``result = 511``.
 */
public class JsMonitorWaitReleaseApp {
    static final Object LOCK = new Object();
    static volatile boolean ready;
    static volatile boolean signal;
    static volatile boolean workerReleasedAfterWait;
    public static volatile int result;

    public static void main(String[] args) throws Exception {
        final Object[] mainEnteredSecond = new Object[1];
        Thread worker = new Thread() {
            public void run() {
                synchronized (LOCK) {
                    ready = true;
                    while (!signal) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                    workerReleasedAfterWait = true;
                }
            }
        };
        worker.start();

        // Wait for the worker to enter the synchronized block and call
        // wait() (which releases the monitor). It signals readiness via
        // ``ready`` (set under the lock just before the wait), then
        // releases by yielding on wait().
        while (!ready) {
            Thread.sleep(1);
        }
        // Give the worker a chance to actually reach LOCK.wait() and
        // park. The cooperative scheduler should run worker until its
        // wait yield before main resumes.
        Thread.sleep(2);

        synchronized (LOCK) {
            // Main must be able to acquire LOCK while worker is in
            // wait() -- if wait didn't release the monitor, this would
            // deadlock the test (translateAndRunFixture would time out).
            mainEnteredSecond[0] = LOCK;
            signal = true;
            LOCK.notifyAll();
        }

        worker.join();

        if (mainEnteredSecond[0] == LOCK) result |= 1;
        if (workerReleasedAfterWait) result |= 2;
        if (signal) result |= 4;
        if (!worker.isAlive()) result |= 8;

        if (result == (1 | 2 | 4 | 8)) {
            result = 511;
        }
    }
}
