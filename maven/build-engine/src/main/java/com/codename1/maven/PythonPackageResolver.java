/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven;

import com.codename1.build.Log;
import com.codename1.builders.BuildException;
import com.codename1.python.transpiler.api.PythonTranspiler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Provides a Python project's PyPI requirements as source, for
/// `cn1:import-python-project`.
///
/// Every requirement, and every requirement of those, is resolved against
/// PyPI's JSON API to the newest release its specifiers accept, downloaded,
/// checked against the SHA-256 PyPI publishes, and unpacked into
/// `src/main/python-packages`, which the build transpiles beside
/// `src/main/python`. A package the runtime provides itself (the standard
/// library, and the packages Codename One maps: `requests`, `numpy`,
/// `flask` and the rest) is not downloaded.
///
/// What was chosen is written to `python.lock` in that directory -- name,
/// version, file and hash -- and a later import installs exactly those,
/// so the packages change only when the lock is deleted or a requirement
/// no longer accepts its pinned version.
///
/// **C extensions.** A package that ships compiled modules is supported
/// the same way: its Python is transpiled, and each compiled module is
/// replaced by Java. [PythonExtensionStubs] finds the modules and the names
/// the package uses from each, and writes a Java class per module with a
/// stub for each name, to be filled in.
public final class PythonPackageResolver {
    /// The directory, beside `src/main/python`, the packages are unpacked into.
    public static final String PACKAGES_DIR = "python-packages";
    /// The lock file in [#PACKAGES_DIR].
    public static final String LOCK = "python.lock";

    /// Fetches a URL's bytes; replaced in tests.
    public interface Fetcher {
        byte[] fetch(String url) throws IOException;
    }

    /// One distribution as installed.
    static final class Pinned {
        final String name;
        final String version;
        final String file;
        final String url;
        final String sha256;

        Pinned(String name, String version, String file, String url, String sha256) {
            this.name = name;
            this.version = version;
            this.file = file;
            this.url = url;
            this.sha256 = sha256;
        }
    }

    /// What a resolution installed.
    public static final class Result {
        /// `name==version` of every distribution installed.
        public final List<String> installed = new ArrayList<String>();
        /// Requirements the runtime provides, by name.
        public final List<String> provided = new ArrayList<String>();
        /// The compiled modules found, by dotted name, each with the Java
        /// class that stands in for it.
        public final Map<String, String> nativeModules = new TreeMap<String, String>();
        /// Compiled modules every import of which falls back to Python
        /// when they are missing: nothing has to be written for these.
        public final List<String> optionalNativeModules = new ArrayList<String>();
        /// Stub classes written by this run (an existing one is never
        /// overwritten).
        public final List<String> stubsWritten = new ArrayList<String>();
    }

    private final Log log;
    private final Fetcher fetcher;
    private String index = "https://pypi.org/pypi/";

    public PythonPackageResolver(Log log) {
        this(log, PythonPackageResolver::download);
    }

    PythonPackageResolver(Log log, Fetcher fetcher) {
        this.log = log;
        this.fetcher = fetcher;
    }

    /// Uses another index that speaks PyPI's JSON API (a mirror).
    public PythonPackageResolver index(String url) {
        this.index = url.endsWith("/") ? url : url + "/";
        return this;
    }

