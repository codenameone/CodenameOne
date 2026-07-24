package com.codename1.flutter.intl;

/**
 * A subset of {@code package:intl}'s NumberFormat covering the currency and
 * percent factory constructors used by the gallery. Formatting is a simple
 * fixed-decimal render (no locale grouping); faithful locale output is a
 * later pass.
 */
public final class NumberFormat {

    private final String prefix;
    private final String suffix;
    private final int digits;
    private final boolean percent;

    private NumberFormat(String prefix, String suffix, int digits, boolean percent) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.digits = digits;
        this.percent = percent;
    }

    public static NumberFormat currency(String locale, String symbol, long decimalDigits, String name) {
        return new NumberFormat(symbol != null ? symbol : "$", "",
                decimalDigits > 0 ? (int) decimalDigits : 2, false);
    }

    public static NumberFormat simpleCurrency(String locale, String name, long decimalDigits) {
        return new NumberFormat("$", "", decimalDigits > 0 ? (int) decimalDigits : 2, false);
    }

    public static NumberFormat decimalPercentPattern(String locale, long decimalDigits) {
        return new NumberFormat("", "%", decimalDigits >= 0 ? (int) decimalDigits : 0, true);
    }

    public String format(Object number) {
        double v = number instanceof Number ? ((Number) number).doubleValue() : 0;
        if (percent) {
            v = v * 100;
        }
        return prefix + fixed(v, digits) + suffix;
    }

    private static String fixed(double value, int digits) {
        boolean neg = value < 0;
        double v = neg ? -value : value;
        long factor = 1;
        for (int i = 0; i < digits; i++) {
            factor *= 10;
        }
        long scaled = Math.round(v * factor);
        long intPart = scaled / factor;
        long fracPart = scaled % factor;
        StringBuilder sb = new StringBuilder();
        if (neg) {
            sb.append('-');
        }
        sb.append(intPart);
        if (digits > 0) {
            sb.append('.');
            String f = Long.toString(fracPart);
            for (int i = f.length(); i < digits; i++) {
                sb.append('0');
            }
            sb.append(f);
        }
        return sb.toString();
    }
}
