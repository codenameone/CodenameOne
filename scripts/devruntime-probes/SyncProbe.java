import com.codename1.ui.*;

/**
 * Monitors under contention, on the device.
 *
 * Each counter is guarded by exactly one monitor -- a synchronized method takes
 * the class's or the receiver's, a block takes whatever it names -- so the
 * totals are exact, and an interpreter that treats monitorenter as a no-op
 * loses increments rather than merely reordering them.
 */
public class SyncProbe {
    static int byLock;
    static int byMethod;
    int byInstance;
    static final Object LOCK = new Object();

    static synchronized void bumpStatic() { byMethod++; }
    synchronized void bumpInstance() { byInstance++; }
    static void guarded() { synchronized (LOCK) { byLock++; } }

    public static void main(String[] a) throws Exception {
        final SyncProbe shared = new SyncProbe();
        Thread[] t = new Thread[4];
        for (int i = 0; i < t.length; i++) {
            t[i] = new Thread(new Runnable() {
                public void run() {
                    for (int j = 0; j < 1000; j++) {
                        bumpStatic(); shared.bumpInstance(); guarded();
                    }
                }
            });
        }
        for (int i = 0; i < t.length; i++) { t[i].start(); }
        for (int i = 0; i < t.length; i++) { t[i].join(); }
        System.out.println("PROBE SyncProbe: method=" + byMethod + " instance=" + shared.byInstance
            + " lock=" + byLock + " expected=4000 each");
        new Form("Sync").show();
    }
}
