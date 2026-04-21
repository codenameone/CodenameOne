public class JsDoubleParseApp {
    public static int result;

    public static void main(String[] args) {
        int mask = 0;
        // Each bit tracks one parseDouble case. If parseDblImpl ignores the
        // exponent argument (the root-cause bug for huge Switch pills), the
        // whole-number-equivalent of each decimal string will be returned
        // instead, and every bit below will be unset.
        if (approx(Double.parseDouble("1.4"), 1.4)) mask |= 1;
        if (approx(Double.parseDouble("2.5"), 2.5)) mask |= 2;
        if (approx(Double.parseDouble("1.5"), 1.5)) mask |= 4;
        if (approx(Double.parseDouble("10.9"), 10.9)) mask |= 8;
        if (approx(Double.parseDouble("0.25"), 0.25)) mask |= 16;
        if (approx(Double.parseDouble(".5"), 0.5)) mask |= 32;
        if (approx(Double.parseDouble("1.5e1"), 15.0)) mask |= 64;
        if (approx(Double.parseDouble("0"), 0.0)) mask |= 128;
        if (approx(Double.parseDouble("-1.4"), -1.4)) mask |= 256;
        result = mask;
        System.exit(mask);
    }

    private static boolean approx(double actual, double expected) {
        double diff = actual - expected;
        if (diff < 0) diff = -diff;
        return diff < 1e-9;
    }
}
