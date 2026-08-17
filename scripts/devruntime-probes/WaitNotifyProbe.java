import com.codename1.ui.*;

/**
 * Producer/consumer across two threads, through wait/notify.
 *
 * This is the case that decides how monitors are implemented. A private lock
 * table keyed by object identity -- a ReentrantLock per object -- gives mutual
 * exclusion and nothing else: wait() requires the caller to own that object's
 * monitor, and would throw IllegalMonitorStateException here. Using the
 * object's own monitor costs a nested interpreter frame per guarded region and
 * buys this, plus exclusion against framework code locking the same object.
 */
public class WaitNotifyProbe {
    static final Object LOCK = new Object();
    static int value = -1;
    static boolean ready;

    public static void main(String[] a) throws Exception {
        Thread consumer = new Thread(new Runnable() {
            public void run() {
                synchronized (LOCK) {
                    while (!ready) {
                        try { LOCK.wait(); } catch (InterruptedException e) { }
                    }
                    System.out.println("PROBE WaitNotifyProbe: consumer got " + value);
                }
            }
        });
        Thread producer = new Thread(new Runnable() {
            public void run() {
                synchronized (LOCK) { value = 42; ready = true; LOCK.notifyAll(); }
            }
        });
        consumer.start();
        Thread.sleep(100);
        producer.start();
        consumer.join();
        producer.join();
        System.out.println("PROBE WaitNotifyProbe: done ready=" + ready);
        new Form("WaitNotify").show();
    }
}
