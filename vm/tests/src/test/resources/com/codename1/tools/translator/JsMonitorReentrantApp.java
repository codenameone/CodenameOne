/**
 * Verifies that a thread that already holds a monitor can re-enter the
 * same synchronized block without parking. ``monitorEnter`` MUST take
 * the fast ``count++`` path when ``monitor.owner === thread.id``;
 * otherwise it would wrongly park itself on its own monitor and
 * deadlock.
 *
 * Tests three patterns:
 *   - Direct re-entry: synchronized(L) { synchronized(L) { ... } }
 *   - Method-call re-entry: synchronized(L) { method that synchronized(L) }
 *   - Synchronized-method re-entry on the same instance receiver
 *
 * Each successful re-entry sets a bit. result == 511 means all worked.
 */
public class JsMonitorReentrantApp {
    static final Object LOCK = new Object();
    public static volatile int result;

    static class Holder {
        synchronized void recurse(int depth) {
            if (depth > 0) {
                recurse(depth - 1);
            }
        }
    }

    static void inner() {
        synchronized (LOCK) {
            result |= 4;
        }
    }

    public static void main(String[] args) throws Exception {
        // Pattern 1: direct re-entry.
        synchronized (LOCK) {
            result |= 1;
            synchronized (LOCK) {
                result |= 2;
            }
        }
        // Pattern 2: re-entry via method call inside the same lock.
        synchronized (LOCK) {
            inner();  // sets result |= 4
        }
        // Pattern 3: synchronized-method recursion -- each call re-enters
        // the same instance's monitor.
        Holder h = new Holder();
        h.recurse(5);
        result |= 8;

        // Pattern 4: verify lock is fully released after the outer
        // exit: a fresh thread should be able to acquire it without
        // contention.
        final boolean[] ok = new boolean[1];
        Thread other = new Thread() {
            public void run() {
                synchronized (LOCK) {
                    ok[0] = true;
                }
            }
        };
        other.start();
        other.join();
        if (ok[0]) {
            result |= 16;
        }

        // Sanity bits to round result up to 511 if everything passed.
        if (result == (1 | 2 | 4 | 8 | 16)) {
            result = 511;
        }
    }
}