    /// Resolves `requirements` (PEP 508 lines) into `commonDir`'s
    /// `src/main/python-packages`, writing stubs for compiled modules under
    /// `src/main/java`.
    public Result resolve(List<String> requirements, File commonDir) throws BuildException {
        File target = new File(commonDir, "src/main/" + PACKAGES_DIR);
        File javaDir = new File(commonDir, "src/main/java");
        Map<String, Pinned> locked = readLock(new File(target, LOCK));
        Result result = new Result();
        Map<String, Pinned> chosen = new LinkedHashMap<String, Pinned>();
        Map<String, String> sources = new TreeMap<String, String>();
        Map<String, List<String>> nativeByDist = new LinkedHashMap<String, List<String>>();
        Deque<Object[]> queue = new ArrayDeque<Object[]>();
        for (String line : requirements) {
            PythonRequirement r = PythonRequirement.parse(line);
            if (r != null && r.applies(Collections.<String>emptyList())) {
                queue.add(new Object[] {r, "the project"});
            }
        }
        while (!queue.isEmpty()) {
            Object[] item = queue.poll();
            PythonRequirement r = (PythonRequirement) item[0];
            if (providedByRuntime(r.name)) {
                if (!result.provided.contains(r.name)) {
                    result.provided.add(r.name);
                }
                continue;
            }
            Pinned have = chosen.get(r.name);
            if (have != null) {
                if (!r.accepts(have.version)) {
                    throw new BuildException(item[1] + " requires " + r + ", but " + have.name + " "
                            + have.version + " was already chosen");
                }
                continue;
            }
            Pinned lock = locked.get(r.name);
            Pinned p = lock != null && r.accepts(lock.version) ? lock : choose(r);
            chosen.put(r.name, p);
            byte[] archive = fetchVerified(p);
            Unpacked u = unpack(p, archive);
            sources.putAll(u.sources);
            nativeByDist.put(p.name, u.nativeModules);
            result.installed.add(p.name + "==" + p.version);
            for (String dep : u.requires) {
                PythonRequirement d = PythonRequirement.parse(dep);
                if (d != null && d.applies(r.extras)) {
                    queue.add(new Object[] {d, p.name + " " + p.version});
                }
            }
        }
        try {
            writeTree(target, sources);
            writeLock(new File(target, LOCK), chosen.values());
            PythonExtensionStubs stubs = new PythonExtensionStubs(sources);
            for (Map.Entry<String, List<String>> e : nativeByDist.entrySet()) {
                for (String module : e.getValue()) {
                    if (stubs.isOptional(module)) {
                        result.optionalNativeModules.add(module);
                        continue;
                    }
                    String cls = stubs.className(module);
                    result.nativeModules.put(module, cls);
                    if (stubs.write(module, e.getKey(), javaDir)) {
                        result.stubsWritten.add(cls);
                    }
                }
            }
            PythonExtensionStubs.writeIndex(new File(target, PythonExtensionStubs.INDEX), result.nativeModules);
        } catch (IOException e) {
            throw new BuildException("Cannot write " + target + ": " + e.getMessage(), e);
        }
        return result;
    }

    /// Whether the runtime provides the distribution `name`: its import
    /// name is a standard or mapped module.
    static boolean providedByRuntime(String name) {
        String n = PythonRequirement.normalize(name);
        String module;
        switch (n) {
            case "pyyaml":
                module = "yaml";
                break;
            case "pillow":
                module = "PIL";
                break;
            case "python-dateutil":
                module = "dateutil";
                break;
            case "attrs":
                module = "attr";
                break;
            case "typing-extensions":
                module = "typing_extensions";
                break;
            default:
                module = n.replace('-', '_');
        }
        return PythonTranspiler.provides(module);
    }

