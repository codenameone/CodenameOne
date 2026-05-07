package com.codename1.tools.translator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class JavascriptBundleWriter {
    private static final String RESOURCE_ROOT = "/javascript/";

    private JavascriptBundleWriter() {
    }

    static void write(File outputDirectory, List<ByteCodeClass> classes) throws IOException {
        writeRuntime(outputDirectory);
        writeTranslatedClasses(outputDirectory, classes);
        copyJavaScriptPortWebAppAssets(outputDirectory);
        writeWorker(outputDirectory);
        writeBrowserBridge(outputDirectory);
        writeIndex(outputDirectory);
        writeProtocol(outputDirectory);
        writeJsoBridgeManifest(outputDirectory, classes);
    }

    /**
     * Emit a sidecar manifest listing every signature-based dispatch id
     * (``cn1_s_<method>_<sig>``) that corresponds to a method declared on
     * a JSO bridge class — i.e. any class transitively assignable to
     * ``com_codename1_html5_js_JSObject``. The mangle script reads this
     * file to keep these dispatch ids unmangled, otherwise call sites
     * end up reaching ``invokeJsoBridge`` with a ``$``-prefixed mangled
     * member name and the host throws ``Missing JS member $X for host
     * receiver`` at the first DOM bridge call.
     *
     * <p>The structural-optimization landing made the translator switch
     * from per-class ``cn1_<class>_<method>_<sig>`` ids to a class-free
     * ``cn1_s_<method>_<sig>`` form for INVOKEVIRTUAL / INVOKEINTERFACE
     * call sites. The legacy form was naturally name-spaced by the
     * class portion (the mangle script uses ``cn1_<jsoClass>_*`` as
     * the exclusion key), but the new form drops the class entirely
     * and flows alongside ordinary identifiers — without a manifest
     * the mangle pass can't tell which sig-based ids belong to JSO
     * bridge interfaces.
     */
    private static void writeJsoBridgeManifest(File outputDirectory, List<ByteCodeClass> classes) throws IOException {
        Map<String, ByteCodeClass> byName = new HashMap<String, ByteCodeClass>();
        for (ByteCodeClass cls : classes) {
            byName.put(cls.getClsName(), cls);
        }
        Set<String> dispatchIds = new TreeSet<String>();
        for (ByteCodeClass cls : classes) {
            if (!isJsoBridgeClass(cls, byName)) {
                continue;
            }
            for (BytecodeMethod m : cls.getMethods()) {
                if (m.isEliminated() || m.isStatic()) {
                    continue;
                }
                String name = m.getMethodName();
                String desc = m.getSignature();
                if (name == null || desc == null) {
                    continue;
                }
                dispatchIds.add(JavascriptNameUtil.dispatchMethodIdentifier(name, desc));
            }
        }
        StringBuilder out = new StringBuilder();
        for (String id : dispatchIds) {
            out.append(id).append('\n');
        }
        Files.write(new File(outputDirectory, "jso-bridge-dispatch-ids.txt").toPath(),
                out.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isJsoBridgeClass(ByteCodeClass cls, Map<String, ByteCodeClass> byName) {
        Set<String> seen = new HashSet<String>();
        Deque<ByteCodeClass> stack = new ArrayDeque<ByteCodeClass>();
        stack.push(cls);
        while (!stack.isEmpty()) {
            ByteCodeClass current = stack.pop();
            if (current == null || !seen.add(current.getClsName())) {
                continue;
            }
            if ("com_codename1_html5_js_JSObject".equals(current.getClsName())) {
                return true;
            }
            String base = current.getBaseClass();
            if (base != null) {
                ByteCodeClass baseObj = byName.get(JavascriptNameUtil.sanitizeClassName(base));
                if (baseObj != null) {
                    stack.push(baseObj);
                }
            }
            if (current.getBaseInterfaces() != null) {
                for (String iface : current.getBaseInterfaces()) {
                    ByteCodeClass ifaceObj = byName.get(JavascriptNameUtil.sanitizeClassName(iface));
                    if (ifaceObj != null) {
                        stack.push(ifaceObj);
                    }
                }
            }
        }
        return false;
    }

    private static void writeRuntime(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "parparvm_runtime.js", "parparvm_runtime.js");
    }

    /**
     * Cap on how large any single emitted class-definitions file may grow
     * before we start a new chunk. Cloudflare Pages rejects uploads with any
     * individual file larger than ~25 MiB, so we stay comfortably under that
     * while keeping the chunk count small. The chunks are concatenated at
     * load time via the worker's generated importScripts list.
     */
    private static final int CLASS_CHUNK_MAX_BYTES = 20 * 1024 * 1024;

    private static void writeTranslatedClasses(File outputDirectory, List<ByteCodeClass> classes) throws IOException {
        List<ByteCodeClass> sorted = new ArrayList<ByteCodeClass>(classes);
        Collections.sort(sorted, new Comparator<ByteCodeClass>() {
            @Override
            public int compare(ByteCodeClass a, ByteCodeClass b) {
                int priorityDiff = bootstrapPriority(a) - bootstrapPriority(b);
                if (priorityDiff != 0) {
                    return priorityDiff;
                }
                return a.getClsName().compareTo(b.getClsName());
            }
        });

        // Stream class bodies into bounded chunks. We materialise every chunk
        // but the last one as translated_app_NN.js; the final chunk lands at
        // translated_app.js and carries the jvm.setMain(...) tail so that
        // call always runs after every class has been registered (writeWorker
        // imports translated_app.js last).
        List<StringBuilder> chunks = new ArrayList<StringBuilder>();
        StringBuilder current = new StringBuilder();
        chunks.add(current);
        for (ByteCodeClass cls : sorted) {
            String code = cls.generateJavascriptCode(classes);
            if (current.length() > 0 && current.length() + code.length() > CLASS_CHUNK_MAX_BYTES) {
                current = new StringBuilder();
                chunks.add(current);
            }
            current.append(code).append('\n');
        }

        StringBuilder tail = chunks.get(chunks.size() - 1);
        ByteCodeClass mainClass = ByteCodeClass.getMainClass();
        if (mainClass != null) {
            tail.append("jvm.setMain(\"").append(mainClass.getClsName()).append("\", \"")
                    .append(JavascriptNameUtil.methodIdentifier(mainClass.getClsName(), "main", "([Ljava/lang/String;)V"))
                    .append("\");\n");
        }

        // Lead chunks use zero-padded suffixes so writeWorker's lexicographic
        // scan of top-level *.js files imports them in the intended order
        // (they're all independent class definitions so the relative order
        // among them doesn't matter for correctness, but stable ordering
        // keeps debug output deterministic).
        int leadCount = chunks.size() - 1;
        for (int i = 0; i < leadCount; i++) {
            String suffix = leadCount >= 10 ? String.format("_%02d", i + 1) : String.format("_%d", i + 1);
            Files.write(new File(outputDirectory, "translated_app" + suffix + ".js").toPath(),
                    hoistStringConstants(chunks.get(i).toString()).getBytes(StandardCharsets.UTF_8));
        }
        Files.write(new File(outputDirectory, "translated_app.js").toPath(),
                hoistStringConstants(tail.toString()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Identifier-character set used to detect hoistable string bodies.
     * A body matches if every char satisfies these rules AND length >= 4.
     * The character set deliberately excludes anything that needs JS
     * escaping ({@code \"}, {@code \\}, etc.) so a literal text-level
     * substitution of {@code "BODY"} -> alias is byte-equivalent to a
     * JS-aware rewrite -- the hoist pass cannot accidentally truncate or
     * splice an escaped string.
     */
    private static boolean isHoistableIdentChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_';
    }

    /**
     * Repeated long string literals (mostly JNI-form dispatch ids like
     * {@code "cn1_s_getHeight_R_int"} -- ~750 occurrences -- and the
     * shorter {@code "com_codename1_html5_js_JSObject"} markers used by
     * runtime helpers) make up roughly 100 KiB of the emitted bundle.
     * Hoist the most-used pure-identifier strings to const aliases at
     * the head of the chunk and substitute the literal occurrences with
     * the alias name.
     *
     * <p>Why pure identifiers only: a body containing escape characters
     * could theoretically be the rest of a different string after an
     * escape sequence we don't decode, so restricting to
     * {@code [A-Za-z0-9_]+} keeps the byte-level substitution provably
     * safe -- the literal {@code "BODY"} cannot appear inside a
     * different JS string, regex, or template, because every other
     * string-bearing token contains either a closing delimiter or an
     * escape we'd notice.
     *
     * <p>Why a const alias prelude: esbuild minification is not part of
     * the JS-port build pipeline, so identifiers and string literals
     * ship verbatim. A {@code const} declared at top of the chunk is in
     * scope for every translated method body and the {@code _Z(...)}
     * class registrations that follow, with no runtime overhead beyond
     * a one-time const binding.
     *
     * <p>Aliases use the {@code _q*} prefix, which the byte-code-to-JS
     * mangle scheme has never produced (see existing usages of
     * {@code _O}, {@code _L}, {@code _Z} in parparvm_runtime.js); the
     * generator names start with {@code $} or a letter.
     */
    private static String hoistStringConstants(String src) {
        // First pass: walk the source, find every "..." literal that is
        // a pure identifier of length >= 4. Skip single-quoted strings
        // and template literals (JS-port currently emits a few of each
        // -- see iOS7Theme CSS strings -- and we mustn't recurse into
        // their content). We don't strip comments because the translator
        // never emits comments.
        int n = src.length();
        Map<String, Integer> counts = new HashMap<String, Integer>();
        int i = 0;
        while (i < n) {
            char c = src.charAt(i);
            if (c == '"') {
                int j = i + 1;
                boolean pure = true;
                while (j < n) {
                    char d = src.charAt(j);
                    if (d == '\\') {
                        pure = false;
                        j += 2;
                        continue;
                    }
                    if (d == '"') {
                        break;
                    }
                    if (!isHoistableIdentChar(d)) {
                        pure = false;
                    }
                    j++;
                }
                if (j >= n) {
                    break;
                }
                int bodyLen = j - i - 1;
                if (pure && bodyLen >= 4) {
                    String body = src.substring(i + 1, j);
                    Integer prev = counts.get(body);
                    counts.put(body, prev == null ? 1 : prev + 1);
                }
                i = j + 1;
            } else if (c == '\'') {
                // Skip single-quoted string body without inspecting it.
                int j = i + 1;
                while (j < n) {
                    char d = src.charAt(j);
                    if (d == '\\') { j += 2; continue; }
                    if (d == '\'') break;
                    j++;
                }
                if (j >= n) break;
                i = j + 1;
            } else if (c == '`') {
                // Skip template literal body. Interpolations ${...} are
                // JS code that may contain its own quoted strings; we
                // don't recurse for simplicity. Translated_app.js only
                // contains a handful of plain `...` literals (no ${}).
                int j = i + 1;
                while (j < n) {
                    char d = src.charAt(j);
                    if (d == '\\') { j += 2; continue; }
                    if (d == '`') break;
                    j++;
                }
                if (j >= n) break;
                i = j + 1;
            } else {
                i++;
            }
        }

        // Pick aliases for the bodies whose hoist net is positive,
        // sorted by descending byte savings so the highest-value strings
        // get the shortest aliases.
        List<Map.Entry<String, Integer>> sorted = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                long sa = (long) (a.getKey().length() - 1) * a.getValue();
                long sb = (long) (b.getKey().length() - 1) * b.getValue();
                if (sa != sb) {
                    return sa < sb ? 1 : -1;
                }
                return a.getKey().compareTo(b.getKey());
            }
        });
        Map<String, String> aliases = new HashMap<String, String>();
        StringBuilder prelude = new StringBuilder();
        int aliasIdx = 0;
        for (Map.Entry<String, Integer> e : sorted) {
            String body = e.getKey();
            int uses = e.getValue();
            if (uses < 2) {
                continue;
            }
            String alias = computeAlias(aliasIdx);
            int aliasLen = alias.length();
            // Each occurrence saves (body.length() + 2 - aliasLen) bytes
            // (literal "BODY" -> alias). One-time cost is the const
            // entry: ',ALIAS="BODY"' = aliasLen + body.length() + 4.
            long saving = (long) (body.length() + 2 - aliasLen) * uses
                    - (aliasLen + body.length() + 4);
            if (saving <= 0) {
                continue;
            }
            aliases.put(body, alias);
            if (prelude.length() == 0) {
                prelude.append("const ");
            } else {
                prelude.append(',');
            }
            prelude.append(alias).append("=\"").append(body).append('"');
            aliasIdx++;
        }
        if (aliases.isEmpty()) {
            return src;
        }
        prelude.append(";\n");

        // Second pass: rebuild the file, substituting "BODY" -> alias
        // wherever we previously matched a hoistable double-quoted body.
        // Walk the same way we did for counting so we never substitute
        // inside single-quoted, template, or unhoistable strings.
        StringBuilder out = new StringBuilder(src.length());
        out.append(prelude);
        i = 0;
        while (i < n) {
            char c = src.charAt(i);
            if (c == '"') {
                int j = i + 1;
                boolean pure = true;
                while (j < n) {
                    char d = src.charAt(j);
                    if (d == '\\') {
                        pure = false;
                        j += 2;
                        continue;
                    }
                    if (d == '"') {
                        break;
                    }
                    if (!isHoistableIdentChar(d)) {
                        pure = false;
                    }
                    j++;
                }
                if (j >= n) {
                    out.append(src, i, n);
                    break;
                }
                int bodyLen = j - i - 1;
                String alias = null;
                if (pure && bodyLen >= 4) {
                    String body = src.substring(i + 1, j);
                    alias = aliases.get(body);
                }
                if (alias != null) {
                    out.append(alias);
                } else {
                    out.append(src, i, j + 1);
                }
                i = j + 1;
            } else if (c == '\'') {
                int j = i + 1;
                while (j < n) {
                    char d = src.charAt(j);
                    if (d == '\\') { j += 2; continue; }
                    if (d == '\'') break;
                    j++;
                }
                if (j >= n) { out.append(src, i, n); break; }
                out.append(src, i, j + 1);
                i = j + 1;
            } else if (c == '`') {
                int j = i + 1;
                while (j < n) {
                    char d = src.charAt(j);
                    if (d == '\\') { j += 2; continue; }
                    if (d == '`') break;
                    j++;
                }
                if (j >= n) { out.append(src, i, n); break; }
                out.append(src, i, j + 1);
                i = j + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Generate an alias name for the given index, drawing from a base-62
     * digit stream prefixed with {@code _q}. The prefix has never been
     * emitted by the translator's identifier scheme, so collisions with
     * generator-emitted locals or class-method short names are
     * structurally impossible -- {@code parparvm_runtime.js} uses
     * {@code _O}, {@code _L}, {@code _T}, {@code _I}, {@code _Z} and
     * the per-method renamer uses single ASCII letters and {@code $X}.
     */
    private static String computeAlias(int idx) {
        // Base-62 digits: 0-9 a-z A-Z. Single suffix gives 62 aliases
        // (_q0 .. _qZ); double suffix gives 62*62 = 3844 more.
        String digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (idx < 62) {
            return "_q" + digits.charAt(idx);
        }
        idx -= 62;
        if (idx < 62 * 62) {
            return "_q" + digits.charAt(idx / 62) + digits.charAt(idx % 62);
        }
        idx -= 62 * 62;
        return "_q" + digits.charAt(idx / (62 * 62)) + digits.charAt((idx / 62) % 62) + digits.charAt(idx % 62);
    }

    private static int bootstrapPriority(ByteCodeClass cls) {
        String name = cls.getClsName();
        if ("java_lang_Object".equals(name)) {
            return 0;
        }
        if ("java_lang_Class".equals(name)) {
            return 1;
        }
        if ("java_lang_String".equals(name)) {
            return 2;
        }
        if ("java_lang_Throwable".equals(name)) {
            return 3;
        }
        if (name.startsWith("java_lang_String_")) {
            return 4;
        }
        if (name.startsWith("java_lang_")) {
            return 5;
        }
        return 10;
    }

    private static void writeWorker(File outputDirectory) throws IOException {
        List<String> nativeScripts = new ArrayList<String>();
        List<String> classChunkScripts = new ArrayList<String>();
        File[] files = outputDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (!name.endsWith(".js")) {
                    continue;
                }
                if ("parparvm_runtime.js".equals(name)
                        || "translated_app.js".equals(name)
                        || "worker.js".equals(name)
                        || "sw.js".equals(name)
                        || "browser_bridge.js".equals(name)) {
                    continue;
                }
                // translated_app_NN.js are class-definition chunks split off
                // from translated_app.js for Cloudflare Pages' per-file size
                // limit. Group them separately so they load *before*
                // translated_app.js (which contains the trailing jvm.setMain
                // call) but *after* other runtime helpers / native shims.
                if (name.startsWith("translated_app_") && name.endsWith(".js")) {
                    classChunkScripts.add(name);
                } else {
                    nativeScripts.add(name);
                }
            }
        }
        // Deterministic order across OSes — listFiles() doesn't guarantee any.
        Collections.sort(nativeScripts);
        Collections.sort(classChunkScripts);

        StringBuilder imports = new StringBuilder();
        imports.append("importScripts('parparvm_runtime.js');\n");
        for (String script : nativeScripts) {
            imports.append("importScripts('").append(script).append("');\n");
        }
        for (String script : classChunkScripts) {
            imports.append("importScripts('").append(script).append("');\n");
        }
        imports.append("importScripts('translated_app.js');\n");

        String worker = loadResource("worker.js").replace("/*__IMPORTS__*/", imports.toString().trim());
        Files.write(new File(outputDirectory, "worker.js").toPath(), worker.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeIndex(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "index.html", "index.html");
    }

    private static void writeBrowserBridge(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "browser_bridge.js", "browser_bridge.js");
    }

    private static void writeProtocol(File outputDirectory) throws IOException {
        writeResource(outputDirectory, "vm_protocol.md", "vm_protocol.md");
    }

    private static void copyJavaScriptPortWebAppAssets(File outputDirectory) throws IOException {
        Path webApp = locateJavaScriptPortWebApp();
        if (webApp == null) {
            return;
        }
        copyPathIfPresent(webApp.resolve("js"), outputDirectory.toPath().resolve("js"));
        copyPathIfPresent(webApp.resolve("css"), outputDirectory.toPath().resolve("css"));
        copyPathIfPresent(webApp.resolve("assets"), outputDirectory.toPath().resolve("assets"));
        copyPathIfPresent(webApp.resolve("style.css"), outputDirectory.toPath().resolve("style.css"));
        copyPathIfPresent(webApp.resolve("progress.gif"), outputDirectory.toPath().resolve("progress.gif"));
        copyPathIfPresent(webApp.resolve("manifest.json"), outputDirectory.toPath().resolve("manifest.json"));
        copyPathIfPresent(webApp.resolve("sw.js"), outputDirectory.toPath().resolve("sw.js"));
        copyPathIfPresent(webApp.resolve("port.js"), outputDirectory.toPath().resolve("port.js"));
    }

    private static Path locateJavaScriptPortWebApp() {
        String override = System.getProperty("codename1.javascriptport.webapp");
        if (override != null && !override.trim().isEmpty()) {
            Path path = Paths.get(override.trim());
            if (Files.isDirectory(path)) {
                return path;
            }
        }

        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(Paths.get("Ports", "JavaScriptPort", "src", "main", "webapp"));
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void copyPathIfPresent(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
                for (Path child : stream) {
                    Path childName = child.getFileName();
                    if (childName != null) {
                        copyPathIfPresent(child, target.resolve(childName.toString()));
                    }
                }
            }
            return;
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeResource(File outputDirectory, String targetName, String resourceName) throws IOException {
        Files.write(new File(outputDirectory, targetName).toPath(),
                loadResource(resourceName).getBytes(StandardCharsets.UTF_8));
    }

    private static String loadResource(String resourceName) throws IOException {
        InputStream input = JavascriptBundleWriter.class.getResourceAsStream(RESOURCE_ROOT + resourceName);
        if (input == null) {
            throw new IOException("Missing javascript backend resource " + resourceName);
        }
        try {
            byte[] data = new byte[8192];
            StringBuilder out = new StringBuilder();
            int len;
            while ((len = input.read(data)) > -1) {
                out.append(new String(data, 0, len, StandardCharsets.UTF_8));
            }
            return out.toString();
        } finally {
            input.close();
        }
    }
}
