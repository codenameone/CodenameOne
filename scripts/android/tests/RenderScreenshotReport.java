import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RenderScreenshotReport {
    private static final String DEFAULT_MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->";
    private static final String DEFAULT_TITLE = "Android screenshot updates";
    private static final String DEFAULT_SUCCESS_MESSAGE = "✅ Native Android screenshot tests passed.";

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            System.exit(2);
            return;
        }
        Path comparePath = arguments.compareJson;
        if (!Files.isRegularFile(comparePath)) {
            System.err.println("Comparison JSON not found: " + comparePath);
            System.exit(1);
        }
        String text = Files.readString(comparePath, StandardCharsets.UTF_8);
        Object parsed = JsonUtil.parse(text);
        Map<String, Object> data = JsonUtil.asObject(parsed);
        String marker = arguments.marker != null ? arguments.marker : DEFAULT_MARKER;
        String title = arguments.title != null ? arguments.title : DEFAULT_TITLE;
        String successMessage = arguments.successMessage != null ? arguments.successMessage : DEFAULT_SUCCESS_MESSAGE;

        SummaryAndComment output = buildSummaryAndComment(data, title, marker, successMessage);
        writeLines(arguments.summaryOut, output.summaryLines);
        writeLines(arguments.commentOut, output.commentLines);
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

    private static SummaryAndComment buildSummaryAndComment(Map<String, Object> data, String title, String marker, String successMessage) {
        List<String> summaryLines = new ArrayList<>();
        List<String> commentLines = new ArrayList<>();
        Object resultsObj = data.get("results");
        List<Object> results = resultsObj instanceof List<?> list ? (List<Object>) list : List.of();
        List<Map<String, Object>> commentEntries = new ArrayList<>();
        for (Object item : results) {
            Map<String, Object> result = JsonUtil.asObject(item);
            String test = stringValue(result.get("test"), "unknown");
            String status = stringValue(result.get("status"), "unknown");
            String expectedPath = stringValue(result.get("expected_path"), "");
            String actualPath = stringValue(result.get("actual_path"), "");
            Map<String, Object> details = JsonUtil.asObject(result.get("details"));
            String base64 = stringValue(result.get("base64"), null);
            String base64Omitted = stringValue(result.get("base64_omitted"), null);
            Integer base64Length = toInteger(result.get("base64_length"));
            String base64Mime = stringValue(result.get("base64_mime"), "image/png");
            String base64Codec = stringValue(result.get("base64_codec"), null);
            Integer base64Quality = toInteger(result.get("base64_quality"));
            String base64Note = stringValue(result.get("base64_note"), null);
            Map<String, Object> preview = JsonUtil.asObject(result.get("preview"));
            String previewName = stringValue(preview.get("name"), null);
            String previewPath = stringValue(preview.get("path"), null);
            String previewMime = stringValue(preview.get("mime"), null);
            String previewNote = stringValue(preview.get("note"), null);
            Integer previewQuality = toInteger(preview.get("quality"));
            String message;
            String copyFlag = "0";
            switch (status) {
                case "equal" -> message = "Matches stored reference.";
                case "missing_expected" -> {
                    message = "Reference screenshot missing at " + expectedPath + ".";
                    copyFlag = "1";
                    commentEntries.add(commentEntry(test, "missing reference", message, previewName, previewPath, previewMime, previewNote,
                            previewQuality, base64, base64Omitted, base64Length, base64Mime, base64Codec, base64Quality, base64Note, test + ".png"));
                }
                case "different" -> {
                    String dims = "";
                    if (!details.isEmpty()) {
                        dims = String.format(" (%sx%s px, bit depth %s)",
                                stringValue(details.get("width"), ""),
                                stringValue(details.get("height"), ""),
                                stringValue(details.get("bit_depth"), ""));
                    }
                    message = "Screenshot differs" + dims + ".";
                    copyFlag = "1";
                    commentEntries.add(commentEntry(test, "updated screenshot", message, previewName, previewPath, previewMime,
                            previewNote, previewQuality, base64, base64Omitted, base64Length, base64Mime, base64Codec,
                            base64Quality, base64Note, test + ".png"));
                }
                case "error" -> {
                    message = "Comparison error: " + stringValue(result.get("message"), "unknown error");
                    copyFlag = "1";
                    commentEntries.add(commentEntry(test, "comparison error", message, previewName, previewPath, previewMime,
                            previewNote, previewQuality, null, base64Omitted, base64Length, base64Mime, base64Codec,
                            base64Quality, base64Note, test + ".png"));
                }
                case "missing_actual" -> {
                    message = "Actual screenshot missing (test did not produce output).";
                    copyFlag = "1";
                    commentEntries.add(commentEntry(test, "missing actual screenshot", message, previewName, previewPath,
                            previewMime, previewNote, previewQuality, null, base64Omitted, base64Length, base64Mime,
                            base64Codec, base64Quality, base64Note, null));
                }
                default -> message = "Status: " + status + ".";
            }
            String noteColumn = previewNote != null ? previewNote : base64Note != null ? base64Note : "";
            summaryLines.add(String.join("|", List.of(status, test, message, copyFlag, actualPath, noteColumn)));
        }

        if (!commentEntries.isEmpty()) {
            if (title != null && !title.isEmpty()) {
                commentLines.add("### " + title);
                commentLines.add("");
            }
            for (Map<String, Object> entry : commentEntries) {
                String test = stringValue(entry.get("test"), "");
                String status = stringValue(entry.get("status"), "");
                String message = stringValue(entry.get("message"), "");
                commentLines.add(String.format("- **%s** — %s. %s", test, status, message));
                addPreviewSection(commentLines, entry);
                commentLines.add("");
            }
            if (!commentLines.isEmpty() && !commentLines.get(commentLines.size() - 1).isEmpty()) {
                commentLines.add("");
            }
            commentLines.add(marker);
        } else {
            commentLines.add(successMessage != null ? successMessage : DEFAULT_SUCCESS_MESSAGE);
            commentLines.add("");
            commentLines.add(marker);
        }
        return new SummaryAndComment(summaryLines, commentLines);
    }

    private static Map<String, Object> commentEntry(
            String test,
            String status,
            String message,
            String previewName,
            String previewPath,
            String previewMime,
            String previewNote,
            Integer previewQuality,
            String base64,
            String base64Omitted,
            Integer base64Length,
            String base64Mime,
            String base64Codec,
            Integer base64Quality,
            String base64Note,
            String artifactName
    ) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("test", test);
        entry.put("status", status);
        entry.put("message", message);
        entry.put("artifact_name", artifactName);
        entry.put("preview_name", previewName);
        entry.put("preview_path", previewPath);
        entry.put("preview_mime", previewMime);
        entry.put("preview_note", previewNote);
        entry.put("preview_quality", previewQuality);
        entry.put("base64", base64);
        entry.put("base64_omitted", base64Omitted);
        entry.put("base64_length", base64Length);
        entry.put("base64_mime", base64Mime);
        entry.put("base64_codec", base64Codec);
        entry.put("base64_quality", base64Quality);
        entry.put("base64_note", base64Note);
        return entry;
    }

    private static void addPreviewSection(List<String> lines, Map<String, Object> entry) {
        String previewName = stringValue(entry.get("preview_name"), null);
        Integer previewQuality = toInteger(entry.get("preview_quality"));
        String previewNote = stringValue(entry.get("preview_note"), null);
        String base64Note = stringValue(entry.get("base64_note"), null);
        String previewMime = stringValue(entry.get("preview_mime"), null);
        List<String> notes = new ArrayList<>();
        if ("image/jpeg".equals(previewMime) && previewQuality != null) {
            notes.add("JPEG preview quality " + previewQuality);
        }
        if (previewNote != null && !previewNote.isEmpty()) {
            notes.add(previewNote);
        }
        if (base64Note != null && !base64Note.isEmpty() && (previewNote == null || !previewNote.equals(base64Note))) {
            notes.add(base64Note);
        }
        if (previewName != null) {
            lines.add("");
            lines.add("  ![" + entry.get("test") + "](attachment:" + previewName + ")");
            if (!notes.isEmpty()) {
                lines.add("  _Preview info: " + String.join("; ", notes) + "._");
            }
        } else if (entry.get("base64") != null) {
            lines.add("");
            lines.add("  _Preview generated but could not be published; see workflow artifacts for JPEG preview._");
            if (!notes.isEmpty()) {
                lines.add("  _Preview info: " + String.join("; ", notes) + "._");
            }
        } else if ("too_large".equals(entry.get("base64_omitted"))) {
            lines.add("");
            String sizeNote = "";
            Integer length = toInteger(entry.get("base64_length"));
            if (length != null && length > 0) {
                sizeNote = " (base64 length ≈ " + String.format("%,d", length) + " chars)";
            }
            List<String> extra = new ArrayList<>();
            if ("jpeg".equals(stringValue(entry.get("base64_codec"), null)) && entry.get("base64_quality") != null) {
                extra.add("attempted JPEG quality " + entry.get("base64_quality"));
            }
            if (base64Note != null && !base64Note.isEmpty()) {
                extra.add(base64Note);
            }
            String tail = extra.isEmpty() ? "" : " (" + String.join("; ", extra) + ")";
            lines.add("  _Screenshot omitted from comment because the encoded payload exceeded GitHub's size limits" + sizeNote + "." + tail + "_");
        } else {
            lines.add("");
            lines.add("  _No preview available for this screenshot._");
        }
        String artifactName = stringValue(entry.get("artifact_name"), null);
        if (artifactName != null) {
            lines.add("  _Full-resolution PNG saved as `" + artifactName + "` in workflow artifacts._");
        }
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

    private static Integer toInteger(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Long l) {
            return l.intValue();
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private record SummaryAndComment(List<String> summaryLines, List<String> commentLines) {
    }

    private static class Arguments {
        final Path compareJson;
        final Path commentOut;
        final Path summaryOut;
        final String marker;
        final String title;
        final String successMessage;

        private Arguments(Path compareJson, Path commentOut, Path summaryOut, String marker, String title, String successMessage) {
            this.compareJson = compareJson;
            this.commentOut = commentOut;
            this.summaryOut = summaryOut;
            this.marker = marker;
            this.title = title;
            this.successMessage = successMessage;
        }

        static Arguments parse(String[] args) {
            Path compare = null;
            Path comment = null;
            Path summary = null;
            String marker = null;
            String title = null;
            String successMessage = null;
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
                    case "--success-message" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --success-message");
                            return null;
                        }
                        successMessage = args[i];
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
            return new Arguments(compare, comment, summary, marker, title, successMessage);
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