    /// The newest release `r` accepts, and the file to install from it: a
    /// pure-Python wheel, else a CPython wheel (whose Python is the same,
    /// and whose compiled modules name what has to be written in Java),
    /// else the source distribution.
    private Pinned choose(PythonRequirement r) throws BuildException {
        JSONObject meta;
        try {
            meta = new JSONObject(new String(fetcher.fetch(index + r.name + "/json"), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BuildException("Cannot look up " + r.name + " on " + index + ": " + e.getMessage(), e);
        }
        JSONObject releases = meta.optJSONObject("releases");
        if (releases == null) {
            throw new BuildException(r.name + ": the index answered no releases");
        }
        String best = null;
        for (String v : releases.keySet()) {
            JSONArray files = releases.getJSONArray(v);
            if (files.length() == 0 || allYanked(files) || !r.accepts(v)) {
                continue;
            }
            if (best == null || PythonRequirement.compare(v, best) > 0) {
                best = v;
            }
        }
        if (best == null) {
            throw new BuildException("No release of " + r.name + " matches " + r);
        }
        JSONArray files = releases.getJSONArray(best);
        JSONObject pick = null;
        int rank = Integer.MAX_VALUE;
        for (int i = 0; i < files.length(); i++) {
            JSONObject f = files.getJSONObject(i);
            if (f.optBoolean("yanked", false)) {
                continue;
            }
            int k = rank(f.getString("filename"));
            if (k < rank || (k == rank && pick != null && f.optLong("size") < pick.optLong("size"))) {
                rank = k;
                pick = f;
            }
        }
        if (pick == null || rank == Integer.MAX_VALUE) {
            throw new BuildException(r.name + " " + best + " has no wheel or source distribution to install");
        }
        String sha = pick.getJSONObject("digests").optString("sha256", null);
        if (sha == null) {
            throw new BuildException(r.name + " " + best + ": the index publishes no SHA-256 for "
                    + pick.getString("filename"));
        }
        return new Pinned(r.name, best, pick.getString("filename"), pick.getString("url"), sha);
    }

    private static boolean allYanked(JSONArray files) {
        for (int i = 0; i < files.length(); i++) {
            if (!files.getJSONObject(i).optBoolean("yanked", false)) {
                return false;
            }
        }
        return true;
    }

    /// Lower is better: a pure wheel, a CPython 3 wheel for Linux, any
    /// CPython 3 wheel, the source distribution.
    static int rank(String filename) {
        String f = filename.toLowerCase(Locale.ROOT);
        if (f.endsWith(".whl")) {
            if (f.endsWith("-none-any.whl") && (f.contains("-py3-") || f.contains(".py3-") || f.contains("-py2.py3-"))) {
                return 0;
            }
            if (f.contains("-cp3") && (f.contains("manylinux") || f.contains("musllinux"))) {
                return 1;
            }
            if (f.contains("-cp3") || f.contains("-abi3-")) {
                return 2;
            }
            return Integer.MAX_VALUE;
        }
        if (f.endsWith(".tar.gz") || f.endsWith(".zip")) {
            return 3;
        }
        return Integer.MAX_VALUE;
    }

    private byte[] fetchVerified(Pinned p) throws BuildException {
        byte[] b;
        try {
            b = fetcher.fetch(p.url);
        } catch (IOException e) {
            throw new BuildException("Cannot download " + p.file + ": " + e.getMessage(), e);
        }
        String got = sha256(b);
        if (!got.equalsIgnoreCase(p.sha256)) {
            throw new BuildException(p.file + " does not match its SHA-256: expected " + p.sha256 + ", got " + got);
        }
        return b;
    }

    static String sha256(byte[] b) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(b);
            StringBuilder sb = new StringBuilder();
            for (byte x : d) {
                sb.append(Character.forDigit((x >> 4) & 0xf, 16)).append(Character.forDigit(x & 0xf, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /// The Python sources, the compiled modules and the requirements of
    /// one archive.
    static final class Unpacked {
        final Map<String, String> sources = new TreeMap<String, String>();
        final List<String> nativeModules = new ArrayList<String>();
        final List<String> requires = new ArrayList<String>();
    }

    static Unpacked unpack(Pinned p, byte[] archive) throws BuildException {
        try {
            Map<String, byte[]> entries = p.file.endsWith(".tar.gz") ? untar(archive) : unzip(archive);
            return p.file.endsWith(".whl") ? fromWheel(entries) : fromSdist(entries);
        } catch (IOException e) {
            throw new BuildException("Cannot read " + p.file + ": " + e.getMessage(), e);
        }
    }

    private static Unpacked fromWheel(Map<String, byte[]> entries) {
        Unpacked u = new Unpacked();
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String path = e.getKey();
            String top = path.contains("/") ? path.substring(0, path.indexOf('/')) : "";
            if (top.endsWith(".dist-info")) {
                if (path.endsWith("/METADATA")) {
                    requiresDist(new String(e.getValue(), StandardCharsets.UTF_8), u.requires);
                }
                continue;
            }
            if (top.endsWith(".data")) {
                // purelib/platlib trees install into site-packages as well
                String[] parts = path.split("/", 3);
                if (parts.length == 3 && ("purelib".equals(parts[1]) || "platlib".equals(parts[1]))) {
                    path = parts[2];
                } else {
                    continue;
                }
            }
            take(path, e.getValue(), u);
        }
        return u;
    }

    private static Unpacked fromSdist(Map<String, byte[]> entries) {
        Unpacked u = new Unpacked();
        String root = null;
        for (String path : entries.keySet()) {
            int slash = path.indexOf('/');
            root = slash < 0 ? "" : path.substring(0, slash + 1);
            break;
        }
        boolean srcLayout = false;
        for (String path : entries.keySet()) {
            if (path.startsWith(root + "src/") && path.endsWith("/__init__.py")) {
                srcLayout = true;
            }
        }
        String base = srcLayout ? root + "src/" : root;
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String path = e.getKey();
            if (path.equals(root + "PKG-INFO")) {
                requiresDist(new String(e.getValue(), StandardCharsets.UTF_8), u.requires);
            }
            if (!path.startsWith(base)) {
                continue;
            }
            String rel = path.substring(base.length());
            String first = rel.contains("/") ? rel.substring(0, rel.indexOf('/')) : rel;
            if (!rel.contains("/") && !rel.endsWith(".py") || "setup.py".equals(rel) || "conftest.py".equals(rel)
                    || first.equals("tests") || first.equals("test") || first.equals("docs")
                    || first.equals("examples") || first.equals("benchmarks") || first.equals("build")) {
                continue;
            }
            take(rel, e.getValue(), u);
        }
        return u;
    }

    /// Keeps a `.py` file, and notes a compiled module (`.so`, `.pyd`) or
    /// the source of one (`.pyx`, `.c` beside a package) by dotted name.
    private static void take(String path, byte[] data, Unpacked u) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        String dir = path.substring(0, path.length() - name.length());
        if (path.endsWith(".py")) {
            u.sources.put(path, new String(data, StandardCharsets.UTF_8));
            return;
        }
        String module = null;
        if (name.endsWith(".so") || name.endsWith(".pyd")) {
            module = name.substring(0, name.indexOf('.'));
        } else if (name.endsWith(".pyx")) {
            module = name.substring(0, name.length() - 4);
        }
        if (module != null) {
            String dotted = (dir + module).replace('/', '.');
            if (!u.nativeModules.contains(dotted)) {
                u.nativeModules.add(dotted);
            }
        }
    }

    private static void requiresDist(String metadata, List<String> into) {
        for (String line : metadata.split("\n")) {
            if (line.isEmpty() || line.equals("\r")) {
                break;
            }
            if (line.regionMatches(true, 0, "Requires-Dist:", 0, 14)) {
                into.add(line.substring(14).trim());
            }
        }
    }

    private static Map<String, byte[]> unzip(byte[] archive) throws IOException {
        Map<String, byte[]> out = new TreeMap<String, byte[]>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (!e.isDirectory() && safe(e.getName())) {
                    out.put(e.getName(), readAll(z));
                }
            }
        }
        return out;
    }

