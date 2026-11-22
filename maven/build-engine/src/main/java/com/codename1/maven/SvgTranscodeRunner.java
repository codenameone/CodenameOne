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

import com.codename1.lottie.transcoder.LottieTranscoder;
import com.codename1.lottie.transcoder.parser.DotLottie;
import com.codename1.lottie.transcoder.parser.JsonParser;
import com.codename1.rive.transcoder.RiveTranscoder;
import com.codename1.svg.transcoder.SVGTranscoder;
import com.codename1.svg.transcoder.SVGTranscoder.GeneratedClass;

import com.codename1.builders.BuildException;
import com.codename1.build.Log;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The build-time vector transcoder, independent of the Maven mojo that
 * normally drives it.
 *
 * <p>Extracted from {@link TranscodeSVGMojo} because there are now two
 * callers. The mojo is one; the other is the self-repair in
 * {@link AbstractCN1Mojo#ensureSvgTranscoderWired}, which has to do this work
 * for a project whose pom predates the {@code transcode-svg} execution and
 * therefore never binds the mojo at all. Both paths must produce byte-identical
 * output -- a project repaired in flight and the same project rebuilt after its
 * pom was updated have to compile to the same classes -- so there is one
 * implementation and no second copy to drift.</p>
 *
 * <p>See {@link TranscodeSVGMojo} for the source layout, the CSS hint
 * vocabulary and why CSS placeholders are emitted.</p>
 */
public class SvgTranscodeRunner {

    /** Package the generated image classes and the registry are emitted into. */
    public static final String DEFAULT_PACKAGE = "com.codename1.generated.svg";

    /** Simple name of the generated registry class. The per-platform builders
     *  look for this class by its compiled path to decide whether to emit an
     *  {@code installGlobal()} call into the application stub, so it is a fixed
     *  name rather than a configurable one. */
    public static final String REGISTRY_CLASS_NAME = "SVGRegistry";

    /** Package the classes translated from {@code .riv} files are emitted into. */
    public static final String RIVE_PACKAGE = "com.codename1.generated.rive";

    /** The artifact that holds the Rive runtime ({@code com.codename1.impl.rive}).
     *  It is not part of the core, and an application never declares it: when
     *  a module has {@code .riv} files the build tool resolves it at its own
     *  version and puts its classes into the module's output directory (see
     *  {@link #installRuntime}), so they compile with the generated classes and
     *  ship in every target like the application's own classes. */
    public static final String RIVE_RUNTIME_ARTIFACT = "codenameone-rive-runtime";

    /** The image class the registry wraps each generated {@code RiveFile} in. */
    public static final String RIVE_IMAGE_CLASS = "com.codename1.impl.rive.RiveImage";

    /** True when {@code moduleDir} has at least one {@code .riv} file in the
     *  default vector source directories ({@code src/main/css} and friends). */
    public static boolean hasRiveSources(File moduleDir) {
        return new SvgTranscodeRunner(moduleDir, null, null, null, null, null).hasRiveSources();
    }

    /** True when this module has at least one {@code .riv} file to translate. */
    public boolean hasRiveSources() {
        for (File f : locateSvgs()) {
            if (VectorFormat.fromFilename(f.getName()) == VectorFormat.RIVE) {
                return true;
            }
        }
        return false;
    }

    /** Marker written into the output directory once a runtime jar is unpacked
     *  there: the jar's size and time on the first line, so a changed jar is
     *  unpacked again, then every file it wrote, so they can be removed first. */
    static final String RUNTIME_MARKER = "META-INF/cn1-rive-runtime.stamp";

    /**
     * Copies the classes of the Rive runtime jar into {@code classesDir}, the
     * module's compiler output directory. That directory is on javac's class
     * path, so the generated classes compile against the runtime, and every
     * target packages and translates what is in it, so the runtime ships the
     * way the compatibility runtimes copied in by {@code remap-compat} do --
     * without a dependency in the project. {@code META-INF} is not copied.
     * Answers the number of files written; 0 when the stamp says the same jar
     * was already unpacked.
     */
    public static int installRuntime(File runtimeJar, File classesDir) throws IOException {
        String stamp = runtimeJar.length() + ":" + runtimeJar.lastModified();
        File marker = new File(classesDir, RUNTIME_MARKER);
        java.nio.file.Path root = classesDir.getCanonicalFile().toPath();
        if (marker.isFile()) {
            List<String> previous = Files.readAllLines(marker.toPath(), StandardCharsets.US_ASCII);
            if (!previous.isEmpty() && stamp.equals(previous.get(0))
                    && new File(classesDir, RIVE_IMAGE_CLASS.replace('.', '/') + ".class").isFile()) {
                return 0;
            }
            // A different runtime: remove what the previous one wrote, so a
            // class it had and this one dropped does not linger in the output
            // and ship with the application until the next clean.
            deleteRecorded(root, previous);
        }
        int written = 0;
        StringBuilder record = new StringBuilder(stamp);
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(runtimeJar)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                java.util.zip.ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.startsWith("META-INF/")) {
                    continue;
                }
                java.nio.file.Path target = root.resolve(name).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException(runtimeJar + " holds an entry outside its root: " + name);
                }
                Files.createDirectories(target.getParent());
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                record.append('\n').append(name);
                written++;
            }
        }
        Files.createDirectories(marker.getParentFile().toPath());
        Files.write(marker.toPath(), record.toString().getBytes(StandardCharsets.US_ASCII));
        return written;
    }

    /**
     * Removes a runtime {@link #installRuntime} put into {@code classesDir}:
     * every file its stamp lists, then the stamp. A module whose last
     * {@code .riv} file was deleted, or a library that must not ship the
     * runtime, would otherwise keep shipping it until the next clean.
     * Answers the number of files removed; 0 when there is no stamp.
     */
    public static int uninstallRuntime(File classesDir) throws IOException {
        File marker = new File(classesDir, RUNTIME_MARKER);
        if (!marker.isFile()) {
            return 0;
        }
        List<String> previous = Files.readAllLines(marker.toPath(), StandardCharsets.US_ASCII);
        int removed = deleteRecorded(classesDir.getCanonicalFile().toPath(), previous);
        Files.deleteIfExists(marker.toPath());
        return removed;
    }

    /** Deletes the files a stamp lists after its first line, inside {@code root} only. */
    private static int deleteRecorded(java.nio.file.Path root, List<String> stamp) throws IOException {
        int removed = 0;
        for (int i = 1; i < stamp.size(); i++) {
            java.nio.file.Path old = root.resolve(stamp.get(i)).normalize();
            if (old.startsWith(root) && Files.deleteIfExists(old)) {
                removed++;
            }
        }
        return removed;
    }

    static final String[] DEFAULT_SVG_DIRS = {
            "src/main/svg",
            "src/main/lottie",
            "src/main/css"
    };

    /** Recognized vector source extensions plus the format key the file
     *  parses as. Order matters only when multiple extensions could match
     *  the same bytes -- they cannot here. */
    enum VectorFormat {
        SVG(".svg"),
        LOTTIE_JSON(".json"),
        LOTTIE_PACK(".lottie"),
        /** Rive; translated to a {@code RiveFile} subclass in {@link #RIVE_PACKAGE}. */
        RIVE(".riv");

        final String ext;
        VectorFormat(String ext) { this.ext = ext; }

        static VectorFormat fromFilename(String name) {
            // Extension matching only, and every extension here is ASCII by
            // definition, so a case fold is safe -- but do it by hand rather
            // than through toLowerCase(), which is locale sensitive.
            String lower = asciiLower(name);
            for (VectorFormat f : values()) {
                if (lower.endsWith(f.ext)) return f;
            }
            return null;
        }
    }

    /** 1x1 transparent PNG (43 bytes). Used as a placeholder so the CSS
     *  compiler resolves {@code url(*.svg)} references without trying to
     *  rasterize the SVG XML. */
    private static final byte[] PLACEHOLDER_PNG = new byte[] {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
            (byte)0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte)0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte)0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE,
            0x42, 0x60, (byte)0x82
    };

    private final File basedir;
    private final List<String> sourceDirs;
    private final File outputDir;
    private final File placeholderDir;
    private final String svgPackage;
    private final Log log;

    private int transcodedCount;

    /**
     * @param basedir        the module root the source directories are resolved against
     * @param sourceDirs     directories to scan, relative to {@code basedir}; null or
     *                       empty selects {@link #DEFAULT_SVG_DIRS}
     * @param outputDir      where generated Java sources are written
     *                       ({@code target/generated-sources/svg})
     * @param placeholderDir where the 1x1 PNG placeholders are written
     *                       ({@code target/css-resources})
     * @param svgPackage     package for the generated classes; null selects
     *                       {@link #DEFAULT_PACKAGE}
     */
    public SvgTranscodeRunner(File basedir, List<String> sourceDirs, File outputDir,
                              File placeholderDir, String svgPackage, Log log) {
        this.basedir = basedir;
        this.sourceDirs = sourceDirs;
        this.outputDir = outputDir;
        this.placeholderDir = placeholderDir;
        this.svgPackage = svgPackage == null || svgPackage.isEmpty() ? DEFAULT_PACKAGE : svgPackage;
        this.log = log;
    }

    /**
     * The vector sources in this module that the transcoder cannot be relied
     * on to read: a {@code .json} that is not a Lottie document, or a
     * {@code .lottie} archive with no animation in it.
     *
     * <p>Asked before anything is generated, so a caller that must not break a
     * build can decide not to start. The transcoder itself has one behaviour
     * and always had: it reports a source it cannot read and stops.</p>
     */
    public List<String> unreadableSources() {
        List<String> unreadable = new ArrayList<String>();
        for (File f : locateSvgs()) {
            VectorFormat fmt = VectorFormat.fromFilename(f.getName());
            if ((fmt == VectorFormat.LOTTIE_JSON || fmt == VectorFormat.LOTTIE_PACK)
                    && !looksLikeLottie(f, fmt)) {
                unreadable.add(f.getName());
            }
        }
        return unreadable;
    }

    /** Number of vector sources transcoded by the last {@link #run()}. */
    public int getTranscodedCount() {
        return transcodedCount;
    }

    public File getOutputDir() {
        return outputDir;
    }

    /** True when this module has at least one vector source to transcode. */
    public boolean hasVectorSources() {
        return !locateSvgs().isEmpty();
    }

    /** Filenames the theme CSS references through {@code url(...)}, whether or
     *  not a matching source file exists. This is what the build has to warn
     *  about when no registry was generated: each one is a 1x1 transparent
     *  placeholder in the compiled theme, which renders as nothing. */
    public Set<String> cssReferencedVectorNames() throws BuildException {
        return scanCssHints().keySet();
    }

    /** The generated registry source file for a given output dir and package. */
    public static File registrySourceFile(File outputDir, String svgPackage) {
        String pkg = svgPackage == null || svgPackage.isEmpty() ? DEFAULT_PACKAGE : svgPackage;
        return new File(new File(outputDir, pkg.replace('.', '/')), REGISTRY_CLASS_NAME + ".java");
    }

    /** Transcode every vector source, emit the registry and the CSS placeholders. */
    public void run() throws BuildException {
        transcodedCount = 0;
        List<File> svgs = new ArrayList<File>();
        List<File> rivs = new ArrayList<File>();
        for (File f : locateSvgs()) {
            (VectorFormat.fromFilename(f.getName()) == VectorFormat.RIVE ? rivs : svgs).add(f);
        }
        List<GeneratedClass> riveClasses = transcodeRive(rivs);
        Map<String, CssHint> cssHints = scanCssHints();

        if (svgs.isEmpty() && rivs.isEmpty() && !cssHints.isEmpty()) {
            log.warn("CSS references " + cssHints.size()
                    + " SVG(s) but no .svg files were found under "
                    + String.join(", ", effectiveSourceDirs()));
        }

        File packageDir = new File(outputDir, svgPackage.replace('.', '/'));
        long registrySrcMtime = Math.max(lastModified(svgs), lastModified(rivs));
        List<GeneratedClass> generated = new ArrayList<GeneratedClass>(riveClasses);
        Set<String> usedClassNames = new HashSet<String>();

        if (svgs.isEmpty() && rivs.isEmpty()) {
            // No SVGs in this project -- skip the registry entirely so the
            // per-platform Stub injection (IPhoneBuilder / AndroidGradleBuilder
            // checking for SVGRegistry.class) stays a no-op. JavaSE Executor's
            // dynamic load also tolerates the absence. Sweep a leftover from a
            // previous build so a stale class doesn't trick the .isFile() check.
            File leftover = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
            if (leftover.exists()) {
                leftover.delete();
            }
            emitPlaceholders(cssHints.keySet());
            return;
        }

        packageDir.mkdirs();
        for (File svg : svgs) {
            String resourceName = svg.getName();
            VectorFormat fmt = VectorFormat.fromFilename(resourceName);
            if (fmt == null) {
                // locateSvgs() already filtered to recognized extensions;
                // defensive guard for future format additions.
                continue;
            }
            String className = uniqueClassName(SVGTranscoder.classNameFor(resourceName), usedClassNames);
            usedClassNames.add(className);
            File outFile = new File(packageDir, className + ".java");
            if (outFile.exists() && outFile.lastModified() >= svg.lastModified()) {
                log.debug("Vector transcoder up-to-date for " + svg.getName());
            } else {
                log.info("Transcoding " + fmt.name() + " " + svg.getName()
                        + " -> " + className + ".java");
                try {
                    transcodeByFormat(fmt, svg, svgPackage, className, outFile);
                } catch (Exception ex) {
                    throw new BuildException("Failed to transcode " + svg, ex);
                }
            }
            transcodedCount++;
            CssHint hint = cssHints.get(resourceName);
            int sourceDensity = hint == null ? 0 : hint.sourceDensity;
            float widthMm = hint == null ? 0f : hint.widthMm;
            float heightMm = hint == null ? 0f : hint.heightMm;
            generated.add(new GeneratedClass(svgPackage, className, resourceName,
                    sourceDensity, widthMm, heightMm));
        }

        if (generated.isEmpty()) {
            // Every candidate was rejected. Emitting a registry that registers
            // nothing is not harmless: it carries the one fixed name the
            // per-platform builders look for, so it would be found, wired into
            // the stub, and run -- shadowing a dependency's registry and
            // leaving that library's images as placeholders. Sweep any stale
            // one for the same reason the no-sources path above does.
            File leftover = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
            if (leftover.exists()) {
                leftover.delete();
            }
        } else {
            emitRegistry(packageDir, generated, registrySrcMtime);
        }
        emitPlaceholders(cssHints.keySet());
    }

    private static String relative(String rootPath, File f) {
        String p = f.getAbsolutePath();
        if (p.startsWith(rootPath)) {
            p = p.substring(rootPath.length());
        }
        return p.replace(File.separatorChar, '/');
    }

    /** Starts the line of a generated Rive class that names the source it came from. */
    static final String RIVE_SOURCE_PREFIX = "// rive-source: ";

    /**
     * The source line written into a generated Rive class: the {@code .riv}
     * file's path relative to the module and a CRC32 of its bytes.
     * Comparing it, and not only modification times, regenerates a class when
     * the files shift between class names (a rename, a new file sorting
     * first) or when a source is replaced by one with an older timestamp.
     */
    static String riveSourceLine(String relativePath, byte[] bytes) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(bytes);
        StringBuilder sb = new StringBuilder(RIVE_SOURCE_PREFIX);
        for (int i = 0; i < relativePath.length(); i++) {
            char c = relativePath.charAt(i);
            // The header is ASCII; a non-ASCII name is escaped so it still compares.
            if (c < 32 || c > 126) {
                sb.append("%").append(Integer.toHexString(c));
            } else {
                sb.append(c);
            }
        }
        return sb.append(" crc=").append(Long.toHexString(crc.getValue())).toString();
    }

    /** True when {@code outFile} exists and carries this transcoder's stamp and {@code sourceLine}. */
    static boolean riveUpToDate(File outFile, String sourceLine) {
        if (!outFile.exists()) {
            return false;
        }
        String expected = RiveTranscoder.STAMP_PREFIX + RiveTranscoder.stamp();
        boolean stamped = false;
        boolean sourced = false;
        try (java.io.BufferedReader r = Files.newBufferedReader(outFile.toPath(), StandardCharsets.US_ASCII)) {
            for (int i = 0; i < 5; i++) {
                String line = r.readLine();
                if (line == null) {
                    break;
                }
                stamped |= line.equals(expected);
                sourced |= line.equals(sourceLine);
            }
        } catch (IOException ex) {
            return false;
        }
        return stamped && sourced;
    }

    /** Inserts {@code sourceLine} after the transcoder's stamp line. */
    static String withSourceLine(String src, String sourceLine) {
        int stampAt = src.indexOf(RiveTranscoder.STAMP_PREFIX);
        int eol = stampAt < 0 ? -1 : src.indexOf('\n', stampAt);
        if (eol < 0) {
            return sourceLine + "\n" + src;
        }
        return src.substring(0, eol + 1) + sourceLine + "\n" + src.substring(eol + 1);
    }

    /** The class a {@code .riv} file becomes: {@code coin.riv} is {@code CoinRive}. */
    static String riveClassNameFor(String fileName) {
        String base = SVGTranscoder.classNameFor(fileName);
        return base.endsWith("Rive") ? base : base + "Rive";
    }

    /**
     * Translates every {@code .riv} file into a {@code RiveFile} subclass in
     * {@link #RIVE_PACKAGE}. The device never parses a Rive file: the class
     * builds the file's objects directly, and names only the runtime classes
     * that file uses. Classes whose source is gone are removed. Answers the
     * registry entries: each file is registered under its file name
     * ({@code juice.riv}), wrapped in a {@code RiveImage}, exactly as an SVG is.
     * {@code rivs} comes from {@link #locateSvgs}, sorted by file name with
     * names unique, so class names are the same on every machine.
     */
    private List<GeneratedClass> transcodeRive(List<File> rivs) throws BuildException {
        List<GeneratedClass> entries = new ArrayList<GeneratedClass>();
        File packageDir = new File(outputDir, RIVE_PACKAGE.replace('.', '/'));
        Set<String> used = new HashSet<String>();
        String riveRoot = basedir.getAbsolutePath();
        for (File riv : rivs) {
            String className = uniqueClassName(riveClassNameFor(riv.getName()), used);
            used.add(className);
            File outFile = new File(packageDir, className + ".java");
            transcodedCount++;
            entries.add(GeneratedClass.wrapped(RIVE_PACKAGE, className, riv.getName(), RIVE_IMAGE_CLASS));
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(riv.toPath());
            } catch (IOException ex) {
                throw new BuildException("Failed to read " + riv + ": " + ex.getMessage(), ex);
            }
            String sourceLine = riveSourceLine(relative(riveRoot, riv), bytes);
            if (riveUpToDate(outFile, sourceLine)) {
                log.debug("Rive transcoder up-to-date for " + riv.getName());
                continue;
            }
            log.info("Transcoding Rive " + riv.getName() + " -> " + className + ".java");
            try {
                for (String skipped : RiveTranscoder.skippedTypes(bytes)) {
                    log.warn(riv.getName() + ": objects of Rive " + skipped
                            + " are not supported and do nothing on the device");
                }
                String src = withSourceLine(RiveTranscoder.transcode(bytes, RIVE_PACKAGE, className), sourceLine);
                Files.createDirectories(packageDir.toPath());
                Files.write(outFile.toPath(), src.getBytes(StandardCharsets.US_ASCII));
            } catch (IOException ex) {
                throw new BuildException("Failed to transcode " + riv + ": " + ex.getMessage(), ex);
            }
        }
        File[] existing = packageDir.listFiles();
        if (existing != null) {
            for (File f : existing) {
                String n = f.getName();
                if (n.endsWith(".java") && !used.contains(n.substring(0, n.length() - 5))) {
                    if (!f.delete()) {
                        log.warn("Could not remove stale " + f);
                    }
                }
            }
        }
        return entries;
    }

    private static long lastModified(List<File> files) {
        long newest = 0L;
        for (File f : files) {
            if (f.lastModified() > newest) {
                newest = f.lastModified();
            }
        }
        return newest;
    }

    /** Walks {@link #effectiveSourceDirs} for *.svg files. */
    private List<File> locateSvgs() {
        Map<String, File> byName = new LinkedHashMap<String, File>();
        for (String dir : effectiveSourceDirs()) {
            File d = new File(basedir, dir);
            if (!d.isDirectory()) continue;
            List<File> found = new ArrayList<File>();
            collect(d, found);
            for (File f : found) {
                // First occurrence wins -- src/main/svg beats src/main/css if a
                // name collides, mirroring how Java classpath resolution would
                // pick the higher-priority root.
                byName.putIfAbsent(f.getName(), f);
            }
        }
        List<File> svgs = new ArrayList<File>(byName.values());
        svgs.sort(new Comparator<File>() {
            @Override public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
        });
        return svgs;
    }

    private List<String> effectiveSourceDirs() {
        if (sourceDirs != null && !sourceDirs.isEmpty()) {
            return sourceDirs;
        }
        return Arrays.asList(DEFAULT_SVG_DIRS);
    }

    /** Holds the CSS-declared sizing hints for one SVG. Either / both fields
     *  may be unset (0 / 0f) -- the generator picks the right constructor
     *  variant based on which fields actually have values. */
    private static final class CssHint {
        int sourceDensity;
        float widthMm;
        float heightMm;
    }

    /** Scans theme CSS files for {@code url(*.svg|*.json|*.lottie)}
     *  together with the enclosing rule's {@code cn1-source-dpi} /
     *  {@code cn1-svg-width} / {@code cn1-svg-height}. Returns a map of
     *  filename -> CssHint. The CSS hint vocabulary is the SVG transcoder's
     *  -- the same {@code cn1-svg-width} property sizes Lottie outputs too
     *  because both share the {@code GeneratedSVGImage} base. */
    private Map<String, CssHint> scanCssHints() throws BuildException {
        Map<String, CssHint> result = new HashMap<String, CssHint>();
        File cssDir = new File(basedir, "src/main/css");
        if (!cssDir.isDirectory()) {
            return result;
        }
        List<File> cssFiles = new ArrayList<File>();
        collectCss(cssDir, cssFiles);
        Pattern blockPattern = Pattern.compile("\\{([^}]*)\\}", Pattern.DOTALL);
        Pattern svgUrlPattern = Pattern.compile(
                "url\\(\\s*['\"]?\\s*([^'\")\\s]+?\\.(?:svg|json|lottie|riv))\\s*['\"]?\\s*\\)",
                Pattern.CASE_INSENSITIVE);
        Pattern dpiPattern = Pattern.compile(
                "cn1-source-dpi\\s*:\\s*([\\w-]+)\\s*;?",
                Pattern.CASE_INSENSITIVE);
        Pattern widthMmPattern = Pattern.compile(
                "cn1-svg-width\\s*:\\s*([\\d.]+)\\s*mm\\s*;?",
                Pattern.CASE_INSENSITIVE);
        Pattern heightMmPattern = Pattern.compile(
                "cn1-svg-height\\s*:\\s*([\\d.]+)\\s*mm\\s*;?",
                Pattern.CASE_INSENSITIVE);
        for (File css : cssFiles) {
            String content;
            try {
                content = readFile(css);
            } catch (IOException ex) {
                throw new BuildException("Failed to read CSS " + css, ex);
            }
            Matcher blocks = blockPattern.matcher(content);
            while (blocks.find()) {
                String block = blocks.group(1);
                Matcher dpis = dpiPattern.matcher(block);
                int dpi = dpis.find() ? densityForCssValue(dpis.group(1)) : 0;
                Matcher widths = widthMmPattern.matcher(block);
                float wMm = widths.find() ? parsePositiveFloat(widths.group(1)) : 0f;
                Matcher heights = heightMmPattern.matcher(block);
                float hMm = heights.find() ? parsePositiveFloat(heights.group(1)) : 0f;
                Matcher svgUrls = svgUrlPattern.matcher(block);
                while (svgUrls.find()) {
                    String name = trimToFileName(svgUrls.group(1));
                    CssHint hint = result.computeIfAbsent(name, k -> new CssHint());
                    // A more-specific declaration always wins; otherwise keep
                    // whatever was set by an earlier rule.
                    if (dpi != 0) hint.sourceDensity = dpi;
                    if (wMm > 0f) hint.widthMm = wMm;
                    if (hMm > 0f) hint.heightMm = hMm;
                }
            }
        }
        // If only one of width/height was specified, derive the other from
        // the SVG's natural aspect ratio at registry-emit time. For now we
        // leave aspect derivation to the runtime by treating a single-axis
        // declaration as "use that axis, ignore mm on the other".
        return result;
    }

    private static float parsePositiveFloat(String s) {
        try {
            float f = Float.parseFloat(s.trim());
            return f > 0f ? f : 0f;
        } catch (NumberFormatException nfe) {
            return 0f;
        }
    }

    /** Drop a 1x1 transparent PNG under each referenced SVG name so the CSS
     *  compiler's url(...) resolver succeeds. We also put the original
     *  SVG-named copy alongside in case the compiler insists on the .svg
     *  extension. */
    private void emitPlaceholders(Set<String> names) throws BuildException {
        if (names.isEmpty()) {
            return;
        }
        if (!placeholderDir.isDirectory() && !placeholderDir.mkdirs()) {
            log.warn("Could not create placeholder dir " + placeholderDir);
            return;
        }
        for (String name : names) {
            File out = new File(placeholderDir, name);
            if (!out.exists() || out.length() != PLACEHOLDER_PNG.length) {
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(PLACEHOLDER_PNG);
                } catch (IOException ex) {
                    throw new BuildException("Failed to write placeholder " + out, ex);
                }
            }
        }
        log.debug("Wrote " + names.size() + " SVG placeholder PNG(s) to " + placeholderDir);
    }

    private void emitRegistry(File packageDir, List<GeneratedClass> generated, long mtime)
            throws BuildException {
        File registryFile = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
        if (registryFile.exists() && registryFile.lastModified() >= mtime) {
            log.debug("SVG registry up-to-date.");
            return;
        }
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(registryFile), "UTF-8"))) {
            SVGTranscoder.writeRegistry(svgPackage, REGISTRY_CLASS_NAME, generated, w);
        } catch (IOException ex) {
            throw new BuildException("Failed to write SVG registry", ex);
        }
        log.info("Wrote SVG registry " + registryFile.getName() + " with " + generated.size() + " image(s)");
    }

    /** Map a {@code cn1-source-dpi} CSS keyword or number to a density code.
     *  Mirrors the keyword set the CN1 CSS compiler accepts for multi-image
     *  density buckets. */
    private static int densityForCssValue(String value) {
        if (value == null) {
            return 0;
        }
        String v = asciiLower(value.trim()).replace('_', '-');
        switch (v) {
            case "very-low":  return 10;  // CN1Constants.DENSITY_VERY_LOW
            case "low":       return 20;
            case "medium":    return 30;
            case "high":      return 40;
            case "very-high": return 50;
            case "hd":        return 60;
            case "560":       return 65;
            case "2hd":       return 70;
            case "4k":        return 80;
            default:
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException nfe) {
                    return 0;
                }
        }
    }

    /** Locale-independent ASCII fold. {@code String.toLowerCase()} is locale
     *  sensitive, so on a machine set to Turkish the {@code I} of a keyword or
     *  a {@code .SVG} extension folds to a dotless i and stops matching. These
     *  tokens are ASCII by specification, so fold them by hand. */
    private static String asciiLower(String s) {
        if (s == null) {
            return null;
        }
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= 'A' && chars[i] <= 'Z') {
                chars[i] = (char) (chars[i] + 32);
            }
        }
        return new String(chars);
    }

    /**
     * Whether a Lottie-shaped input is plausibly an animation: a JSON object
     * carrying a {@code layers} key. For a {@code .lottie} archive the question
     * is asked of the animation inside it.
     *
     * <p>Deliberately a shape check and not a parse. The parser is permissive
     * by design -- that permissiveness is exactly what made an unrelated JSON
     * file look like a successful transcode -- so the question asked here is
     * the cheap structural one the parser does not ask.</p>
     */
    private static boolean looksLikeLottie(File file, VectorFormat fmt) {
        Object root;
        try {
            String json = fmt == VectorFormat.LOTTIE_PACK ? dotLottieAnimation(file) : readFile(file);
            if (json == null) {
                return false;
            }
            root = JsonParser.parse(json);
        } catch (Exception ex) {
            return false;
        }
        // Ask the same question the parser answers: a top-level object whose
        // "layers" is an array. A substring search for the word was not enough
        // -- it matched a nested property, or the text inside an unrelated
        // string value, and let a config file be bound to the strict goal.
        return root instanceof Map && ((Map<?, ?>) root).get("layers") instanceof List;
    }

    private static String trimToFileName(String url) {
        int slash = Math.max(url.lastIndexOf('/'), url.lastIndexOf('\\'));
        return slash < 0 ? url : url.substring(slash + 1);
    }

    private static String uniqueClassName(String base, Set<String> taken) {
        if (!taken.contains(base)) return base;
        int n = 2;
        while (taken.contains(base + n)) n++;
        return base + n;
    }

    private static void collect(File dir, List<File> out) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        Arrays.sort(entries, new Comparator<File>() {
            @Override public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
        });
        for (File f : entries) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (VectorFormat.fromFilename(f.getName()) != null) {
                out.add(f);
            }
        }
    }

    private static void transcodeByFormat(VectorFormat fmt, File src,
                                          String pkg, String className, File outFile) throws IOException {
        switch (fmt) {
            case SVG:
                SVGTranscoder.transcode(src, pkg, className, outFile);
                break;
            case LOTTIE_JSON:
                LottieTranscoder.transcode(src, pkg, className, outFile);
                break;
            case RIVE:
                // run() hands Rive files to transcodeRive, never here.
                throw new IOException(src + " is a Rive file, which transcodeRive translates");
            case LOTTIE_PACK: {
                // A dotLottie file is an archive around the same JSON.
                String json = dotLottieAnimation(src);
                if (json == null) {
                    throw new IOException(src + " is not a dotLottie archive holding an animation");
                }
                if (outFile.getParentFile() != null) {
                    Files.createDirectories(outFile.getParentFile().toPath());
                }
                try (Writer out = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
                    LottieTranscoder.transcode(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                            pkg, className, out);
                }
                break;
            }
        }
    }

    /**
     * The animation of a dotLottie archive as JSON text: the one its manifest
     * picks, see {@link DotLottie#animation}. Null when the archive holds
     * none.
     */
    private static String dotLottieAnimation(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return DotLottie.animation(in);
        }
    }

    private static void collectCss(File dir, List<File> out) {
        File[] entries = dir.listFiles();
        if (entries == null) {
            return;
        }
        for (File f : entries) {
            if (f.isDirectory()) {
                collectCss(f, out);
            } else if (asciiLower(f.getName()).endsWith(".css")) {
                out.add(f);
            }
        }
    }

    private static String readFile(File f) throws IOException {
        try (Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8")) {
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = r.read(buf)) > 0) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        }
    }
}
