package com.codename1.tools.cn1ss;

import java.io.IOException;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PrCommentPublisher {
    private static final String LOG_PREFIX = "[run-ios-simulator-tests]";
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("\\(attachment:([^)]+)\\)");

    private PrCommentPublisher() {
    }

    static int publish(Path bodyPath, Path previewDir) throws Exception {
        if (!Files.isRegularFile(bodyPath)) {
            return 0;
        }
        String rawBody = Files.readString(bodyPath, StandardCharsets.UTF_8);
        String body = rawBody.trim();
        if (body.isEmpty()) {
            return 0;
        }
        if (!body.contains(CommentRenderer.MARKER)) {
            body = body + System.lineSeparator() + System.lineSeparator() + CommentRenderer.MARKER;
        }
        String bodyWithoutMarker = body.replace(CommentRenderer.MARKER, "").trim();
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

        Object eventData = Json.parse(Files.readString(eventPath, StandardCharsets.UTF_8));
        if (!(eventData instanceof Map)) {
            return 0;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> event = (Map<String, Object>) eventData;
        Integer prNumber = findPrNumber(event);
        if (prNumber == null) {
            return 0;
        }

        boolean isFork = isForkedPr(event);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        Map<String, Object> existing = findExistingComment(client, repo, prNumber, token);
        Integer commentId = existing != null ? toInt(existing.get("id")) : null;
        boolean createdPlaceholder = false;
        if (commentId == null) {
            commentId = createPlaceholder(client, repo, prNumber, token);
            if (commentId == null) {
                return 1;
            }
            createdPlaceholder = true;
            log("Created new screenshot comment placeholder (id=" + commentId + ")");
        }

        Map<String, String> attachmentUrls = new HashMap<>();
        if (body.contains("(attachment:")) {
            try {
                attachmentUrls = publishPreviews(previewDir, repo, prNumber, token, !isFork);
                for (Map.Entry<String, String> entry : attachmentUrls.entrySet()) {
                    log("Preview available for " + entry.getKey() + ": " + entry.getValue());
                }
            } catch (Exception ex) {
                err("Preview publishing failed: " + ex.getMessage());
                return 1;
            }
        }

        List<String> missing = new ArrayList<>();
        String finalBody = replaceAttachments(body, attachmentUrls, missing);
        if (!missing.isEmpty() && !isFork) {
            err("Failed to resolve preview URLs for: " + String.join(", ", missing));
            return 1;
        }
        if (!missing.isEmpty() && isFork) {
            log("Preview URLs unavailable in forked PR context; placeholders left as-is");
        }

        String jsonBody = Json.stringify(Map.of("body", finalBody));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repo + "/issues/comments/" + commentId))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            err("PR comment update failed with status " + response.statusCode() + ": " + response.body());
            return 1;
        }
        log("PR comment " + (createdPlaceholder ? "posted" : "updated") + " (status=" + response.statusCode() + ")");
        return 0;
    }

    private static Integer findPrNumber(Map<String, Object> event) {
        Integer number = toInt(getNested(event, "pull_request", "number"));
        if (number != null) {
            return number;
        }
        Object issueObj = event.get("issue");
        if (issueObj instanceof Map) {
            Map<?, ?> issue = (Map<?, ?>) issueObj;
            if (issue.get("pull_request") != null) {
                return toInt(issue.get("number"));
            }
        }
        return null;
    }

    private static boolean isForkedPr(Map<String, Object> event) {
        Object pullRequest = event.get("pull_request");
        if (pullRequest instanceof Map) {
            Object head = ((Map<?, ?>) pullRequest).get("head");
            if (head instanceof Map) {
                Object repo = ((Map<?, ?>) head).get("repo");
                if (repo instanceof Map) {
                    Object fork = ((Map<?, ?>) repo).get("fork");
                    if (fork instanceof Boolean) {
                        return (Boolean) fork;
                    }
                }
            }
        }
        return false;
    }

    private static Map<String, Object> findExistingComment(HttpClient client, String repo, int prNumber, String token) throws Exception {
        String url = "https://api.github.com/repos/" + repo + "/issues/" + prNumber + "/comments?per_page=100";
        String actor = Optional.ofNullable(System.getenv("GITHUB_ACTOR")).orElse("");
        while (url != null) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IOException("Failed to list comments: status=" + response.statusCode());
            }
            Object data = Json.parse(response.body());
            if (!(data instanceof List)) {
                break;
            }
            @SuppressWarnings("unchecked")
            List<Object> comments = (List<Object>) data;
            Map<String, Object> preferred = null;
            for (Object item : comments) {
                if (!(item instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> comment = (Map<String, Object>) item;
                String body = stringValue(comment.get("body"));
                if (body != null && body.contains(CommentRenderer.MARKER)) {
                    Map<String, Object> user = getMap(comment, "user");
                    String login = user != null ? stringValue(user.get("login")) : null;
                    if (preferred == null) {
                        preferred = comment;
                    }
                    if (login != null) {
                        if (login.equals(actor) || login.equals("github-actions[bot]")) {
                            return comment;
                        }
                    }
                }
            }
            if (preferred != null) {
                return preferred;
            }
            url = nextLink(response.headers().firstValue("Link").orElse(null));
        }
        return null;
    }

    private static Integer createPlaceholder(HttpClient client, String repo, int prNumber, String token) throws Exception {
        Map<String, Object> payload = Map.of("body", CommentRenderer.MARKER);
        String json = Json.stringify(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repo + "/issues/" + prNumber + "/comments"))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            err("Failed to create PR comment placeholder: status=" + response.statusCode());
            return null;
        }
        Object data = Json.parse(response.body());
        if (data instanceof Map) {
            return toInt(((Map<?, ?>) data).get("id"));
        }
        return null;
    }

    private static Map<String, String> publishPreviews(
            Path previewDir,
            String repo,
            int prNumber,
            String token,
            boolean allowPush
    ) throws IOException, InterruptedException {
        Map<String, String> urls = new HashMap<>();
        if (previewDir == null || !Files.isDirectory(previewDir)) {
            return urls;
        }
        List<Path> images = new ArrayList<>();
        try (var stream = Files.list(previewDir)) {
            stream.filter(path -> Files.isRegularFile(path))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
                    })
                    .sorted()
                    .forEach(images::add);
        }
        if (images.isEmpty() || !allowPush || repo == null || repo.isEmpty() || token == null || token.isEmpty()) {
            return urls;
        }

        Path workspace = Optional.ofNullable(System.getenv("GITHUB_WORKSPACE"))
                .map(Path::of)
                .orElse(Path.of("."))
                .toAbsolutePath();
        Path worktree = workspace.resolve(".cn1ss-previews-pr-" + prNumber);
        if (Files.exists(worktree)) {
            FileUtils.deleteRecursive(worktree);
        }
        Files.createDirectories(worktree);

        try {
            runGit(worktree, List.of("init"));
            String actor = Optional.ofNullable(System.getenv("GITHUB_ACTOR")).orElse("github-actions");
            runGit(worktree, List.of("config", "user.name", actor));
            runGit(worktree, List.of("config", "user.email", "github-actions@users.noreply.github.com"));
            String remoteUrl = "https://x-access-token:" + token + "@github.com/" + repo + ".git";
            runGit(worktree, List.of("remote", "add", "origin", remoteUrl));

            ProcessResult lsRemote = runGit(worktree, List.of("ls-remote", "--heads", "origin", "cn1ss-previews"), false);
            if (lsRemote.exitCode == 0 && !lsRemote.stdout.isBlank()) {
                runGit(worktree, List.of("fetch", "origin", "cn1ss-previews"));
                runGit(worktree, List.of("checkout", "cn1ss-previews"));
            } else {
                runGit(worktree, List.of("checkout", "--orphan", "cn1ss-previews"));
            }

            Path dest = worktree.resolve("pr-" + prNumber);
            if (Files.exists(dest)) {
                FileUtils.deleteRecursive(dest);
            }
            Files.createDirectories(dest);
            for (Path image : images) {
                Files.copy(image, dest.resolve(image.getFileName()));
            }

            runGit(worktree, List.of("add", "-A", "."));
            ProcessResult status = runGit(worktree, List.of("status", "--porcelain"), false);
            if (!status.stdout.isBlank()) {
                runGit(worktree, List.of("commit", "-m", "Add previews for PR #" + prNumber));
                ProcessResult push = runGit(worktree, List.of("push", "origin", "HEAD:cn1ss-previews"), false);
                if (push.exitCode != 0) {
                    throw new IOException(push.stderr.isBlank() ? push.stdout.trim() : push.stderr.trim());
                }
                log("Published " + images.size() + " preview(s) to cn1ss-previews/pr-" + prNumber);
            } else {
                log("Preview branch already up-to-date for PR #" + prNumber);
            }

            String rawBase = "https://raw.githubusercontent.com/" + repo + "/cn1ss-previews/pr-" + prNumber;
            try (var stream = Files.list(dest)) {
                stream.filter(Files::isRegularFile)
                        .sorted()
                        .forEach(path -> urls.put(path.getFileName().toString(), rawBase + "/" + path.getFileName()));
            }
        } finally {
            FileUtils.deleteRecursive(worktree);
        }
        return urls;
    }

    private static String replaceAttachments(String body, Map<String, String> urls, List<String> missing) {
        Matcher matcher = ATTACHMENT_PATTERN.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String url = urls.get(name);
            if (url != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("(" + url + ")"));
            } else {
                missing.add(name);
                log("Preview URL missing for " + name + "; leaving placeholder");
                matcher.appendReplacement(sb, Matcher.quoteReplacement("(#)"));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String nextLink(String header) {
        if (header == null) {
            return null;
        }
        String[] parts = header.split(",");
        for (String part : parts) {
            String segment = part.trim();
            if (segment.endsWith("rel=\"next\"")) {
                int start = segment.indexOf('<');
                int end = segment.indexOf('>');
                if (start >= 0 && end > start) {
                    return segment.substring(start + 1, end);
                }
            }
        }
        return null;
    }

    private static void log(String message) {
        System.out.println(LOG_PREFIX + " " + message);
    }

    private static void err(String message) {
        System.err.println(LOG_PREFIX + " " + message);
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    private static Object getNested(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return current;
    }

    private static ProcessResult runGit(Path workdir, List<String> args) throws IOException, InterruptedException {
        return runGit(workdir, args, true);
    }

    private static ProcessResult runGit(Path workdir, List<String> args, boolean check) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(args);
        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> env = builder.environment();
        env.putIfAbsent("GIT_TERMINAL_PROMPT", "0");
        builder.directory(workdir.toFile());
        Process process = builder.start();
        int exit = process.waitFor();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (check && exit != 0) {
            throw new IOException("git command failed: " + String.join(" ", args) + " -> " + stderr.trim());
        }
        return new ProcessResult(exit, stdout, stderr);
    }

    private static final class ProcessResult {
        final int exitCode;
        final String stdout;
        final String stderr;

        ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = Objects.requireNonNullElse(stdout, "");
            this.stderr = Objects.requireNonNullElse(stderr, "");
        }
    }
}
