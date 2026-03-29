public class JsStaticAccessFlow {
    static int value = 7;

    static int pick(int delta) {
        int result = value;
        if (delta > 0) {
            result += value;
        }
        return result;
    }

    public static void main(String[] args) {
        System.exit(pick(1));
    }
}
