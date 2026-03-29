class JsInheritedFieldAccessBase {
    Object lock;

    JsInheritedFieldAccessBase() {
        lock = this;
    }
}

public class JsInheritedFieldAccessApp extends JsInheritedFieldAccessBase {
    int runOnce() {
        synchronized (lock) {
            return 7;
        }
    }

    public static void main(String[] args) {
        System.exit(new JsInheritedFieldAccessApp().runOnce());
    }
}