    /// A ustar/pax reader: enough for source distributions.
    static Map<String, byte[]> untar(byte[] archive) throws IOException {
        Map<String, byte[]> out = new TreeMap<String, byte[]>();
        byte[] tar;
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(archive))) {
            tar = readAll(in);
        }
        int pos = 0;
        String longName = null;
        while (pos + 512 <= tar.length) {
            if (tar[pos] == 0) {
                break;
            }
            String name = field(tar, pos, 100);
            String prefix = field(tar, pos + 345, 155);
            long size = Long.parseLong(field(tar, pos + 124, 12).trim().isEmpty() ? "0" : field(tar, pos + 124, 12).trim(), 8);
            char type = (char) tar[pos + 156];
            int data = pos + 512;
            // The header's size is the archive's word, not ours: an entry that
            // claims more than is left would truncate the casts below.
            if (size < 0 || size > tar.length - data) {
                throw new IOException("Corrupt sdist archive: an entry claims " + size
                        + " bytes with " + (tar.length - data) + " left");
            }
            int len = (int) size;
            if (type == 'x' || type == 'L') {
                String body = new String(tar, data, len, StandardCharsets.UTF_8);
                if (type == 'L') {
                    longName = body.trim();
                } else {
                    for (String rec : body.split("\n")) {
                        int sp = rec.indexOf(' ');
                        if (sp > 0 && rec.startsWith("path=", sp + 1)) {
                            longName = rec.substring(sp + 6);
                        }
                    }
                }
            } else if (type == '0' || type == '\0') {
                String full = longName != null ? longName : prefix.isEmpty() ? name : prefix + "/" + name;
                longName = null;
                if (safe(full)) {
                    byte[] b = new byte[len];
                    System.arraycopy(tar, data, b, 0, len);
                    out.put(full, b);
                }
            } else {
                longName = null;
            }
            pos = data + (len + 511) / 512 * 512;
        }
        return out;
    }

    private static String field(byte[] b, int off, int len) {
        int end = off;
        while (end < off + len && b[end] != 0) {
            end++;
        }
        return new String(b, off, end - off, StandardCharsets.UTF_8);
    }

    /// Rejects an entry that would land outside the target directory.
    private static boolean safe(String path) {
        return !path.startsWith("/") && !path.contains("\\") && !("/" + path + "/").contains("/../");
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /// Makes `target` hold exactly `sources`, keeping the lock and index.
    private static void writeTree(File target, Map<String, String> sources) throws IOException {
        if (target.isDirectory()) {
            deletePy(target);
        }
        for (Map.Entry<String, String> e : sources.entrySet()) {
            File f = new File(target, e.getKey());
            Files.createDirectories(f.getParentFile().toPath());
            Files.write(f.toPath(), e.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deletePy(File dir) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory() && !Files.isSymbolicLink(c.toPath())) {
                deletePy(c);
                String[] left = c.list();
                if (left != null && left.length == 0) {
                    Files.delete(c.toPath());
                }
            } else if (c.getName().endsWith(".py")) {
                Files.delete(c.toPath());
            }
        }
    }

    static Map<String, Pinned> readLock(File lock) throws BuildException {
        Map<String, Pinned> out = new TreeMap<String, Pinned>();
        if (!lock.isFile()) {
            return out;
        }
        try {
            for (String line : Files.readAllLines(lock.toPath(), StandardCharsets.UTF_8)) {
                String l = line.trim();
                if (l.isEmpty() || l.startsWith("#")) {
                    continue;
                }
                String[] f = l.split("\\s+");
                int eq = f[0].indexOf("==");
                if (f.length < 4 || eq < 0) {
                    throw new BuildException(lock + ": cannot read the line " + l);
                }
                Map<String, String> kv = new TreeMap<String, String>();
                for (int i = 1; i < f.length; i++) {
                    int e = f[i].indexOf('=');
                    if (e > 0) {
                        kv.put(f[i].substring(0, e), f[i].substring(e + 1));
                    }
                }
                String name = f[0].substring(0, eq);
                out.put(name, new Pinned(name, f[0].substring(eq + 2), kv.get("file"), kv.get("url"), kv.get("sha256")));
            }
        } catch (IOException e) {
            throw new BuildException("Cannot read " + lock + ": " + e.getMessage(), e);
        }
        return out;
    }

    private static void writeLock(File lock, Iterable<Pinned> pins) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Written by cn1:import-python-project. The packages in this directory are\n");
        sb.append("# exactly these; delete a line (or the file) to resolve that package again.\n");
        TreeSet<String> lines = new TreeSet<String>();
        for (Pinned p : pins) {
            lines.add(p.name + "==" + p.version + " file=" + p.file + " sha256=" + p.sha256 + " url=" + p.url);
        }
        for (String l : lines) {
            sb.append(l).append('\n');
        }
        Files.createDirectories(lock.getParentFile().toPath());
        Files.write(lock.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] download(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(30000);
        c.setReadTimeout(120000);
        c.setRequestProperty("Accept", "application/json, */*");
        c.setRequestProperty("User-Agent", "codenameone-python-import");
        int status = c.getResponseCode();
        if (status != 200) {
            throw new IOException(url + " answered " + status);
        }
        try (InputStream in = c.getInputStream()) {
            return readAll(in);
        }
    }
}
