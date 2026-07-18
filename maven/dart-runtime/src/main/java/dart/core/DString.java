package dart.core;

/**
 * Static helpers implementing Dart's String API over java.lang.String
 * (used directly — no wrapper allocation). The transpiler maps Dart String
 * members that have no direct java.lang.String equivalent to these.
 */
public final class DString {

    private DString() {
    }

    public static long length(String s) {
        return s.length();
    }

    public static boolean isEmpty(String s) {
        return s.isEmpty();
    }

    public static boolean isNotEmpty(String s) {
        return !s.isEmpty();
    }

    /** Dart's s[i] — single-character string. */
    public static String idx(String s, long index) {
        RangeError.checkValidIndex(index, s.length());
        return String.valueOf(s.charAt((int) index));
    }

    public static long codeUnitAt(String s, long index) {
        RangeError.checkValidIndex(index, s.length());
        return s.charAt((int) index);
    }

    /** Dart's s * n operator — repeat. */
    public static String repeat(String s, long times) {
        if (times <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (long i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String substring(String s, long start) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        return s.substring((int) start);
    }

    public static String substring(String s, long start, long end) {
        RangeError.checkValueInInterval(start, 0, s.length(), "start");
        RangeError.checkValueInInterval(end, start, s.length(), "end");
        return s.substring((int) start, (int) end);
    }

    public static String padLeft(String s, long width, String padding) {
        StringBuilder sb = new StringBuilder();
        for (long i = s.length(); i < width; i++) {
            sb.append(padding);
        }
        return sb.append(s).toString();
    }

    public static String padLeft(String s, long width) {
        return padLeft(s, width, " ");
    }

    public static String padRight(String s, long width, String padding) {
        StringBuilder sb = new StringBuilder(s);
        for (long i = s.length(); i < width; i++) {
            sb.append(padding);
        }
        return sb.toString();
    }

    public static String padRight(String s, long width) {
        return padRight(s, width, " ");
    }

    public static DartList<String> split(String s, String pattern) {
        DartList<String> out = new DartList<>();
        if (pattern.isEmpty()) {
            for (int i = 0; i < s.length(); i++) {
                out.add(String.valueOf(s.charAt(i)));
            }
            return out;
        }
        int start = 0;
        int i;
        while ((i = s.indexOf(pattern, start)) >= 0) {
            out.add(s.substring(start, i));
            start = i + pattern.length();
        }
        out.add(s.substring(start));
        return out;
    }

    public static long indexOf(String s, String other) {
        return s.indexOf(other);
    }

    public static long indexOf(String s, String other, long start) {
        return s.indexOf(other, (int) start);
    }

    public static long lastIndexOf(String s, String other) {
        return s.lastIndexOf(other);
    }

    public static boolean contains(String s, String other) {
        return s.contains(other);
    }

    public static String replaceAll(String s, String from, String to) {
        // Dart's replaceAll on a String pattern is literal, like Java's replace.
        return s.replace(from, to);
    }

    public static String replaceFirst(String s, String from, String to) {
        int i = s.indexOf(from);
        if (i < 0) {
            return s;
        }
        return s.substring(0, i) + to + s.substring(i + from.length());
    }

    /** Dart's int.parse. */
    public static long parseInt(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            throw new dart.core.FormatException("Invalid radix-10 number: " + s);
        }
    }

    /** Dart's int.tryParse. */
    public static Long tryParseInt(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Dart's double.parse. */
    public static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new dart.core.FormatException("Invalid double: " + s);
        }
    }

    /** Dart's double.tryParse. */
    public static Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long compareTo(String a, String b) {
        int r = a.compareTo(b);
        return r < 0 ? -1 : (r > 0 ? 1 : 0);
    }
}
