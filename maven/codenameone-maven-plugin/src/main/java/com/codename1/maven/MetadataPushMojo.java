package com.codename1.maven;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Pushes "metadata as code" (store listing text + screenshots) to the Codename
 * One build cloud, which applies it to App Store Connect / Google Play at
 * submission time. The descriptor lives in your project as a folder of plain text
 * files (one file per listing field) so the listing is versioned in git and
 * CI-drivable; this goal reads it, builds the descriptor, and PUTs it plus the
 * screenshots over the same Codename One account the build uses.
 *
 * <p>Folder layout (default {@code <cn1-project>/cn1-metadata}):
 * <pre>
 *   cn1-metadata/
 *     apple/
 *       primary_locale.txt          (optional; default "en-US")
 *       primary_category.txt        (optional; ASC appCategories id)
 *       secondary_category.txt      (optional)
 *       en-US/
 *         name.txt subtitle.txt description.txt keywords.txt
 *         promotional_text.txt whats_new.txt
 *         marketing_url.txt support_url.txt privacy_url.txt
 *         screenshots/APP_IPHONE_67/1.png 2.png ...
 *     google/
 *       en-US/
 *         name.txt subtitle.txt description.txt whats_new.txt support_url.txt
 *         screenshots/phoneScreenshots/1.png ...
 * </pre>
 *
 * <p>For Apple, {@code screenshots/<dir>} names are ASC {@code screenshotDisplayType}
 * values; for Google they are listing {@code imageType} values (e.g.
 * {@code phoneScreenshots}). Unset files are simply omitted (the store keeps its
 * current value). Metadata is a paid feature (Basic and up) and monthly-metered.
 *
 * <p>Auth reuses the cached Codename One token (run {@code mvn cn1:certificatewizard}
 * once, or {@code -Dtoken=...}); the app's bundle id comes from
 * {@code codename1.packageName} in {@code codenameone_settings.properties}.
 */
@Mojo(name = "metadata-push", requiresProject = true)
public class MetadataPushMojo extends AbstractCN1Mojo {

    /** Base URL of the Codename One build cloud. */
    @Parameter(property = "baseUrl", defaultValue = "https://cloud.codenameone.com")
    private String baseUrl;

    /** Keycloak bearer token; falls back to the cached CN1 login token. */
    @Parameter(property = "token")
    private String token;

    /** {@code apple}, {@code google}, or {@code both} (default). */
    @Parameter(property = "store", defaultValue = "both")
    private String store;

    /** Bundle/package id; defaults to {@code codename1.packageName}. */
    @Parameter(property = "package")
    private String packageName;

    /** Metadata folder; defaults to {@code <cn1-project>/cn1-metadata}. */
    @Parameter(property = "metadataDir")
    private File metadataDir;

