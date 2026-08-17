import com.codename1.ui.*;
public class ThreadProbe {
    static final StringBuilder r = new StringBuilder();
    static synchronized void add(String s) { r.append(s); }
    public static void main(String[] a) throws Exception {
        Thread t = new Thread(new Runnable() {
            public void run() { add("worker "); }
        });
        t.start();
        t.join();
        Object lock = new Object();
        synchronized (lock) { add("sync "); }
        System.out.println("PROBE ThreadProbe: " + r);
        new Form("Thread").show();
    }
}
