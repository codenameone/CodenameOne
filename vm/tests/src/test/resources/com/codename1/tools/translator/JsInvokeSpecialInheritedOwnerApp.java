class JsInvokeSpecialGrandBase {
    int direct;

    void record(int value) {
        direct = direct * 10 + value;
    }

    int getDirect() {
        return direct;
    }
}

class JsInvokeSpecialMidBase extends JsInvokeSpecialGrandBase {
}

public class JsInvokeSpecialInheritedOwnerApp extends JsInvokeSpecialMidBase {
    static int result;
    int delegated;

    void callSuperRecord(int value) {
        super.record(value);
    }

    @Override
    void record(int value) {
        delegated = delegated * 10 + value;
    }

    public static void main(String[] args) {
        JsInvokeSpecialInheritedOwnerApp app = new JsInvokeSpecialInheritedOwnerApp();
        app.callSuperRecord(1);
        app.record(1);
        result = app.getDirect() * 100 + app.delegated * 10;
        System.exit(result);
    }
}
