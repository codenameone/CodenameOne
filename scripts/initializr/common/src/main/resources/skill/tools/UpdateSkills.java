import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Fetches the freshest version of this Codename One skill (SKILL.md, every
/// reference, tool and template) straight from GitHub, so an agent can self-update
/// instead of relying on whatever shipped with the project.
///
/// It walks the skill directory in the repo via the GitHub contents API and
/// downloads each file's raw bytes into the local skill folder, preserving the
/// tree. Existing files are overwritten; new files are created.
///
/// Usage:
///     java tools/UpdateSkills.java                 # update the skill this tool lives in
///     java tools/UpdateSkills.java --dry-run       # list what WOULD change, write nothing
///     java tools/UpdateSkills.java --dir path/to/skill --ref master --token <PAT>
///
///   --dir DIR     local skill root to write into (default: auto-detected - the
///                 nearest ancestor of the working dir that contains SKILL.md, else
///                 the working dir)
///   --ref REF     git branch/tag/sha to fetch (default: master)
///   --token PAT   GitHub token (optional; raises the API rate limit)
///   --dry-run     print the file list and sizes, change nothing
///
/// Self-contained: only the JDK (java.net.http + a tiny embedded JSON reader).
/// Exit codes: 0 updated (or dry-run ok), 2 usage / network / IO error.
public class UpdateSkills {
    static final String REPO = "codenameone/CodenameOne";
    static final String SKILL_PATH = "scripts/initializr/common/src/main/resources/skill";

    public static void main(String[] args) {
        String ref = "master", token = null, dir = null;
        boolean dryRun = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--ref":     ref = args[++i]; break;
                    case "--token":   token = args[++i]; break;
                    case "--dir":     dir = args[++i]; break;
                    case "--dry-run": dryRun = true; break;
                    default: System.err.println("Unknown option: " + args[i]); usage(); return;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) { usage(); return; }

        Path root = dir != null ? Paths.get(dir) : detectSkillRoot();
        if (root == null) {
            System.err.println("Could not locate the skill root (no SKILL.md in the working dir or its parents).");
            System.err.println("Pass --dir <path-to-skill-folder>.");
            System.exit(2);
        }
        System.err.println("[UpdateSkills] target=" + root.toAbsolutePath() + " ref=" + ref + (dryRun ? " (dry-run)" : ""));

        HttpClient http = HttpClient.newHttpClient();
        List<String[]> files = new ArrayList<>(); // {relativePath, downloadUrl}
        try {
            listFiles(http, token, SKILL_PATH, ref, files);
        } catch (Exception e) {
            System.err.println("Failed to list skill files: " + e.getMessage());
            System.exit(2);
        }
        if (files.isEmpty()) { System.err.println("No files found under " + SKILL_PATH + " @" + ref); System.exit(2); }

        int changed = 0, written = 0;
        for (String[] fr : files) {
            String rel = fr[0], url = fr[1];
            try {
                byte[] remote = getBytes(http, token, url);
                Path local = root.resolve(rel);
                boolean differs = !Files.exists(local) || !sameBytes(Files.readAllBytes(local), remote);
                if (differs) changed++;
                if (dryRun) {
                    System.out.println((differs ? "CHANGED " : "same    ") + rel + "  (" + remote.length + " bytes)");
                } else {
                    if (local.getParent() != null && !Files.isDirectory(local.getParent())) {
                        Files.createDirectories(local.getParent());
                    }
                    Files.write(local, remote);
                    written++;
                    System.out.println((differs ? "updated " : "ok      ") + rel);
                }
            } catch (Exception e) {
                System.err.println("! failed " + rel + ": " + e.getMessage());
            }
        }
        System.err.println("[UpdateSkills] " + files.size() + " file(s), " + changed + " changed"
                + (dryRun ? " (dry-run, nothing written)" : ", " + written + " written"));
        System.exit(0);
    }

    /// Recursively walks the skill directory via the GitHub contents API,
    /// collecting {pathRelativeToSkillRoot, rawDownloadUrl} for every file.
    private static void listFiles(HttpClient http, String token, String path, String ref, List<String[]> out) throws Exception {
        String api = "https://api.github.com/repos/" + REPO + "/contents/" + path + "?ref=" + ref;
        Object json = Json.parse(new String(getBytes(http, token, api), StandardCharsets.UTF_8));
        if (!(json instanceof List)) throw new IOException("Unexpected API response for " + path);
        for (Object o : (List<?>) json) {
            Map<?, ?> entry = (Map<?, ?>) o;
            String type = str(entry.get("type"));
            String entryPath = str(entry.get("path"));
            if ("dir".equals(type)) {
                listFiles(http, token, entryPath, ref, out);
            } else if ("file".equals(type)) {
                String rel = entryPath.substring(SKILL_PATH.length() + 1);
                out.add(new String[]{rel, str(entry.get("download_url"))});
            }
        }
    }

    private static byte[] getBytes(HttpClient http, String token, String url) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "cn1-update-skills").header("Accept", "application/vnd.github+json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() == 403) throw new IOException("HTTP 403 (GitHub rate limit? pass --token)");
        if (r.statusCode() / 100 != 2) throw new IOException("HTTP " + r.statusCode() + " for " + url);
        return r.body();
    }

    /// Walks up from the working directory to find a folder containing SKILL.md.
    private static Path detectSkillRoot() {
        Path p = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 8 && p != null; i++, p = p.getParent()) {
            if (Files.exists(p.resolve("SKILL.md")) && Files.isDirectory(p.resolve("tools"))) return p;
        }
        return null;
    }

    private static boolean sameBytes(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static void usage() {
        System.err.println("Usage: java UpdateSkills.java [--dir DIR] [--ref REF] [--token PAT] [--dry-run]");
        System.exit(2);
    }

    // --- minimal JSON reader (objects, arrays, strings, numbers, bool, null) ---
    static final class Json {
        private final String s; private int i;
        private Json(String s) { this.s = s; }
        static Object parse(String s) throws IOException {
            Json j = new Json(s); j.ws();
            Object v = j.value(); j.ws();
            return v;
        }
        private Object value() throws IOException {
            char c = peek();
            switch (c) {
                case '{': return obj();
                case '[': return arr();
                case '"': return string();
                case 't': expect("true"); return Boolean.TRUE;
                case 'f': expect("false"); return Boolean.FALSE;
                case 'n': expect("null"); return null;
                default:  return number();
            }
        }
        private Map<String, Object> obj() throws IOException {
            Map<String, Object> m = new LinkedHashMap<>(); i++; ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws(); String k = string(); ws(); if (s.charAt(i) != ':') throw err(": expected"); i++; ws();
                m.put(k, value()); ws();
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw err(", or } expected");
            }
        }
        private List<Object> arr() throws IOException {
            List<Object> l = new ArrayList<>(); i++; ws();
            if (peek() == ']') { i++; return l; }
            while (true) {
                ws(); l.add(value()); ws();
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw err(", or ] expected");
            }
        }
        private String string() throws IOException {
            if (s.charAt(i) != '"') throw err("string expected"); i++;
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': b.append('\n'); break; case 't': b.append('\t'); break;
                        case 'r': b.append('\r'); break; case 'b': b.append('\b'); break;
                        case 'f': b.append('\f'); break; case '/': b.append('/'); break;
                        case '\\': b.append('\\'); break; case '"': b.append('"'); break;
                        case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: b.append(e);
                    }
                } else b.append(c);
            }
        }
        private Object number() throws IOException {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            return Double.parseDouble(s.substring(start, i));
        }
        private void expect(String w) throws IOException {
            if (!s.startsWith(w, i)) throw err(w + " expected"); i += w.length();
        }
        private char peek() { return s.charAt(i); }
        private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        private IOException err(String m) { return new IOException("JSON@" + i + ": " + m); }
    }
}
