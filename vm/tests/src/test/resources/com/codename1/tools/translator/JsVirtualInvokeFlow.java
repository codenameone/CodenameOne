class JsVirtualInvokeBase {
    int value() {
        return 3;
    }
}

class JsVirtualInvokeImpl extends JsVirtualInvokeBase {
    int value() {
        return 5;
    }
}

public class JsVirtualInvokeFlow {
    static int repeat(JsVirtualInvokeBase base, int delta) {
        int result = base.value();
        if (delta > 0) {
            result += base.value();
        }
        return result;
    }

    public static void main(String[] args) {
        System.exit(repeat(new JsVirtualInvokeImpl(), 1));
    }
}
