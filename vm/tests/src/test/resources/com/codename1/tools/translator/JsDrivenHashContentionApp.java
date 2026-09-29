/**
 * Pins what happens when a hashCode() really blocks at a synchronous call site.
 *
 * hashCode() and equals(Object) call sites are emitted synchronous and a generator
 * implementation is driven to completion (JavascriptSuspensionAnalysis
 * .isDrivenSignature). A synchronized hashCode() whose monitor another green thread
 * holds across a suspension cannot wait there. The contract is that this fails with
 * the driver's named error -- never that it proceeds without the lock or returns a
 * wrong hash -- and that the failed attempt leaves the monitor usable.
 *
 * Bits:
 *   1  map.put on the contended key threw
 *   2  the throwable carries the driver's message
 *   4  once the holder releases, the same key hashes and the map works
 *   8  the monitor's entrant queue was left clean: main can lock the key
 *
 * result == 15 means all held.
 */
public class JsDrivenHashContentionApp {
    public static volatile int result;
    static volatile boolean holding;
    static volatile boolean release;

    static final class Key {
        public synchronized int hashCode() {
            return 42;
        }

        public boolean equals(Object o) {
            return o == this;
        }
    }

    public static void main(String[] args) throws Exception {
        final Key key = new Key();
        Thread holder = new Thread() {
            public void run() {
                synchronized (key) {
                    holding = true;
                    while (!release) {
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            }
        };
        holder.start();
        while (!holding) {
            Thread.sleep(2);
        }
        java.util.HashMap<Object, Object> map = new java.util.HashMap<Object, Object>();
        try {
            map.put(key, "v");
        } catch (Throwable t) {
            result |= 1;
            String message = t.getMessage();
            if (message != null && message.indexOf("sync virtual dispatch reached a blocking method") >= 0) {
                result |= 2;
            }
        }
        release = true;
        holder.join();
        map.put(key, "v");
        if ("v".equals(map.get(key))) {
            result |= 4;
        }
        synchronized (key) {
            result |= 8;
        }
    }
}
