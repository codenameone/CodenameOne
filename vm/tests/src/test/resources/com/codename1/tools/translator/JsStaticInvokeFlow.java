public class JsStaticInvokeFlow {
    static int base = 5;

    // Not a bare "return base;": a trivial getter is folded into a field read
    // at the call site, and this fixture is about the call.
    static int helper() {
        return base + 1;
    }

    static int pick(int delta) {
        int result = helper();
        if (delta > 0) {
            result += helper();
        }
        return result;
    }

    public static void main(String[] args) {
        System.exit(pick(1));
    }
}
