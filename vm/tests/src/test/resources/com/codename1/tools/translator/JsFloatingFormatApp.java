import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class JsFloatingFormatApp {
    public static int result;

    private static class AlternatePrintStream extends PrintStream {
        AlternatePrintStream(ByteArrayOutputStream out) {
            super(out);
        }

        @Override
        public void print(String value) {
            super.print(value);
        }
    }

    private static void printValues(PrintStream print) {
        print.print(12500000.0d);
        print.print('|');
        print.print(12500000.0f);
    }

    public static void main(String[] args) {
        int score = 0;
        if ("1.0E7".equals(Double.toString(10000000.0d))) {
            score |= 1;
        }
        if ("1.25E7".equals(Double.toString(12500000.0d))) {
            score |= 2;
        }
        if ("1.25E8".equals(Double.toString(125000000.0d))) {
            score |= 4;
        }
        if ("1.0E7".equals(Float.toString(10000000.0f))) {
            score |= 8;
        }
        if ("1.25E7".equals(Float.toString(12500000.0f))) {
            score |= 16;
        }
        if ("1.6777216E7".equals(Float.toString(16777216.0f))) {
            score |= 32;
        }
        if ("balance=1.25E7".equals("balance=" + 12500000.0d)) {
            score |= 64;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream print = new PrintStream(out);
        // Keep more than one print(String) implementation reachable so this
        // exercises runtime virtual dispatch and the native-override path.
        // A monomorphic fixture bypasses that path through devirtualization.
        if (args.length < 0) {
            print = new AlternatePrintStream(out);
        }
        printValues(print);
        if ("1.25E7|1.25E7".equals(out.toString())) {
            score |= 128;
        }
        result = score;
    }
}
