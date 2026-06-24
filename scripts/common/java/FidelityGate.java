import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// The native-fidelity ratchet gate. Given a ProcessScreenshots "--mode fidelity"
/// comparison JSON and a stored baseline, it fails (exit 20) when any component's
/// measured fidelity has DROPPED below its recorded baseline (minus an epsilon),
/// or when a pair could not be compared (missing render, missing golden, size
/// mismatch, error). Being below 100% never fails on its own -- the suite is a
/// one-way ratchet that can only improve.
///
/// With --update-baseline it instead WRITES a fresh baseline from the current
/// measurements (merged over any existing one) and exits 0 without gating. This
/// is the deliberate, loud act invoked via FIDELITY_UPDATE_BASELINE=1.
///
/// Baseline format: { "pairs": { "<test>": <fidelity_percent>, ... } }
public class FidelityGate {
    private static final int EXIT_USAGE = 2;
    private static final int EXIT_IO = 1;
    private static final int EXIT_REGRESSION = 20;

    public static void main(String[] args) {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            System.exit(EXIT_USAGE);
            return;
        }
        Map<String, Object> data;
        try {
            String text = Files.readString(arguments.compareJson, StandardCharsets.UTF_8);
            data = JsonUtil.asObject(JsonUtil.parse(text));
        } catch (IOException ex) {
            System.err.println("FidelityGate: cannot read comparison JSON " + arguments.compareJson + ": " + ex.getMessage());
            System.exit(EXIT_IO);
            return;
        }
        Map<String, Double> current = new LinkedHashMap<>();
        List<String> broken = new ArrayList<>();
        for (Object item : JsonUtil.asArray(data.get("results"))) {
            Map<String, Object> result = JsonUtil.asObject(item);
            String test = stringValue(result.get("test"), "unknown");
            String status = stringValue(result.get("status"), "unknown");
            if ("compared".equals(status)) {
                Double fidelity = toDouble(JsonUtil.asObject(result.get("details")).get("fidelity_percent"));
                if (fidelity != null) {
                    current.put(test, fidelity);
                }
            } else {
                broken.add(test + " (" + status + ")");
            }
        }

        if (arguments.updateBaseline != null) {
            updateBaseline(arguments, current);
            return;
        }

        Map<String, Double> baseline = loadBaseline(arguments.baselineJson);
        List<String> regressions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : current.entrySet()) {
            Double base = baseline.get(entry.getKey());
            if (base == null) {
                System.out.println("[gate] new pair (no baseline yet): " + entry.getKey()
                        + " = " + String.format("%.2f%%", entry.getValue()));
                continue;
            }
            double drop = base - entry.getValue();
            if (drop > arguments.epsilon) {
                regressions.add(String.format("%s: %.2f%% -> %.2f%% (dropped %.2f, epsilon %.2f)",
                        entry.getKey(), base, entry.getValue(), drop, arguments.epsilon));
            }
        }

        boolean failed = !regressions.isEmpty() || !broken.isEmpty();
        if (!regressions.isEmpty()) {
            System.err.println("[gate] FAIL: " + regressions.size() + " fidelity regression(s) below baseline:");
            for (String r : regressions) {
                System.err.println("  - " + r);
            }
        }
        if (!broken.isEmpty()) {
            System.err.println("[gate] FAIL: " + broken.size() + " pair(s) could not be compared:");
            for (String b : broken) {
                System.err.println("  - " + b);
            }
        }
        if (failed) {
            System.err.println("[gate] To accept a deliberate change, regenerate goldens (FIDELITY_UPDATE_GOLDENS=1) "
                    + "and/or update the baseline (FIDELITY_UPDATE_BASELINE=1).");
            System.exit(EXIT_REGRESSION);
            return;
        }
        System.out.println("[gate] OK: " + current.size() + " pair(s) at or above baseline (epsilon "
                + String.format("%.2f", arguments.epsilon) + ").");
    }

    private static void updateBaseline(Arguments arguments, Map<String, Double> current) {
        // Merge current measurements over any existing baseline so a partial run
        // does not drop entries it did not exercise. Loud by design.
        Map<String, Double> merged = new TreeMap<>(loadBaseline(arguments.baselineJson));
        merged.putAll(current);
        Map<String, Object> pairs = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : merged.entrySet()) {
            pairs.put(entry.getKey(), round2(entry.getValue()));
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("pairs", pairs);
        try {
            Path out = arguments.updateBaseline;
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            Files.writeString(out, JsonUtil.stringify(root) + "\n", StandardCharsets.UTF_8);
            System.out.println("[gate] WARNING: baseline UPDATED at " + out + " with " + merged.size()
                    + " pair(s). This bypasses the regression gate and must be reviewed in the PR.");
        } catch (IOException ex) {
            System.err.println("FidelityGate: cannot write baseline " + arguments.updateBaseline + ": " + ex.getMessage());
            System.exit(EXIT_IO);
        }
    }

    private static Map<String, Double> loadBaseline(Path baselinePath) {
        Map<String, Double> baseline = new LinkedHashMap<>();
        if (baselinePath == null || !Files.isRegularFile(baselinePath)) {
            return baseline;
        }
        try {
            String text = Files.readString(baselinePath, StandardCharsets.UTF_8);
            Map<String, Object> pairs = JsonUtil.asObject(JsonUtil.asObject(JsonUtil.parse(text)).get("pairs"));
            for (Map.Entry<String, Object> entry : pairs.entrySet()) {
                Double value = toDouble(entry.getValue());
                if (value != null) {
                    baseline.put(entry.getKey(), value);
                }
            }
        } catch (IOException ex) {
            System.err.println("Warning: could not read baseline " + baselinePath + ": " + ex.getMessage());
        }
        return baseline;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof String s) {
            return s;
        }
        return value.toString();
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static final class Arguments {
        final Path compareJson;
        final Path baselineJson;
        final Path updateBaseline;
        final double epsilon;

        private Arguments(Path compareJson, Path baselineJson, Path updateBaseline, double epsilon) {
            this.compareJson = compareJson;
            this.baselineJson = baselineJson;
            this.updateBaseline = updateBaseline;
            this.epsilon = epsilon;
        }

        static Arguments parse(String[] args) {
            Path compare = null;
            Path baseline = null;
            Path update = null;
            double epsilon = 0.5d;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--compare-json" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --compare-json");
                            return null;
                        }
                        compare = Path.of(args[i]);
                    }
                    case "--baseline" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --baseline");
                            return null;
                        }
                        baseline = Path.of(args[i]);
                    }
                    case "--update-baseline" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --update-baseline");
                            return null;
                        }
                        update = Path.of(args[i]);
                    }
                    case "--epsilon" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --epsilon");
                            return null;
                        }
                        try {
                            epsilon = Double.parseDouble(args[i]);
                        } catch (NumberFormatException ex) {
                            System.err.println("Invalid value for --epsilon: " + args[i]);
                            return null;
                        }
                    }
                    default -> {
                        System.err.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }
            if (compare == null) {
                System.err.println("--compare-json is required");
                return null;
            }
            return new Arguments(compare, baseline, update, epsilon);
        }
    }
}

