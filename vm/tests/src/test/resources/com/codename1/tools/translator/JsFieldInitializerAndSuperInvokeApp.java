class JsFieldInitializerAndSuperInvokeBase {
    final StringBuilder direct = new StringBuilder();

    void add(String value) {
        direct.append(value);
    }

    String directValue() {
        return direct.toString();
    }
}

public class JsFieldInitializerAndSuperInvokeApp extends JsFieldInitializerAndSuperInvokeBase {
    static int result;
    final StringBuilder delegated = new StringBuilder();

    JsFieldInitializerAndSuperInvokeApp() {
        addDirect("A");
        addDirect("B");
        add("Z");
    }

    final void addDirect(String value) {
        super.add(value);
    }

    @Override
    void add(String value) {
        delegated.append(value);
    }

    public static void main(String[] args) {
        JsFieldInitializerAndSuperInvokeApp app = new JsFieldInitializerAndSuperInvokeApp();
        int mask = 0;
        mask += "AB".equals(app.directValue()) ? 100 : 0;
        mask += "Z".equals(app.delegated.toString()) ? 11 : 0;
        result = mask;
        System.exit(result);
    }
}
