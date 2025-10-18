import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostPrComment {
    private static final String DEFAULT_MARKER = "<!-- CN1SS_SCREENSHOT_COMMENT -->";
    private static final String DEFAULT_LOG_PREFIX = "[run-android-instrumentation-tests]";
    private static String marker = DEFAULT_MARKER;
    private static String logPrefix = DEFAULT_LOG_PREFIX;

    public static void main(String[] args) throws Exception {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    private static int execute(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            return 2;
        }
        marker = arguments.marker != null ? arguments.marker : DEFAULT_MARKER;
        logPrefix = arguments.logPrefix != null ? arguments.logPrefix : DEFAULT_LOG_PREFIX;

        Path bodyPath = arguments.body;
        if (!Files.isRegularFile(bodyPath)) {
            return 0;
        }
        String rawBody = Files.readString(bodyPath, StandardCharsets.UTF_8);
        String body = rawBody.trim();
        if (body.isEmpty()) {
            return 0;
        }
        if (!body.contains(marker)) {
            body = body.stripTrailing() + "\n\n" + marker;
        }
        String bodyWithoutMarker = body.replace(marker, "").trim();
        if (bodyWithoutMarker.isEmpty()) {
            return 0;
        }

        String eventPathEnv = System.getenv("GITHUB_EVENT_PATH");
        String repo = System.getenv("GITHUB_REPOSITORY");
        String token = System.getenv("GITHUB_TOKEN");
        if (eventPathEnv == null || repo == null || token == null) {
            return 0;
        }
        Path eventPath = Path.of(eventPathEnv);
        if (!Files.isRegularFile(eventPath)) {
            return 0;
        }

        Map<String, Object> event = JsonUtil.asObject(JsonUtil.parse(Files.readString(eventPath, StandardCharsets.UTF_8)));
        Integer prNumber = findPrNumber(event);
        if (prNumber == null) {
            return 0;
        }

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
        Map<String, String> headers = Map.of(
                "Authorization", "token " + token,
                "Accept", "application/vnd.github+json",
                "Content-Type", "application/json"
        );

        boolean isForkPr = isForkPullRequest(event);
        CommentContext context = locateExistingComment(client, headers, repo, prNumber, body, event);
        if (context == null) {
            return 1;
        }
        Long commentId = context.commentId;
        boolean createdPlaceholder = context.createdPlaceholder;

        Path previewDir = arguments.previewDir;
        Map<String, String> attachmentUrls = new HashMap<>();
        if (body.contains("(attachment:")) {
            try {
                attachmentUrls = publishPreviewsToBranch(previewDir, repo, prNumber, token, !isForkPr);
                for (Map.Entry<String, String> entry : attachmentUrls.entrySet()) {
                    log("Preview available for " + entry.getKey() + ": " + entry.getValue());
                }
            } catch (Exception ex) {
                err("Preview publishing failed: " + ex.getMessage());
                return 1;
            }
        }

        AttachmentReplacement replacement = replaceAttachments(body, attachmentUrls);
        if (!replacement.missing.isEmpty()) {
            if (isForkPr) {
                log("Preview URLs unavailable in forked PR context; placeholders left as-is");
            } else {
                err("Failed to resolve preview URLs for: " + String.join(", ", replacement.missing));
                return 1;
            }
        }

        String finalBody = replacement.body;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repo + "/issues/comments/" + commentId))
                .timeout(Duration.ofSeconds(20))
                .headers(headers.entrySet().stream().flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue())).toArray(String[]::new))
                .method("PATCH", HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(Map.of("body", finalBody))))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String action = createdPlaceholder ? "posted" : "updated";
            log("PR comment " + action + " (status=" + response.statusCode() + ", bytes=" + finalBody.length() + ")");
            return 0;
        } else {
            err("Failed to update PR comment: HTTP " + response.statusCode() + " - " + response.body());
            return 1;
        }
    }

    private static CommentContext locateExistingComment(HttpClient client, Map<String, String> headers, String repo, int prNumber, String body, Map<String, Object> event) throws IOException, InterruptedException {
        String commentsUrl = "https://api.github.com/repos/" + repo + "/issues/" + prNumber + "/comments?per_page=100";
        Map<String, Object> existingComment = null;
        Map<String, Object> preferredComment = null;
        String actor = System.getenv("GITHUB_ACTOR");
        Set<String> preferredLogins = new java.util.HashSet<>();
        if (actor != null && !actor.isEmpty()) {
            preferredLogins.add(actor);
        }
        preferredLogins.add("github-actions[bot]");

        while (commentsUrl != null) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(commentsUrl))
                    .timeout(Duration.ofSeconds(20))
                    .headers(headers.entrySet().stream().flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue())).toArray(String[]::new))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                err("Failed to list PR comments: HTTP " + response.statusCode());
                return null;
            }
            Object parsed = JsonUtil.parse(response.body());
            List<Object> comments = parsed instanceof List<?> list ? (List<Object>) list : List.of();
            for (Object comment : comments) {
                Map<String, Object> commentMap = JsonUtil.asObject(comment);
                String bodyText = stringValue(commentMap.get("body"), "");
                if (bodyText.contains(marker)) {
                    existingComment = commentMap;
                    Map<String, Object> user = JsonUtil.asObject(commentMap.get("user"));
                    String login = stringValue(user.get("login"), null);
                    if (login != null && preferredLogins.contains(login)) {
                        preferredComment = commentMap;
                    }
                }
            }
            commentsUrl = nextLink(response.headers().firstValue("Link").orElse(null));
        }
        if (preferredComment != null) {
            existingComment = preferredComment;
        }

        Long commentId = null;
        boolean createdPlaceholder = false;
        if (existingComment != null) {
            Object idValue = existingComment.get("id");
            if (idValue instanceof Number number) {
                commentId = number.longValue();
            }
        } else {
            HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repo + "/issues/" + prNumber + "/comments"))
                    .timeout(Duration.ofSeconds(20))
                    .headers(headers.entrySet().stream().flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue())).toArray(String[]::new))
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtil.stringify(Map.of("body", marker))))
                    .build();
            HttpResponse<String> createResponse = client.send(createRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (createResponse.statusCode() >= 200 && createResponse.statusCode() < 300) {
                Map<String, Object> created = JsonUtil.asObject(JsonUtil.parse(createResponse.body()));
                Object idValue = created.get("id");
                if (idValue instanceof Number number) {
                    commentId = number.longValue();
                }
                createdPlaceholder = commentId != null;
                if (createdPlaceholder) {
                    log("Created new screenshot comment placeholder (id=" + commentId + ")");
                }
            }
        }
        if (commentId == null) {
            err("Unable to locate or create PR comment placeholder");
            return null;
        }
        return new CommentContext(commentId, createdPlaceholder);
    }

    private static Integer findPrNumber(Map<String, Object> event) {
        Object prData = event.get("pull_request");
        if (prData instanceof Map<?, ?> map) {
            Object number = ((Map<?, ?>) prData).get("number");
            if (number instanceof Number num) {
                return num.intValue();
            }
        }
        Object issue = event.get("issue");
        if (issue instanceof Map<?, ?> issueMap) {
            Object pr = ((Map<?, ?>) issueMap).get("pull_request");
            if (pr instanceof Map<?, ?>) {
                Object number = issueMap.get("number");
                if (number instanceof Number num) {
                    return num.intValue();
                }
            }
        }
        return null;
    }

    private static boolean isForkPullRequest(Map<String, Object> event) {
        Map<String, Object> pr = JsonUtil.asObject(event.get("pull_request"));
        Map<String, Object> head = JsonUtil.asObject(pr.get("head"));
        Map<String, Object> repo = JsonUtil.asObject(head.get("repo"));
        Object fork = repo.get("fork");
        if (fork instanceof Boolean b) {
            return b;
        }
        return false;
    }

    private static String nextLink(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }
        String[] parts = header.split(",");
        for (String part : parts) {
            String segment = part.trim();
            if (segment.endsWith("rel=\"next\"")) {
                int lt = segment.indexOf('<');
                int gt = segment.indexOf('>');
                if (lt >= 0 && gt > lt) {
                    return segment.substring(lt + 1, gt);
                }
            }
        }
        return null;
    }

    private static Map<String, String> publishPreviewsToBranch(Path previewDir, String repo, int prNumber, String token, boolean allowPush) throws IOException, InterruptedException {
        if (previewDir == null || !Files.isDirectory(previewDir)) {
            return Map.of();
        }
        List<Path> imageFiles = new ArrayList<>();
        try (var stream = Files.list(previewDir)) {
            stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> {
                        String lower = path.getFileName().toString().toLowerCase();
                        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
                    })
                    .sorted()
                    .forEach(imageFiles::add);
        }
        if (imageFiles.isEmpty()) {
            return Map.of();
        }
        if (!allowPush) {
            log("Preview publishing skipped for forked PR");
            return Map.of();
        }
        if (repo == null || repo.isEmpty() || token == null || token.isEmpty()) {
            return Map.of();
        }
        Path workspace = Path.of(Optional.ofNullable(System.getenv("GITHUB_WORKSPACE")).orElse(".")).toAbsolutePath();
        Path worktree = workspace.resolve(".cn1ss-previews-pr-" + prNumber);
        deleteRecursively(worktree);
        Files.createDirectories(worktree);
        Map<String, String> env = new HashMap<>(System.getenv());
        env.putIfAbsent("GIT_TERMINAL_PROMPT", "0");
        runGit(worktree, env, "init");
        String actor = Optional.ofNullable(System.getenv("GITHUB_ACTOR")).filter(s -> !s.isBlank()).orElse("github-actions");
        runGit(worktree, env, "config", "user.name", actor);
        runGit(worktree, env, "config", "user.email", "github-actions@users.noreply.github.com");
        String remoteUrl = "https://x-access-token:" + token + "@github.com/" + repo + ".git";
        runGit(worktree, env, "remote", "add", "origin", remoteUrl);
        ProcessResult hasBranch = runGit(worktree, env, false, "ls-remote", "--heads", "origin", "cn1ss-previews");
        if (hasBranch.exitCode == 0 && !hasBranch.stdout.trim().isEmpty()) {
            runGit(worktree, env, "fetch", "origin", "cn1ss-previews");
            runGit(worktree, env, "checkout", "cn1ss-previews");
        } else {
            runGit(worktree, env, "checkout", "--orphan", "cn1ss-previews");
        }
        Path dest = worktree.resolve("pr-" + prNumber);
        deleteRecursively(dest);
        Files.createDirectories(dest);
        for (Path source : imageFiles) {
            Files.copy(source, dest.resolve(source.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        runGit(worktree, env, "add", "-A", ".");
        ProcessResult status = runGit(worktree, env, true, "status", "--porcelain");
        if (!status.stdout.trim().isEmpty()) {
            runGit(worktree, env, "commit", "-m", "Add previews for PR #" + prNumber);
            ProcessResult push = runGit(worktree, env, false, "push", "origin", "HEAD:cn1ss-previews");
            if (push.exitCode != 0) {
                throw new IOException(push.stderr.isEmpty() ? push.stdout : push.stderr);
            }
            log("Published " + imageFiles.size() + " preview(s) to cn1ss-previews/pr-" + prNumber);
        } else {
            log("Preview branch already up-to-date for PR #" + prNumber);
        }
        String rawBase = "https://raw.githubusercontent.com/" + repo + "/cn1ss-previews/pr-" + prNumber;
        Map<String, String> urls = new LinkedHashMap<>();
        try (var stream = Files.list(dest)) {
            stream.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> urls.put(path.getFileName().toString(), rawBase + "/" + path.getFileName()));
        }
        deleteRecursively(worktree);
        return urls;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    private static ProcessResult runGit(Path cwd, Map<String, String> env, String... args) throws IOException, InterruptedException {
        return runGit(cwd, env, true, args);
    }

    private static ProcessResult runGit(Path cwd, Map<String, String> env, boolean check, String... args) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add("git");
        java.util.Collections.addAll(command, args);
        builder.command(command);
        builder.directory(cwd.toFile());
        builder.environment().putAll(env);
        Process process = builder.start();
        int exitCode = process.waitFor();
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        if (check && exitCode != 0) {
            throw new IOException("git " + String.join(" ", args) + " failed: " + (stderr.isEmpty() ? stdout : stderr));
        }
        return new ProcessResult(exitCode, stdout, stderr);
    }

    private static String readStream(java.io.InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private static AttachmentReplacement replaceAttachments(String body, Map<String, String> urls) {
        Pattern pattern = Pattern.compile("\\(attachment:([^\\)]+)\\)");
        Matcher matcher = pattern.matcher(body);
        StringBuffer sb = new StringBuffer();
        List<String> missing = new ArrayList<>();
        while (matcher.find()) {
            String name = matcher.group(1);
            String url = urls.get(name);
            if (url != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("(" + url + ")"));
            } else {
                missing.add(name);
                log("Preview URL missing for " + name + "; leaving placeholder");
                matcher.appendReplacement(sb, "(#)");
            }
        }
        matcher.appendTail(sb);
        return new AttachmentReplacement(sb.toString(), missing);
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

    private static void log(String message) {
        System.out.println(logPrefix + " " + message);
    }

    private static void err(String message) {
        System.err.println(logPrefix + " " + message);
    }

    private record CommentContext(long commentId, boolean createdPlaceholder) {
    }

    private record AttachmentReplacement(String body, List<String> missing) {
    }

    private static class Arguments {
        final Path body;
        final Path previewDir;
        final String marker;
        final String logPrefix;

        private Arguments(Path body, Path previewDir, String marker, String logPrefix) {
            this.body = body;
            this.previewDir = previewDir;
            this.marker = marker;
            this.logPrefix = logPrefix;
        }

        static Arguments parse(String[] args) {
            Path body = null;
            Path previewDir = null;
            String marker = null;
            String logPrefix = null;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--body" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --body");
                            return null;
                        }
                        body = Path.of(args[i]);
                    }
                    case "--preview-dir" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --preview-dir");
                            return null;
                        }
                        previewDir = Path.of(args[i]);
                    }
                    case "--marker" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --marker");
                            return null;
                        }
                        marker = args[i];
                    }
                    case "--log-prefix" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --log-prefix");
                            return null;
                        }
                        logPrefix = args[i];
                    }
                    default -> {
                        System.err.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }
            if (body == null) {
                System.err.println("--body is required");
                return null;
            }
            return new Arguments(body, previewDir, marker, logPrefix);
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
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
