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
            // reach the synchronized block.
            //
            // ``Thread.sleep(1)`` (not sleep(0)) between starts is
            // required for FIFO determinism on the cooperative scheduler:
            // sleep(0) is a pure runqueue-tail enqueue with no real-time
            // delay, so depending on which generator step finishes first,
            // the newly-started entrant may not have reached
            // ``monitorEnter`` before main resumes from sleep(0) and
            // queues the next entrant. Whether it has or hasn't depends
            // on how many synthetic ``yield* _Y()`` budget yields the
            // translator emitted into the entrant's run() prologue, which
            // varies across CompilerConfig parameterisations and host
            // JS engines (Linux Node.js consistently lost the race;
            // macOS Node.js mostly won it). A wall-clock park drives the
            // scheduler's _wakeupTimer through setTimeout, which in turn
            // drains the runqueue all the way down to the empty state
            // before main resumes -- pinning the push-into-monitor.entrants
            // order to start order.
            for (int i = 0; i < ENTRANTS; i++) {
                entrants[i].start();
                Thread.sleep(1);
            }
            // Final guaranteed wall-clock window for any entrant whose
            // step happened to be in flight when the per-iteration timer
            // fired. 20 ms is well under the test deadline and dwarfs
            // the per-step work in the cooperative scheduler.
            Thread.sleep(20);
        }

        for (int i = 0; i < ENTRANTS; i++) {
            entrants[i].join();
        }

        boolean ok = true;
        StringBuilder actual = new StringBuilder("[");
        for (int i = 0; i < ENTRANTS; i++) {
            if (i > 0) actual.append(",");
            actual.append(order[i]);
            if (order[i] != i + 1) {
                ok = false;
            }
        }
        actual.append("]");
        // Always print the order so CI surfaces it whether the test
        // passed or failed -- earlier diagnostic-on-failure attempts
        // never made it into the rawMessage assertion field. The
        // ``CN1FIFO:`` prefix stays unique enough for grep matching.
        System.out.println("CN1FIFO:order=" + actual);
        result = ok ? 511 : 0;
    }
}
