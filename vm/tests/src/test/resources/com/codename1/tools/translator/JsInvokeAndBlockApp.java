/**
 * Models the cooperative-scheduling pattern that
 * Display.invokeAndBlock + Dialog body-thread polling rely on. This
 * is the scenario that exposed the original lock-stealing bug:
 *
 *   - A "blocker" thread loops on a shared lock, doing
 *     ``synchronized(L) { if (cond) break; L.wait(N); }``.
 *   - A "notifier" thread eventually calls ``synchronized(L) { cond =
 *     true; L.notifyAll(); }``.
 *   - The blocker wakes, observes the condition, exits.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play.
 * The blockers and notifiers are Java green threads cooperatively
 * scheduled inside that single Web Worker. Naming them
 * ``Blocker`` / ``Notifier`` avoids any host Web-Worker name
 * collision in the source.
 *
 * Heavy load matters: with only one (blocker, notifier) pair the
 * scheduler runs the simplest possible cooperative interleave -- if
 * there is a state leak between rounds, or contention between
 * concurrent invokeAndBlock-like sessions, this fixture has to
 * exercise it. We run SESSIONS independent (lock, blocker, notifier)
 * tuples concurrently; every session has to complete cleanly. Each
 * session in turn cycles through ROUNDS of the cooperative wait /
 * notify pattern to catch any per-cycle leak.
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
 *   - Any state leak between rounds or between concurrent sessions
 *     would surface when the next round / session deadlocks or
 *     observes stale state.
 */
public class JsInvokeAndBlockApp {
    static final int SESSIONS = 4;
    static final int ROUNDS = 6;

    static volatile int sessionsCompleted;
    public static volatile int result;

    static class Session {
        final Object lock = new Object();
        volatile boolean cond;
        volatile int loopCount;
        volatile boolean blockerExitedCleanly;
        volatile boolean notifierExitedCleanly;
    }

    static class Blocker extends Thread {
        final Session s;
        Blocker(Session s) { this.s = s; }
        public void run() {
            for (int round = 0; round < ROUNDS; round++) {
                synchronized (s.lock) {
                    while (!s.cond) {
                        s.loopCount++;
                        if (s.loopCount > 200 * ROUNDS) {
                            return;  // watchdog
                        }
                        try {
                            s.lock.wait(50);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    // Reset for next round under the lock.
                    s.cond = false;
                }
            }
            s.blockerExitedCleanly = true;
        }
    }

    static class Notifier extends Thread {
        final Session s;
        Notifier(Session s) { this.s = s; }
        public void run() {
            for (int round = 0; round < ROUNDS; round++) {
                // Yield a couple of times so the blocker has actually
                // entered the wait-loop and parked before we acquire.
                try {
                    Thread.sleep(2);
                } catch (InterruptedException ignored) {
                }
                synchronized (s.lock) {
                    s.cond = true;
                    s.lock.notifyAll();
                }
                // Wait until the blocker has consumed cond before
                // looping to the next round, so each round is a
                // distinct wait/notify cycle.
                long deadline = System.currentTimeMillis() + 5000;
                while (true) {
                    synchronized (s.lock) {
                        if (!s.cond) {
                            break;
                        }
                    }
                    if (System.currentTimeMillis() > deadline) {
                        return;  // watchdog
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            s.notifierExitedCleanly = true;
        }
    }

    public static void main(String[] args) throws Exception {
        Session[] sessions = new Session[SESSIONS];
        Blocker[] blockers = new Blocker[SESSIONS];
        Notifier[] notifiers = new Notifier[SESSIONS];
        for (int i = 0; i < SESSIONS; i++) {
            sessions[i] = new Session();
            blockers[i] = new Blocker(sessions[i]);
            notifiers[i] = new Notifier(sessions[i]);
        }
        // Start all blockers first so they are parked on wait when
        // the notifiers begin notifying.
        for (int i = 0; i < SESSIONS; i++) {
            blockers[i].start();
        }
        for (int i = 0; i < SESSIONS; i++) {
            notifiers[i].start();
        }
        for (int i = 0; i < SESSIONS; i++) {
            blockers[i].join();
            notifiers[i].join();
        }

        for (int i = 0; i < SESSIONS; i++) {
            if (sessions[i].blockerExitedCleanly && sessions[i].notifierExitedCleanly) {
                sessionsCompleted++;
            }
        }

        if (sessionsCompleted == SESSIONS) result |= 1;
        boolean allDone = true;
        for (int i = 0; i < SESSIONS; i++) {
            if (blockers[i].isAlive() || notifiers[i].isAlive()) {
                allDone = false;
                break;
            }
        }
        if (allDone) result |= 2;
        boolean noWatchdog = true;
        for (int i = 0; i < SESSIONS; i++) {
            if (sessions[i].loopCount > 200 * ROUNDS) {
                noWatchdog = false;
                break;
            }
        }
        if (noWatchdog) result |= 4;

        if (result == (1 | 2 | 4)) {
            result = 511;
        }
    }
}
