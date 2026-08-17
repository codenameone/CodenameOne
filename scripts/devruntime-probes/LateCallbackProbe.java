import com.codename1.ui.*;

/**
 * A callback that arrives well after the program started, doing enough work to
 * reach a watchdog checkpoint.
 *
 * This is what every button press in a real application looks like: the program
 * ran once, and the framework calls back into it minutes later. The EDT budget
 * must apply to that one callback, not to the age of the session.
 */
public class LateCallbackProbe {
    public static void main(String[] a) {
        new Form("LateCallback").show();
        System.out.println("PROBE LateCallbackProbe: started, callback scheduled");
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(5000); } catch (InterruptedException e) { }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        long n = 0;
                        for (int i = 0; i < 3000000; i++) { n += i; }
                        System.out.println("PROBE LateCallbackProbe: late callback ran, n=" + n);
                    }
                });
            }
        }).start();
    }
}
