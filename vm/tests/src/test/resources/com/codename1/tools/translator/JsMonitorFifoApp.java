/**
 * Verifies that contended monitor entrants are admitted in FIFO order
 * (the order they parked). Many Java green threads (``Entrant``) each
 * park on the same lock held by main; when main releases, they should
 * run in registration order.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play.
 * "Many entrants" here means many Java green threads parked on the
 * monitor's entrants queue inside that single Worker -- the FIFO
 * property is enforced by the cooperative scheduler, not by any OS
 * scheduling fairness.
 *
 * Heavy load matters: with only three entrants the order verification
 * is too coarse to catch a partial reordering bug. With twelve
 * entrants any swap between adjacent slots is detected.
 *
 * If the runtime correctly preserves entrant order, the captured
 * sequence is [1,2,3,...,N] and the test reports ``result = 511``.
 * Out-of-order promotion -- which the old lock-stealing path could
 * exhibit if a later-arriving thread "stole" before earlier entrants
 * drained -- would record a different sequence and report 0.
 */
public class JsMonitorFifoApp {
    static final int ENTRANTS = 12;

    static final Object LOCK = new Object();
    static final int[] order = new int[ENTRANTS];
    static volatile int orderIdx;
    public static volatile int result;

    static class Entrant extends Thread {
        final int id;
        Entrant(int id) { this.id = id; }
        public void run() {
            synchronized (LOCK) {  // contended -- main holds it
                int slot = orderIdx++;
                if (slot >= 0 && slot < order.length) {
                    order[slot] = id;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Entrant[] entrants = new Entrant[ENTRANTS];
        for (int i = 0; i < ENTRANTS; i++) {
            entrants[i] = new Entrant(i + 1);
        }

        synchronized (LOCK) {
            // Hold the lock and start entrants in known order. Each will
            // park on the monitor's entrants queue in the order they
            // reach the synchronized block. ``Thread.sleep(0)`` between
            // starts yields so each entrant has a turn to actually run
            // up to the lock acquisition before the next is started.
            for (int i = 0; i < ENTRANTS; i++) {
                entrants[i].start();
                Thread.sleep(0);
            }
            // Give all entrants a chance to actually reach the lock and
            // park before we release. A short sleep plus join below is
            // enough in the cooperative scheduler.
            Thread.sleep(5);
        }

        for (int i = 0; i < ENTRANTS; i++) {
            entrants[i].join();
        }

        boolean ok = true;
        for (int i = 0; i < ENTRANTS; i++) {
            if (order[i] != i + 1) {
                ok = false;
                break;
            }
        }
        result = ok ? 511 : 0;
    }
}
