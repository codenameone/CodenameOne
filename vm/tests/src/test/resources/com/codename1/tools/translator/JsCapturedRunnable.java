import java.util.ArrayList;

public class JsCapturedRunnable {
    private final ArrayList values = new ArrayList();
    private final ArrayList ops = new ArrayList();

    void add(final Object value) {
        ops.add(new Runnable() {
            public void run() {
                values.add(value);
            }
        });
    }

    public static void main(String[] args) {
        new JsCapturedRunnable().add("ok");
        System.exit(0);
    }
}
