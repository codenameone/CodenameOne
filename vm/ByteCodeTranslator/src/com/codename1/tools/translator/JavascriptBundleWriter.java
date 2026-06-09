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
            if (!seen.add(current.getClsName())) {
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
        // Materialise every chunk, then minify the long generated function
        // identifiers across the WHOLE bundle with one shared mapping (a function
        // defined in one chunk may be called from another). esbuild is not in the
        // pipeline, so without this the ``cn1_<pkg>_<Class>_<method>_<sig>``
        // identifiers (avg ~45 chars, the largest single contributor to bundle
        // size) ship verbatim at every definition and call site.
        java.util.List<String> chunkStrings = new java.util.ArrayList<String>(chunks.size());
        for (StringBuilder c : chunks) {
            chunkStrings.add(c.toString());
        }
        minifyGeneratedIdentifiers(chunkStrings);

        int leadCount = chunkStrings.size() - 1;
        for (int i = 0; i < leadCount; i++) {
            String suffix = leadCount >= 10 ? String.format("_%02d", i + 1) : String.format("_%d", i + 1);
            Files.write(new File(outputDirectory, "translated_app" + suffix + ".js").toPath(),
                    minifyJs(hoistStringConstants(chunkStrings.get(i))).getBytes(StandardCharsets.UTF_8));
        }
        Files.write(new File(outputDirectory, "translated_app.js").toPath(),
                minifyJs(hoistStringConstants(chunkStrings.get(chunkStrings.size() - 1))).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Renames the translator's generated function identifiers
     * ({@code cn1_<pkg>_<Class>_<method>_<sig>}) to short {@code $M*} symbols,
     * consistently across every chunk. These are bundle-internal: definitions,
     * direct/static/special call sites, and method-table values. The only
     * string reference is {@code jvm.setMain("...","cn1_..._main_...")} consumed
     * via {@code global[this.mainMethod]} -- the whole-token rewrite updates that
     * string in lockstep with its definition, so dispatch still resolves.
     *
     * <p>Safe because: the renamed set is exactly the identifiers that have a
     * {@code function[*] cn1_X(} definition in the bundle (so runtime-provided
     * natives, which are bindNative'd and never defined here, keep their names);
     * virtual dispatch keys are the distinct {@code cn1_s_*} strings (not in the
     * set); field names carry no signature suffix and are never function
     * definitions; and {@code $M} is a fresh prefix the mangler never emits.
     * Kill switch: {@code -Dparparvm.js.minify.idents.off}.
     */
    private static void minifyGeneratedIdentifiers(java.util.List<String> chunkStrings) {
        if (System.getProperty("parparvm.js.minify.idents.off") != null) {
            return;
        }
        java.util.regex.Pattern defPattern = java.util.regex.Pattern.compile(
                "function\\*?\\s+(cn1_[A-Za-z0-9_]+)\\s*\\(");
        java.util.TreeSet<String> defs = new java.util.TreeSet<String>();
        for (String chunk : chunkStrings) {
            java.util.regex.Matcher m = defPattern.matcher(chunk);
            while (m.find()) {
                String name = m.group(1);
                // Constructors / class initialisers are reconstructed by string at
                // runtime (global["cn1_"+className+"___INIT__"], the clinit id, etc.,
                // in parparvm_runtime.js), so renaming them would break global[]
                // resolution. Keep their conventional names.
                if (name.contains("___INIT__") || name.contains("___CLINIT__")) {
                    continue;
                }
                defs.add(name);
            }
        }
        if (defs.isEmpty()) {
            return;
        }
        // Any cn1_ token referenced as a string literal must NOT be renamed:
        //  - in the app bundle: jvm.setMain's main method, field-list manifests;
        //  - in the runtime/port JS: bindNative([...]) override targets and any
        //    global["cn1_..."] / nativeMethods[...] lookup. The JS<->worker bridge
        //    overrides native methods by reassigning the global of that exact name
        //    (CN1 has no reflection/serialization; this naming IS the binding), so
        //    a renamed static native stub would bypass its override and return its
        //    placeholder (e.g. null) -> NPE. Scan the bundle AND the runtime sources
        //    so every bridge-resolved name keeps its canonical identifier.
        java.util.List<String> stringScanSources = new java.util.ArrayList<String>(chunkStrings);
        for (String runtimeSrc : loadRuntimeNameSources()) {
            stringScanSources.add(runtimeSrc);
        }
        java.util.Set<String> stringTokens = collectStringLiteralCn1Tokens(stringScanSources);
        // installNativeBindings overrides BOTH global[name] and the CONSTRUCTED
        // global[name + "__impl"] (the static-method body) -- see parparvm_runtime.js.
        // The "__impl" variant never appears as a literal string, so add it for every
        // protected base name; otherwise a renamed static-native body bypasses its
        // override and returns its placeholder (e.g. null) -> NPE.
        java.util.Set<String> excluded = new java.util.HashSet<String>(stringTokens);
        for (String t : stringTokens) {
            excluded.add(t + "__impl");
        }
        // Authoritative: every native method the translator emitted (the bridge's
        // override targets, by name) -- more reliable than scanning runtime JS text.
        excluded.addAll(JavascriptMethodGenerator.NATIVE_METHOD_IDENTIFIERS);
        defs.removeAll(excluded);
        // Prefix protection: some bridge names are CONSTRUCTED at runtime by string
        // concatenation, so the full identifier never appears as a literal -- only
        // its stem does. The screenshot runner (port.js) builds
        //   "cn1_..._Cn1ssDeviceRunner_lambda_" + methodName + "_" + i + "_" + sig
        // so the scanned literal is the stem ".._lambda_" while the generated def is
        // ".._lambda_runNextTest_2_<sig>". Treat every scanned cn1_ string token as a
        // prefix and protect any def that extends it at an identifier-segment boundary
        // (the next char is '_', or the stem already ends in '_'), so the constructed
        // name still resolves after minification. Over-protecting only forgoes size;
        // under-protecting breaks a name-resolved bridge -> wedge.
        // Only class-qualified stems are eligible as prefixes. The generic
        // construction roots "cn1_" (4, completes to ___INIT__/___CLINIT__, already
        // skipped above) and "cn1_s_" (6, dispatch-ids resolved via the _qX table,
        // never a function def) are short and would over-match -- "cn1_" as a prefix
        // matches EVERY def and would disable all minification. A length floor keeps
        // those out while admitting genuine fully-qualified stems (the only real one,
        // the screenshot runner's lambda stem, is 77 chars).
        final int MIN_PREFIX_PROTECT_LEN = 16;
        if (!defs.isEmpty()) {
            java.util.List<String> prefixTokens = new java.util.ArrayList<String>();
            for (String t : stringTokens) {
                if (t.length() >= MIN_PREFIX_PROTECT_LEN) {
                    prefixTokens.add(t);
                }
            }
            if (!prefixTokens.isEmpty()) {
                java.util.Iterator<String> it = defs.iterator();
                while (it.hasNext()) {
                    String d = it.next();
                    for (String t : prefixTokens) {
                        if (d.length() > t.length() && d.startsWith(t)
                                && (t.endsWith("_") || d.charAt(t.length()) == '_')) {
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
        if (defs.isEmpty()) {
            return;
        }
        java.util.Map<String, String> map = new java.util.HashMap<String, String>(defs.size() * 2);
        int idx = 0;
        for (String d : defs) {
            map.put(d, shortIdentifier(idx++));
        }
        for (int i = 0; i < chunkStrings.size(); i++) {
            chunkStrings.set(i, renameTokens(chunkStrings.get(i), map));
        }
    }

    /** {@code $M} + base-26 (a..z, aa..) — a prefix the bytecode mangler never produces. */
    private static String shortIdentifier(int n) {
        StringBuilder sb = new StringBuilder();
        do {
            sb.insert(0, (char) ('a' + (n % 26)));
            n = n / 26 - 1;
        } while (n >= 0);
        return "$M" + sb;
    }

    /**
     * Single O(n) pass replacing each maximal identifier token present in
     * {@code map}, but NEVER inside a string/template literal -- string-referenced
     * names are excluded from {@code map} (see caller) and their string spellings
     * must be left intact. Tokens are {@code [A-Za-z0-9_$]} runs.
     */
    private static String renameTokens(String src, java.util.Map<String, String> map) {
        int n = src.length();
        StringBuilder out = new StringBuilder(n);
        int i = 0;
        char inString = 0;
        while (i < n) {
            char c = src.charAt(i);
            if (inString != 0) {
                out.append(c);
                if (c == '\\' && i + 1 < n) {
                    out.append(src.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == inString) {
                    inString = 0;
                }
                i++;
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                inString = c;
                out.append(c);
                i++;
                continue;
            }
            boolean idStart = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
            if (idStart) {
                int j = i + 1;
                while (j < n) {
                    char d = src.charAt(j);
                    if ((d >= 'a' && d <= 'z') || (d >= 'A' && d <= 'Z')
                            || (d >= '0' && d <= '9') || d == '_' || d == '$') {
                        j++;
                    } else {
                        break;
                    }
                }
                String token = src.substring(i, j);
                String repl = map.get(token);
                out.append(repl != null ? repl : token);
                i = j;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * Collect every {@code cn1_*} identifier token that appears inside a string or
     * template literal across all chunks. These are names referenced by string at
     * runtime (setMain's main method, serialized field manifests, reflection), so
     * they must not be renamed in code.
     */
    private static java.util.Set<String> collectStringLiteralCn1Tokens(java.util.List<String> chunkStrings) {
        java.util.Set<String> tokens = new java.util.HashSet<String>();
        for (String src : chunkStrings) {
            int n = src.length();
            int i = 0;
            char inString = 0;
            while (i < n) {
                char c = src.charAt(i);
                if (inString != 0) {
                    if (c == '\\' && i + 1 < n) {
                        i += 2;
                        continue;
                    }
                    if (c == inString) {
                        inString = 0;
                        i++;
                        continue;
                    }
                    if (c == 'c' && src.startsWith("cn1_", i)
                            && (i == 0 || !isIdentChar(src.charAt(i - 1)))) {
                        int j = i + 4;
                        while (j < n && isIdentChar(src.charAt(j))) {
                            j++;
                        }
                        tokens.add(src.substring(i, j));
                        i = j;
                        continue;
                    }
                    i++;
                    continue;
                }
                if (c == '"' || c == '\'' || c == '`') {
                    inString = c;
                }
                i++;
            }
        }
        return tokens;
    }

    /**
     * Returns the JS sources that resolve translated functions by name (the
     * worker-side runtime + JS port) so their referenced {@code cn1_*} names are
     * protected from renaming. Missing sources are skipped -- protecting fewer
     * names only forgoes some size, it never produces an unsafe rename (the app
     * bundle's own string scan still covers anything it references).
     */
    private static java.util.List<String> loadRuntimeNameSources() {
        java.util.List<String> sources = new java.util.ArrayList<String>();
        for (String res : new String[]{ "parparvm_runtime.js", "browser_bridge.js" }) {
            try {
                sources.add(loadResource(res));
            } catch (IOException ignore) {
                // resource absent -- skip
            }
        }
        try {
            Path webApp = locateJavaScriptPortWebApp();
            if (webApp != null) {
                Path portJs = webApp.resolve("port.js");
                if (java.nio.file.Files.exists(portJs)) {
                    sources.add(new String(java.nio.file.Files.readAllBytes(portJs), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignore) {
            // port.js unavailable -- skip
        }
        return sources;
    }

    private static boolean isIdentChar(char d) {
        return (d >= 'a' && d <= 'z') || (d >= 'A' && d <= 'Z')
                || (d >= '0' && d <= '9') || d == '_' || d == '$';
    }

    /**
     * Strips the translator's pretty-printing indentation and blank lines from the
     * emitted application JS. The translator emits one statement per line with
     * generous indentation for readability; for a deployed bundle that is ~20% dead
     * weight that the browser must still download and parse. We keep one statement
     * per line (newlines preserved) so the transform is safe regardless of ASI or
     * {@code //} comments -- only leading/trailing line whitespace and empty lines
     * are removed. Set {@code -Dparparvm.js.pretty=true} to keep the readable form
     * for debugging the generated code.
     */
    private static String minifyJs(String code) {
        if (System.getProperty("parparvm.js.pretty") != null) {
            return code;
        }
        int n = code.length();
        StringBuilder out = new StringBuilder(n);
        int i = 0;
        while (i < n) {
            int eol = code.indexOf('\n', i);
            if (eol < 0) {
                eol = n;
            }
            int start = i;
            int end = eol;
            while (start < end && code.charAt(start) <= ' ') {
                start++;
            }
            while (end > start && code.charAt(end - 1) <= ' ') {
                end--;
            }
            if (end > start) {
                out.append(code, start, end).append('\n');
            }
            i = eol + 1;
        }
        return out.toString();
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
        // The translator's mangle scheme also emits {@code $}-prefixed
        // names ({@code $a}, {@code $XX}, ...) and these appear inside
        // quoted strings as args to {@code _O("$Xx")} class lookups
        // and dispatch-id args to {@code cn1_iv*}. They share the same
        // safety property as {@code [A-Za-z0-9_]} bodies (no escape
        // sequences possible), so include {@code $} in the hoistable
        // alphabet to widen coverage.
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_'
                || c == '$';
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
            } else if (c == ',') {
                // Object-key alias rewrite: when an unquoted body that
                // already has an alias appears as a key inside an object
                // literal (``,KEY:VAL``), rewrite it to the computed
                // form ``,[ALIAS]:VAL``. Saves
                // ``len(KEY) - len(ALIAS) - 2`` bytes per occurrence.
                //
                // Why ``,`` only and not ``{``: ``{ KEY: ... }`` is also
                // valid as a *block* containing a labeled statement, and
                // the translator emits both. After ``,`` we're always
                // inside a list context (function args, array, obj
                // literal); only obj literals accept ``KEY:`` shape, so
                // matching after ``,`` is unambiguous. This skips the
                // first key of each object literal but keeps every
                // subsequent key, which is enough to recover most of
                // the byte savings on the ``_Z({m:{a:fn,b:fn,...}})``
                // class-table entries that dominate the obj-key uses.
                out.append(c);
                int peek = i + 1;
                while (peek < n) {
                    char d = src.charAt(peek);
                    if (!isHoistableIdentChar(d)) break;
                    peek++;
                }
                int bodyLen = peek - i - 1;
                if (bodyLen >= 4 && peek < n && src.charAt(peek) == ':') {
                    String body = src.substring(i + 1, peek);
                    String alias = aliases.get(body);
                    if (alias != null && (long) (body.length() - alias.length() - 2) > 0) {
                        out.append('[').append(alias).append(']');
                        i = peek;
                        continue;
                    }
                }
                i++;
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
                } else if (isNativeInterfaceStub(file)) {
                    // Native-interface implementations run on the MAIN thread (index.html),
                    // not in the worker -- they need DOM access. Skip them here.
                    continue;
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
        String index = loadResource("index.html");
        StringBuilder stubs = new StringBuilder();
        for (String stub : collectNativeInterfaceStubs(outputDirectory)) {
            stubs.append("<script src=\"").append(stub).append("\"></script>\n");
        }
        index = index.replace("<!--__NATIVE_INTERFACE_STUBS__-->", stubs.toString().trim());
        Files.write(new File(outputDirectory, "index.html").toPath(), index.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Native-interface JS implementations self-register into
     * {@code cn1_native_interfaces} (they end with {@code })(cn1_get_native_interfaces());}).
     * They run on the MAIN thread so their DOM access works, and are dispatched from the
     * worker via the host-call bridge. Identify them by that content marker so the worker
     * importScripts list excludes them and index.html loads them on the page instead.
     */
    private static List<String> collectNativeInterfaceStubs(File outputDirectory) {
        List<String> stubs = new ArrayList<String>();
        File[] files = outputDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(".js") && isNativeInterfaceStub(file)) {
                    stubs.add(file.getName());
                }
            }
        }
        Collections.sort(stubs);
        return stubs;
    }

    private static boolean isNativeInterfaceStub(File jsFile) {
        String name = jsFile.getName();
        if ("parparvm_runtime.js".equals(name)
                || "translated_app.js".equals(name)
                || "worker.js".equals(name)
                || "sw.js".equals(name)
                || "port.js".equals(name)
                || "browser_bridge.js".equals(name)
                || name.startsWith("translated_app_")) {
            return false;
        }
        try {
            String content = new String(Files.readAllBytes(jsFile.toPath()), StandardCharsets.UTF_8);
            return content.contains("cn1_get_native_interfaces");
        } catch (IOException ex) {
            return false;
        }
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

    /**
     * Every {@code cn1_*} token referenced as a string literal by the
     * hand-written bridge JS (parparvm_runtime.js, browser_bridge.js and
     * the JavaScript port's port.js). These are names the bridge resolves
     * by string at runtime -- and, in the {@code bindNative} /
     * {@code bindCiFallback} case, REPLACES with {@code function*}
     * overrides. The suspension analysis must treat the named methods as
     * suspending: a translated caller that skipped {@code yield*} (because
     * the static body looked synchronous) would receive the installed
     * override's raw generator object as its "result" and the override
     * body would never run.
     */
    static Set<String> collectBridgeReferencedCn1Tokens() {
        Set<String> tokens = new HashSet<String>();
        List<String> sources = new ArrayList<String>();
        for (String res : new String[]{ "parparvm_runtime.js", "browser_bridge.js" }) {
            try {
                sources.add(loadResource(res));
            } catch (IOException ignore) {
                // resource absent -- skip
            }
        }
        try {
            Path webApp = locateJavaScriptPortWebApp();
            if (webApp != null) {
                Path portJs = webApp.resolve("port.js");
                if (Files.exists(portJs)) {
                    sources.add(new String(Files.readAllBytes(portJs), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignore) {
            // port.js unavailable -- skip
        }
        java.util.regex.Pattern literal = java.util.regex.Pattern.compile("[\"'](cn1_[A-Za-z0-9_]+)[\"']");
        for (String src : sources) {
            java.util.regex.Matcher m = literal.matcher(src);
            while (m.find()) {
                tokens.add(m.group(1));
            }
        }
        return tokens;
    }
}
