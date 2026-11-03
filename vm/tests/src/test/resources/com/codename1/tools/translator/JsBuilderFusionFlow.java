public class JsBuilderFusionFlow {
    static int sink;

    // Never escapes: fused into a JS string box.
    static String fused(int n) {
        StringBuilder b = new StringBuilder("n=");
        b.append(n).append(',').append(n > 0);
        return b.toString() + b.length();
    }

    // Passed to another method: must stay a real StringBuilder.
    static String passed(int n) {
        StringBuilder b = new StringBuilder();
        consume(b);
        return b.append(n).toString();
    }

    // Live into a handler that hands it on: must stay a real StringBuilder.
    static String handled(int n) {
        StringBuilder b = new StringBuilder();
        try {
            b.append(10 / n);
        } catch (ArithmeticException e) {
            consume(b);
        }
        return b.toString();
    }

    static void consume(StringBuilder b) {
        sink += b.length();
    }

    public static void main(String[] args) {
        System.exit(fused(1).length() + passed(2).length() + handled(0).length());
    }
}
