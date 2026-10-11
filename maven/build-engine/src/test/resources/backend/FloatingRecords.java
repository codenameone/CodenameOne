// Shared JVM/native regression fixture. Compile with --release 17 or newer.
package example;

public class FloatingRecords {
    record Floats(float value) {
        private static int cn1$recordFloatBits(float value) { return 42; }
        private static int cn1$recordFloatBits1(float value) { return 43; }
    }
    record Doubles(double value) {
        private static long cn1$recordDoubleBits(double value) { return 42; }
    }
    record Mixed(boolean enabled, float first, double second, int last) {}
    record Custom(float value) {
        public boolean equals(Object other) { return false; }
        public int hashCode() { return 99; }
    }

    public static void verify() {
        int[] floats = {0x7fc00000, 0x7fc00001, 0x7fffffff, 0xffc00001,
                0, 0x80000000, 0x7f800000, 0xff800000, 0x3fc00000, 0xbfc00000};
        long[] doubles = {0x7ff8000000000000L, 0x7ff8000000000001L,
                0x7fffffffffffffffL, 0xfff8000000000001L, 0L, 0x8000000000000000L,
                0x7ff0000000000000L, 0xfff0000000000000L, 0x3ff8000000000000L, 0xbff8000000000000L};
        for (int i = 0; i < floats.length; i++) {
            Floats f = new Floats(Float.intBitsToFloat(floats[i]));
            Doubles d = new Doubles(Double.longBitsToDouble(doubles[i]));
            int fbits = i < 4 ? 0x7fc00000 : floats[i];
            long dbits = i < 4 ? 0x7ff8000000000000L : doubles[i];
            int dhash = (int) (dbits ^ (dbits >>> 32));
            require(f.hashCode() == fbits, "float hash " + i);
            require(d.hashCode() == dhash, "double hash " + i);
            require(new Mixed(true, f.value(), d.value(), 7).hashCode()
                    == ((1231 * 31 + fbits) * 31 + dhash) * 31 + 7, "mixed hash " + i);
            for (int j = 0; j < floats.length; j++) {
                boolean same = i == j || i < 4 && j < 4;
                require(f.equals(new Floats(Float.intBitsToFloat(floats[j]))) == same, "float equality");
                require(d.equals(new Doubles(Double.longBitsToDouble(doubles[j]))) == same, "double equality");
            }
        }
        require(Floats.cn1$recordFloatBits(0) == 42 && Floats.cn1$recordFloatBits1(0) == 43
                && Doubles.cn1$recordDoubleBits(0) == 42, "user methods");
        Custom custom = new Custom(Float.NaN);
        require(custom.hashCode() == 99 && !custom.equals(custom), "explicit methods");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
