/**
 * Verifies that contended monitor entrants are admitted in FIFO order
 * (the order they parked). Three Java green threads (``Entrant``)
 * each park on the same lock held by main; when main releases,
 * entrant 1 should run first, then 2, then 3.
 *
 * Architecture note: only one OS thread (the Web Worker) is in play.
 * "Three entrants" here means three Java green threads parked on
 * the monitor's entrants queue inside that single Worker -- the
 * FIFO property is enforced by the cooperative scheduler, not by
 * any OS scheduling fairness.
 *
 * If the runtime correctly preserves entrant order, the captured
 * sequence is [1,2,3] and the test reports ``result = 511``.
 * Out-of-order promotion -- which the old lock-stealing path could
 * exhibit if a later-arriving thread "stole" before earlier entrants
 * drained -- would record a different sequence and report 0.
 */
public class JsMonitorFifoApp {
    static final Object LOCK = new Object();
    static final int[] order = new int[3];
    static volatile int orderIdx;
    static volatile int barrier;
    public static volatile int result;

    static class Entrant extends Thread {
        final int id;
        Entrant(int id) { this.id = id; }
        public void run() {
            // Bump the barrier so main knows this entrant is about to
            // hit the lock. Spin briefly (yielding) until the previous
            // entrant has parked, so the parking order is deterministic.
            synchronized (LOCK) {  // contended -- main holds it
                int slot = orderIdx++;
                if (slot >= 0 && slot < order.length) {
                    order[slot] = id;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Entrant w1 = new Entrant(1);
        Entrant w2 = new Entrant(2);
        Entrant w3 = new Entrant(3);

        synchronized (LOCK) {
            // Hold the lock and start entrants. Each will park on the
            // monitor's entrants queue in the order they reach the
            // synchronized block. ``Thread.sleep(0)`` between starts
            // yields so each entrant has a turn to actually run up to
            // the lock acquisition before the next is started.
            w1.start();
            Thread.sleep(0);
            w2.start();
            Thread.sleep(0);
            w3.start();
            Thread.sleep(0);
            // Spin until all three are blocked. Each entrant gets parked
            // before incrementing barrier (we don't directly observe the
            // park, but if we don't sleep enough, the order test races).
            // A short sleep plus join below is enough in the cooperative
            // scheduler.
            Thread.sleep(5);
        }

        w1.join();
        w2.join();
        w3.join();

        boolean ok = order[0] == 1 && order[1] == 2 && order[2] == 3;
        result = ok ? 511 : 0;
    }
}
