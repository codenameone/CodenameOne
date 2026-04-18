class JsSuperInvokeInCtorBase {
    int children;

    void add(int value) {
        children = children * 10 + value;
    }

    int getChildren() {
        return children;
    }
}

public class JsSuperInvokeInCtorApp extends JsSuperInvokeInCtorBase {
    static int result;
    int delegated;

    JsSuperInvokeInCtorApp() {
        addToForm(1);
        addToForm(2);
        add(9);
    }

    final void addToForm(int value) {
        super.add(value);
    }

    @Override
    void add(int value) {
        delegated = delegated * 10 + value;
    }

    public static void main(String[] args) {
        JsSuperInvokeInCtorApp app = new JsSuperInvokeInCtorApp();
        result = app.getChildren() * 100 + app.delegated;
        System.exit(result);
    }
}