    /** Text file name -> descriptor field. */
    private static final Map<String, String> FIELDS = new LinkedHashMap<>();
    static {
        FIELDS.put("name.txt", "name");
        FIELDS.put("subtitle.txt", "subtitle");
        FIELDS.put("description.txt", "description");
        FIELDS.put("keywords.txt", "keywords");
        FIELDS.put("promotional_text.txt", "promotionalText");
        FIELDS.put("whats_new.txt", "whatsNew");
        FIELDS.put("release_notes.txt", "whatsNew"); // alias
        FIELDS.put("marketing_url.txt", "marketingUrl");
        FIELDS.put("support_url.txt", "supportUrl");
        FIELDS.put("privacy_url.txt", "privacyPolicyUrl");
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            getLog().warn("Not a Codename One project directory; skipping metadata-push.");
            return;
        }
        String pkg = firstNonEmpty(packageName,
                properties != null ? properties.getProperty("codename1.packageName") : null);
        if (isEmpty(pkg)) {
            throw new MojoFailureException("No package id. Set codename1.packageName in "
                    + "codenameone_settings.properties or pass -Dpackage=com.example.app");
        }
        String jwt = resolveToken();
        File dir = metadataDir != null ? metadataDir : new File(getCN1ProjectDir(), "cn1-metadata");
        if (!dir.isDirectory()) {
            throw new MojoFailureException("Metadata folder not found: " + dir
                    + " (create it, or pass -DmetadataDir=...)");
        }
        List<String> stores = "both".equalsIgnoreCase(store)
                ? Arrays.asList("apple", "google") : Arrays.asList(store.toLowerCase());
        boolean pushedAny = false;
        for (String s : stores) {
            File storeDir = new File(dir, s);
            if (!storeDir.isDirectory()) {
                getLog().info("No " + s + "/ folder under " + dir + "; skipping " + s + ".");
                continue;
            }
            pushStore(baseUrl, jwt, pkg, s, storeDir);
            pushedAny = true;
        }
        if (!pushedAny) {
            getLog().warn("Nothing to push -- no apple/ or google/ folder under " + dir);
        }
    }

    private void pushStore(String base, String jwt, String pkg, String store, File storeDir)
            throws MojoExecutionException, MojoFailureException {
        String descriptor = buildDescriptor(storeDir);
        getLog().info("Pushing " + store + " metadata for " + pkg + " ...");
        String url = base + "/appsec/7.0/metadata?package=" + enc(pkg) + "&store=" + enc(store);
        HttpResult put = httpJson("PUT", url, jwt, descriptor);
        if (put.code < 200 || put.code >= 300) {
            throw new MojoFailureException("Metadata push failed (HTTP " + put.code + "): " + put.body);
        }
        pushScreenshots(base, jwt, pkg, store, storeDir);
        getLog().info(store + " metadata pushed.");
    }

    /** Build the AppMetadata JSON from the store folder. */
    private String buildDescriptor(File storeDir) throws MojoExecutionException {
        String primaryLocale = readOpt(new File(storeDir, "primary_locale.txt"));
        String primaryCategory = readOpt(new File(storeDir, "primary_category.txt"));
        String secondaryCategory = readOpt(new File(storeDir, "secondary_category.txt"));

        StringBuilder locales = new StringBuilder();
        String firstLocale = null;
        File[] localeDirs = storeDir.listFiles(File::isDirectory);
        if (localeDirs != null) {
            Arrays.sort(localeDirs);
            for (File localeDir : localeDirs) {
                String loc = localeDir.getName();
                StringBuilder fields = new StringBuilder();
                for (Map.Entry<String, String> e : FIELDS.entrySet()) {
                    String v = readOpt(new File(localeDir, e.getKey()));
                    if (v != null) {
                        appendField(fields, e.getValue(), v);
                    }
                }
                if (fields.length() == 0) {
                    continue; // a screenshots-only locale dir
                }
                if (firstLocale == null) {
                    firstLocale = loc;
                }
                if (locales.length() > 0) {
                    locales.append(',');
                }
                locales.append(jsonStr(loc)).append(":{").append(fields).append('}');
            }
        }
        if (primaryLocale == null) {
            primaryLocale = firstLocale != null ? firstLocale : "en-US";
        }
        StringBuilder json = new StringBuilder("{");
        appendField(json, "primaryLocale", primaryLocale);
        if (primaryCategory != null) {
            appendField(json, "primaryCategory", primaryCategory);
        }
        if (secondaryCategory != null) {
            appendField(json, "secondaryCategory", secondaryCategory);
        }
        json.append(",\"locales\":{").append(locales).append("}}");
        return json.toString();
    }

    /** Replace the staged set, then upload every screenshots/<type>/*.png|jpg. */
    private void pushScreenshots(String base, String jwt, String pkg, String store, File storeDir)
            throws MojoExecutionException, MojoFailureException {
        List<Shot> shots = collectShots(storeDir);
        // Clear the previous staged set so a re-push is idempotent.
        httpJson("DELETE", base + "/appsec/7.0/metadata/screenshots?package=" + enc(pkg)
                + "&store=" + enc(store), jwt, null);
        for (Shot shot : shots) {
            String url = base + "/appsec/7.0/metadata/screenshots?package=" + enc(pkg)
                    + "&store=" + enc(store) + "&locale=" + enc(shot.locale)
                    + "&displayType=" + enc(shot.displayType) + "&ordinal=" + shot.ordinal;
            HttpResult r = httpMultipart(url, jwt, shot.file);
            if (r.code < 200 || r.code >= 300) {
                throw new MojoFailureException("Screenshot upload failed for " + shot.file.getName()
                        + " (HTTP " + r.code + "): " + r.body);
            }
            getLog().info("  uploaded " + shot.locale + "/" + shot.displayType + "/" + shot.file.getName());
        }
    }

    private List<Shot> collectShots(File storeDir) {
        List<Shot> out = new ArrayList<>();
        File[] localeDirs = storeDir.listFiles(File::isDirectory);
        if (localeDirs == null) {
            return out;
        }
        Arrays.sort(localeDirs);
        for (File localeDir : localeDirs) {
            for (String dirName : new String[] {"screenshots", "images"}) {
                File shotsRoot = new File(localeDir, dirName);
                File[] typeDirs = shotsRoot.listFiles(File::isDirectory);
                if (typeDirs == null) {
                    continue;
                }
                Arrays.sort(typeDirs);
                for (File typeDir : typeDirs) {
                    File[] imgs = typeDir.listFiles((d, n) -> {
                        String l = n.toLowerCase();
                        return l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg");
                    });
                    if (imgs == null) {
                        continue;
                    }
                    Arrays.sort(imgs);
                    int ordinal = 0;
                    for (File img : imgs) {
                        out.add(new Shot(localeDir.getName(), typeDir.getName(), ordinal++, img));
                    }
                }
            }
        }
        return out;
    }

    // --- HTTP ---

    private HttpResult httpJson(String method, String url, String jwt, String body)
            throws MojoExecutionException {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(20000);
            con.setReadTimeout(60000);
            con.setRequestProperty("Authorization", "Bearer " + jwt);
            if (body != null) {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = con.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            return read(con);
        } catch (IOException e) {
            throw new MojoExecutionException("HTTP " + method + " " + url + " failed", e);
        }
    }

    private HttpResult httpMultipart(String url, String jwt, File file) throws MojoExecutionException {
        String boundary = "----cn1meta" + Long.toHexString(file.length()) + file.getName().hashCode();
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(20000);
            con.setReadTimeout(120000);
            con.setDoOutput(true);
            con.setRequestProperty("Authorization", "Bearer " + jwt);
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            String type = file.getName().toLowerCase().endsWith(".jpg")
                    || file.getName().toLowerCase().endsWith(".jpeg") ? "image/jpeg" : "image/png";
            try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + file.getName() + "\"\r\n");
                out.writeBytes("Content-Type: " + type + "\r\n\r\n");
                out.write(Files.readAllBytes(file.toPath()));
                out.writeBytes("\r\n--" + boundary + "--\r\n");
            }
            return read(con);
        } catch (IOException e) {
            throw new MojoExecutionException("Screenshot upload " + url + " failed", e);
        }
    }

    private static HttpResult read(HttpURLConnection con) throws IOException {
        int code = con.getResponseCode();
        java.io.InputStream in = code >= 400 ? con.getErrorStream() : con.getInputStream();
        String body = "";
        if (in != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > -1) {
                bos.write(buf, 0, n);
            }
            body = bos.toString("UTF-8");
        }
        con.disconnect();
        return new HttpResult(code, body);
    }

    // --- helpers ---

    private String resolveToken() throws MojoFailureException {
        Preferences prefs = Preferences.userRoot().node("/com/codename1/ui");
        String t = firstNonEmpty(token, prefs.get("token", null));
        if (isEmpty(t)) {
            throw new MojoFailureException("No Codename One token. Run 'mvn cn1:certificatewizard' "
                    + "to sign in once, or pass -Dtoken=<jwt>.");
        }
        return t;
    }

    private String readOpt(File f) throws MojoExecutionException {
        if (f == null || !f.isFile()) {
            return null;
        }
        try {
            String s = FileUtils.readFileToString(f, "UTF-8");
            // Strip a single trailing newline (editors add one); keep internal newlines.
            if (s.endsWith("\r\n")) {
                s = s.substring(0, s.length() - 2);
            } else if (s.endsWith("\n")) {
                s = s.substring(0, s.length() - 1);
            }
            return s.isEmpty() ? null : s;
        } catch (IOException e) {
            throw new MojoExecutionException("Could not read " + f, e);
        }
    }

    private static void appendField(StringBuilder sb, String field, String value) {
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '{') {
            sb.append(',');
        }
        sb.append(jsonStr(field)).append(':').append(jsonStr(value));
    }

    private static String jsonStr(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.append('"').toString();
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e); // UTF-8 is always present
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonEmpty(String... vals) {
        for (String v : vals) {
            if (!isEmpty(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static final class Shot {
        final String locale;
        final String displayType;
        final int ordinal;
        final File file;

        Shot(String locale, String displayType, int ordinal, File file) {
            this.locale = locale;
            this.displayType = displayType;
            this.ordinal = ordinal;
            this.file = file;
        }
    }

    private static final class HttpResult {
        final int code;
        final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
