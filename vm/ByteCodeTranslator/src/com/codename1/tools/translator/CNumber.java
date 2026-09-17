package com.codename1.tools.translator;

/** Exact C99 literals, independent of the host VM's decimal rendering algorithm. */
public final class CNumber {
    private CNumber() { }

    public static String literal(float value) {
        if (Float.isNaN(value)) return "(0.0f/0.0f)";
        if (Float.isInfinite(value)) return value < 0 ? "(-1.0f/0.0f)" : "(1.0f/0.0f)";
        int bits = Float.floatToRawIntBits(value);
        int exponent = (bits >>> 23) & 255;
        int significand = bits & 0x7fffff;
        if (exponent != 0) significand |= 0x800000;
        return (bits < 0 ? "-0x" : "0x") + Integer.toHexString(significand)
                + "p" + (exponent == 0 ? -149 : exponent - 150) + "f";
    }

    public static String literal(double value) {
        if (Double.isNaN(value)) return "(0.0/0.0)";
        if (Double.isInfinite(value)) return value < 0 ? "(-1.0/0.0)" : "(1.0/0.0)";
        long bits = Double.doubleToRawLongBits(value);
        int exponent = (int) ((bits >>> 52) & 2047);
        long significand = bits & 0xfffffffffffffL;
        if (exponent != 0) significand |= 0x10000000000000L;
        return (bits < 0 ? "-0x" : "0x") + Long.toString(significand, 16)
                + "p" + (exponent == 0 ? -1074 : exponent - 1075);
    }
}
