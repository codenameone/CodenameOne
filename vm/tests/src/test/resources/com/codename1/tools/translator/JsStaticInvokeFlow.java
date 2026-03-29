public class JsStaticInvokeFlow {
    static int base = 5;

    static int helper() {
        return base;
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
