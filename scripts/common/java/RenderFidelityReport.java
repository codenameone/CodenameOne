import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Renders the native-fidelity report from a ProcessScreenshots "--mode fidelity"
/// comparison JSON. Unlike RenderScreenshotReport (which describes pixel-equality
/// pass/fail against a stored CN1 golden), this renders a SIMILARITY report: how
/// close the CN1 render of each component is to the real native OS widget, as a
/// percentage, with the native and CN1 previews shown side by side, the change in
/// fidelity vs the recorded baseline, and a sorted "lowest fidelity" backlog so
/// theme authors can see what still needs work.
///
/// The golden report is intentionally left untouched; this is a sibling tool.
public class RenderFidelityReport {
    private static final String DEFAULT_MARKER = "<!-- CN1SS_FIDELITY_COMMENT -->";
    private static final String DEFAULT_TITLE = "Native fidelity report";
    // Aspirational (non-blocking) bar: pairs below this are flagged in the
    // backlog as still needing theme work. The hard regression gate lives in
    // FidelityGate; this threshold never fails the build on its own.
    private static final double ASPIRATIONAL_THRESHOLD = 99.0d;

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            System.exit(2);
            return;
        }
        if (!Files.isRegularFile(arguments.compareJson)) {
            System.err.println("Comparison JSON not found: " + arguments.compareJson);
            System.exit(1);
        }
        String text = Files.readString(arguments.compareJson, StandardCharsets.UTF_8);
        Map<String, Object> data = JsonUtil.asObject(JsonUtil.parse(text));
        Map<String, Double> baseline = loadBaseline(arguments.baselineJson);
        String marker = arguments.marker != null ? arguments.marker : DEFAULT_MARKER;
        String title = arguments.title != null ? arguments.title : DEFAULT_TITLE;

        Report report = buildReport(data, baseline, title, marker, arguments.aspirational);
        writeLines(arguments.summaryOut, report.summaryLines);
        writeLines(arguments.commentOut, report.commentLines);
    }

    private static Report buildReport(Map<String, Object> data, Map<String, Double> baseline,
            String title, String marker, double aspirational) {
        List<String> summaryLines = new ArrayList<>();
        List<String> commentLines = new ArrayList<>();
        List<Object> results = JsonUtil.asArray(data.get("results"));

        List<PairRow> rows = new ArrayList<>();
        int compared = 0;
        int missing = 0;
        int errors = 0;
        double fidelitySum = 0.0d;
        for (Object item : results) {
            Map<String, Object> result = JsonUtil.asObject(item);
            String test = stringValue(result.get("test"), "unknown");
            String status = stringValue(result.get("status"), "unknown");
            Map<String, Object> details = JsonUtil.asObject(result.get("details"));
            Double fidelity = toDouble(details.get("fidelity_percent"));
            Double ssim = toDouble(details.get("ssim"));
            Double meanDelta = toDouble(details.get("mean_channel_delta"));
            Double base = baseline.get(test);
            Double delta = (fidelity != null && base != null) ? (fidelity - base) : null;

            PairRow row = new PairRow(test, status, fidelity, ssim, meanDelta, base, delta,
                    stringValue(result.get("native_path"), ""),
                    stringValue(result.get("cn1_path"), ""),
                    JsonUtil.asObject(result.get("preview")),
                    JsonUtil.asObject(result.get("native_preview")),
                    stringValue(result.get("message"), ""));
            rows.add(row);

            String message;
            switch (status) {
                case "compared" -> {
                    compared++;
                    if (fidelity != null) {
                        fidelitySum += fidelity;
                    }
                    message = String.format("Fidelity %.2f%% (SSIM %.4f, mean delta %.2f)%s",
                            nz(fidelity), nz(ssim), nz(meanDelta), deltaSuffix(delta));
                }
                case "missing_actual" -> {
                    missing++;
                    message = "CN1 render not delivered.";
                }
                case "missing_expected" -> {
                    missing++;
                    message = "Native golden missing (regenerate with FIDELITY_UPDATE_GOLDENS=1).";
                }
                case "size_mismatch" -> {
                    errors++;
                    message = stringValue(result.get("message"), "CN1 tile size differs from native golden.");
                }
                case "error" -> {
                    errors++;
                    message = "Comparison error: " + stringValue(result.get("message"), "unknown error");
                }
                default -> message = "Status: " + status + ".";
            }
            // Pipe-delimited summary consumed by cn1ss.sh (status|test|message|
            // copyFlag|cn1Path|fidelity). copyFlag is always 1 so the CN1 render
            // is archived as an artifact regardless of score.
            summaryLines.add(String.join("|", List.of(
                    status, test, message, "1",
                    stringValue(result.get("cn1_path"), ""),
                    fidelity != null ? String.format("%.2f", fidelity) : "")));
        }

        double meanFidelity = compared > 0 ? fidelitySum / compared : 0.0d;

        // Compared pairs sorted ascending (worst first) -- the basis for the
        // distribution statistics, the per-pair percentage table and the cards.
        List<PairRow> comparedRows = new ArrayList<>();
        for (PairRow row : rows) {
            if ("compared".equals(row.status) && row.fidelity != null) {
                comparedRows.add(row);
            }
        }
        comparedRows.sort(Comparator.comparingDouble(r -> r.fidelity));

        if (title != null && !title.isEmpty()) {
            commentLines.add("### " + title);
            commentLines.add("");
        }

        if (compared > 0) {
            double median = percentile(comparedRows, 50);
            double p25 = percentile(comparedRows, 25);
            PairRow worst = comparedRows.get(0);
            // Distribution, not just the mean: a single average hides the low
            // points, so report where the pairs actually land.
            int b99 = 0, b95 = 0, b90 = 0, bLow = 0;
            for (PairRow row : comparedRows) {
                double f = row.fidelity;
                if (f >= 99.0d) {
                    b99++;
                } else if (f >= 95.0d) {
                    b95++;
                } else if (f >= 90.0d) {
                    b90++;
                } else {
                    bLow++;
                }
            }
            commentLines.add(String.format(
                    "**%d pairs compared** -- median **%.1f%%**, worst **%.1f%%** (`%s`), 25th pct %.1f%%, mean %.1f%%.",
                    compared, median, worst.fidelity, worst.test, p25, meanFidelity));
            commentLines.add("");
            commentLines.add(String.format(
                    "Distribution -- `>=99%%`: **%d** | `95-99%%`: **%d** | `90-95%%`: **%d** | `<90%%`: **%d**%s%s",
                    b99, b95, b90, bLow,
                    missing > 0 ? String.format(" | %d not delivered/missing golden", missing) : "",
                    errors > 0 ? String.format(" | %d error(s)", errors) : ""));
        } else {
            commentLines.add(String.format("**No pairs could be compared.**%s%s",
                    missing > 0 ? " " + missing + " not delivered/missing golden." : "",
                    errors > 0 ? " " + errors + " error(s)." : ""));
        }
        commentLines.add("");

        // Per-pair fidelity table (worst first): the percentage data for every
        // mismatch, at a glance, without scrolling through the image cards.
        if (compared > 0) {
            commentLines.add("| Component | State | Appearance | Fidelity | SSIM | mean delta | vs base |");
            commentLines.add("|---|---|---|--:|--:|--:|--:|");
            for (PairRow row : comparedRows) {
                String[] p = splitTest(row.test);
                commentLines.add(String.format("| %s | %s | %s | %.1f%% | %.3f | %.2f | %s |",
                        p[0], p[1], p[2], row.fidelity, nz(row.ssim), nz(row.meanDelta), deltaCell(row.delta)));
            }
            commentLines.add("");
        }

        // Non-compared pairs (errors / not delivered / missing golden) listed
        // explicitly so they are never silently dropped from the percentages.
        List<PairRow> problemRows = new ArrayList<>();
        for (PairRow row : rows) {
            if (!"compared".equals(row.status)) {
                problemRows.add(row);
            }
        }
        if (!problemRows.isEmpty()) {
            commentLines.add(String.format("**%d pair(s) not scored:**", problemRows.size()));
            for (PairRow row : problemRows) {
                commentLines.add(String.format("- `%s` -- %s%s", row.test, row.status,
                        row.message != null && !row.message.isEmpty() ? " (" + row.message + ")" : ""));
            }
            commentLines.add("");
        }

        // Side-by-side comparison cards, worst first, then the unscored pairs.
        commentLines.add("#### Side-by-side comparisons (worst first)");
        commentLines.add("");
        List<PairRow> cardRows = new ArrayList<>(comparedRows);
        cardRows.addAll(problemRows);
        for (PairRow row : cardRows) {
            commentLines.add(detailHeadline(row));
            addPreviewPair(commentLines, row);
            commentLines.add("");
        }

        if (marker != null && !marker.isEmpty()) {
            commentLines.add(marker);
        }
        return new Report(summaryLines, commentLines);
    }

    private static int rowPriority(PairRow row) {
        if (row.delta != null && row.delta < 0) {
            return 0; // regressions first
        }
        switch (row.status) {
            case "size_mismatch":
            case "error":
            case "missing_actual":
            case "missing_expected":
                return 1;
            default:
                return 2;
        }
    }

    private static String detailHeadline(PairRow row) {
        switch (row.status) {
            case "compared":
                return String.format("- **%s** -- %.2f%% fidelity (SSIM %.4f)%s",
                        row.test, nz(row.fidelity), nz(row.ssim), deltaSuffix(row.delta));
            case "missing_actual":
                return String.format("- **%s** -- CN1 render not delivered.", row.test);
            case "missing_expected":
                return String.format("- **%s** -- native golden missing.", row.test);
            case "size_mismatch":
                return String.format("- **%s** -- size mismatch: %s", row.test, row.message);
            case "error":
                return String.format("- **%s** -- error: %s", row.test, row.message);
            default:
                return String.format("- **%s** -- %s", row.test, row.status);
        }
    }

    private static String deltaSuffix(Double delta) {
        if (delta == null) {
            return " (no baseline)";
        }
        if (Math.abs(delta) < 0.005d) {
            return " (no change)";
        }
        return String.format(" (%+.2f vs baseline)", delta);
    }

    /// Value at percentile p (0-100) of an ascending-sorted list (nearest rank).
    private static double percentile(List<PairRow> sortedAsc, double p) {
        if (sortedAsc.isEmpty()) {
            return 0.0d;
        }
        int idx = (int) Math.round((p / 100.0d) * (sortedAsc.size() - 1));
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= sortedAsc.size()) {
            idx = sortedAsc.size() - 1;
        }
        return sortedAsc.get(idx).fidelity;
    }

    /// Splits a "Component_state_appearance" test id into its three parts. The
    /// appearance and state are the last two underscore-separated tokens; the
    /// component name (which may itself contain no underscore) is the remainder.
    private static String[] splitTest(String test) {
        int last = test.lastIndexOf('_');
        if (last < 0) {
            return new String[] {test, "", ""};
        }
        String appearance = test.substring(last + 1);
        int prev = test.lastIndexOf('_', last - 1);
        if (prev < 0) {
            return new String[] {test.substring(0, last), "", appearance};
        }
        return new String[] {test.substring(0, prev), test.substring(prev + 1, last), appearance};
    }

    /// Baseline-delta for a table cell (ASCII only).
    private static String deltaCell(Double delta) {
        if (delta == null) {
            return "n/a";
        }
        if (Math.abs(delta) < 0.05d) {
            return "0.0";
        }
        return String.format("%+.1f", delta);
    }

    private static void addPreviewPair(List<String> lines, PairRow row) {
        String nativeName = stringValue(row.nativePreview.get("name"), null);
        String cn1Name = stringValue(row.cn1Preview.get("name"), null);
        if (nativeName == null && cn1Name == null) {
            return;
        }
        // Two attachment images on one line: native (left) vs CN1 (right).
        // PostPrComment uploads the preview files and resolves attachment:NAME.
        StringBuilder sb = new StringBuilder("  ");
        if (nativeName != null) {
            sb.append("![native ").append(row.test).append("](attachment:").append(nativeName).append(") ");
        }
        if (cn1Name != null) {
            sb.append("![cn1 ").append(row.test).append("](attachment:").append(cn1Name).append(")");
        }
        lines.add("");
        lines.add(sb.toString().stripTrailing());
        lines.add("  _Left: native widget. Right: Codename One render._");
    }

    private static Map<String, Double> loadBaseline(Path baselinePath) {
        Map<String, Double> baseline = new LinkedHashMap<>();
        if (baselinePath == null || !Files.isRegularFile(baselinePath)) {
            return baseline;
        }
        try {
            String text = Files.readString(baselinePath, StandardCharsets.UTF_8);
            Map<String, Object> obj = JsonUtil.asObject(JsonUtil.parse(text));
            // Baseline format: { "pairs": { "<test>": <fidelity_percent>, ... } }
            Map<String, Object> pairs = JsonUtil.asObject(obj.get("pairs"));
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

    private static void writeLines(Path path, List<String> lines) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i + 1 < lines.size()) {
                sb.append('\n');
            }
        }
        if (!lines.isEmpty()) {
            sb.append('\n');
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
    }

    private static double nz(Double value) {
        return value == null ? 0.0d : value;
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

    private record Report(List<String> summaryLines, List<String> commentLines) {
    }

    private static final class PairRow {
        final String test;
        final String status;
        final Double fidelity;
        final Double ssim;
        final Double meanDelta;
        final Double baseline;
        final Double delta;
        final String nativePath;
        final String cn1Path;
        final Map<String, Object> cn1Preview;
        final Map<String, Object> nativePreview;
        final String message;

        PairRow(String test, String status, Double fidelity, Double ssim, Double meanDelta, Double baseline,
                Double delta, String nativePath, String cn1Path, Map<String, Object> cn1Preview,
                Map<String, Object> nativePreview, String message) {
            this.test = test;
            this.status = status;
            this.fidelity = fidelity;
            this.ssim = ssim;
            this.meanDelta = meanDelta;
            this.baseline = baseline;
            this.delta = delta;
            this.nativePath = nativePath;
            this.cn1Path = cn1Path;
            this.cn1Preview = cn1Preview;
            this.nativePreview = nativePreview;
            this.message = message;
        }
    }

    private static final class Arguments {
        final Path compareJson;
        final Path commentOut;
        final Path summaryOut;
        final Path baselineJson;
        final String marker;
        final String title;
        final double aspirational;

        private Arguments(Path compareJson, Path commentOut, Path summaryOut, Path baselineJson,
                String marker, String title, double aspirational) {
            this.compareJson = compareJson;
            this.commentOut = commentOut;
            this.summaryOut = summaryOut;
            this.baselineJson = baselineJson;
            this.marker = marker;
            this.title = title;
            this.aspirational = aspirational;
        }

        static Arguments parse(String[] args) {
            Path compare = null;
            Path comment = null;
            Path summary = null;
            Path baseline = null;
            String marker = null;
            String title = null;
            double aspirational = ASPIRATIONAL_THRESHOLD;
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
                    case "--comment-out" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --comment-out");
                            return null;
                        }
                        comment = Path.of(args[i]);
                    }
                    case "--summary-out" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --summary-out");
                            return null;
                        }
                        summary = Path.of(args[i]);
                    }
                    case "--baseline" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --baseline");
                            return null;
                        }
                        baseline = Path.of(args[i]);
                    }
                    case "--marker" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --marker");
                            return null;
                        }
                        marker = args[i];
                    }
                    case "--title" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --title");
                            return null;
                        }
                        title = args[i];
                    }
                    case "--aspirational" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --aspirational");
                            return null;
                        }
                        try {
                            aspirational = Double.parseDouble(args[i]);
                        } catch (NumberFormatException ex) {
                            System.err.println("Invalid value for --aspirational: " + args[i]);
                            return null;
                        }
                    }
                    default -> {
                        System.err.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }
            if (compare == null || comment == null || summary == null) {
                System.err.println("--compare-json, --comment-out, and --summary-out are required");
                return null;
            }
            return new Arguments(compare, comment, summary, baseline, marker, title, aspirational);
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