class JsonUtil {
    private JsonUtil() {}

    public static Object parse(String text) {
        return new Parser(text).parseValue();
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof String s) {
                    result.put(s, entry.getValue());
                }
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>((List<Object>) list);
        }
        return new ArrayList<>();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String sKey)) {
                    continue;
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, sKey);
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                index++;
                Object value = parseValue();
                result.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
                index++;
            }
            return result;
        }

        private List<Object> parseArray() {
            index++;
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                Object value = parseValue();
                result.add(value);
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
                index++;
            }
            return result;
        }

        private String parseString() {
            expect('"');
            index++;
            StringBuilder sb = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    if (index >= text.length()) {
                        throw new IllegalArgumentException("Invalid escape sequence");
                    }
                    char esc = text.charAt(index++);
                    sb.append(switch (esc) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> parseUnicode();
                        default -> throw new IllegalArgumentException("Invalid escape character: " + esc);
                    });
                } else {
                    sb.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw new IllegalArgumentException("Incomplete unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                char ch = text.charAt(index++);
                int digit = Character.digit(ch, 16);
                if (digit < 0) {
                    throw new IllegalArgumentException("Invalid hex digit in unicode escape");
                }
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected '" + literal + "'");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (peek('0')) {
                index++;
            } else {
                if (!Character.isDigit(peekChar())) {
                    throw new IllegalArgumentException("Invalid number");
                }
                while (Character.isDigit(peekChar())) {
                    index++;
                }
            }
            boolean isFloat = false;
            if (peek('.')) {
                isFloat = true;
                index++;
                if (!Character.isDigit(peekChar())) {
                    throw new IllegalArgumentException("Invalid fractional number");
                }
                while (Character.isDigit(peekChar())) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                isFloat = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                if (!Character.isDigit(peekChar())) {
                    throw new IllegalArgumentException("Invalid exponent");
                }
                while (Character.isDigit(peekChar())) {
                    index++;
                }
            }
            String number = text.substring(start, index);
            try {
                if (!isFloat) {
                    long value = Long.parseLong(number);
                    if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                        return (int) value;
                    }
                    return value;
                }
                return Double.parseDouble(number);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + number, ex);
            }
        }

        private void expect(char ch) {
            if (!peek(ch)) {
                throw new IllegalArgumentException("Expected '" + ch + "'");
            }
        }

        private boolean peek(char ch) {
            return index < text.length() && text.charAt(index) == ch;
        }

        private char peekChar() {
            return index < text.length() ? text.charAt(index) : '\0';
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }
    }
}
