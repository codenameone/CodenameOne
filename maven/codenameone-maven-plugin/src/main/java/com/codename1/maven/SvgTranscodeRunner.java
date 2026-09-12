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
import com.codename1.svg.transcoder.SVGTranscoder;
import com.codename1.svg.transcoder.SVGTranscoder.GeneratedClass;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        LOTTIE_PACK(".lottie");

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
    public Set<String> cssReferencedVectorNames() throws MojoExecutionException {
        return scanCssHints().keySet();
    }

    /** Filenames of the vector sources this module actually contains. The
     *  difference between this and {@link #cssReferencedVectorNames()} is the
     *  set of CSS references that resolve to nothing. */
    public Set<String> presentVectorSourceNames() {
        Set<String> names = new LinkedHashSet<String>();
        for (File f : locateSvgs()) {
            names.add(f.getName());
        }
        return names;
    }

    /** The generated registry source file for a given output dir and package. */
    public static File registrySourceFile(File outputDir, String svgPackage) {
        String pkg = svgPackage == null || svgPackage.isEmpty() ? DEFAULT_PACKAGE : svgPackage;
        return new File(new File(outputDir, pkg.replace('.', '/')), REGISTRY_CLASS_NAME + ".java");
    }

    /** Transcode every vector source, emit the registry and the CSS placeholders. */
    public void run() throws MojoExecutionException {
        transcodedCount = 0;
        List<File> svgs = locateSvgs();
        Map<String, CssHint> cssHints = scanCssHints();

        if (svgs.isEmpty() && !cssHints.isEmpty()) {
            log.warn("CSS references " + cssHints.size()
                    + " SVG(s) but no .svg files were found under "
                    + String.join(", ", effectiveSourceDirs()));
        }

        File packageDir = new File(outputDir, svgPackage.replace('.', '/'));
        long registrySrcMtime = lastModified(svgs);
        List<GeneratedClass> generated = new ArrayList<GeneratedClass>();
        Set<String> usedClassNames = new HashSet<String>();

        if (svgs.isEmpty()) {
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
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to transcode " + svg, ex);
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

        emitRegistry(packageDir, generated, registrySrcMtime);
        emitPlaceholders(cssHints.keySet());
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
    private Map<String, CssHint> scanCssHints() throws MojoExecutionException {
        Map<String, CssHint> result = new HashMap<String, CssHint>();
        File cssDir = new File(basedir, "src/main/css");
        if (!cssDir.isDirectory()) {
            return result;
        }
        List<File> cssFiles = new ArrayList<File>();
        collectCss(cssDir, cssFiles);
        Pattern blockPattern = Pattern.compile("\\{([^}]*)\\}", Pattern.DOTALL);
        Pattern svgUrlPattern = Pattern.compile(
                "url\\(\\s*['\"]?\\s*([^'\")\\s]+?\\.(?:svg|json|lottie))\\s*['\"]?\\s*\\)",
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
                throw new MojoExecutionException("Failed to read CSS " + css, ex);
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
    private void emitPlaceholders(Set<String> names) throws MojoExecutionException {
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
                    throw new MojoExecutionException("Failed to write placeholder " + out, ex);
                }
            }
        }
        log.debug("Wrote " + names.size() + " SVG placeholder PNG(s) to " + placeholderDir);
    }

    private void emitRegistry(File packageDir, List<GeneratedClass> generated, long mtime)
            throws MojoExecutionException {
        File registryFile = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
        if (registryFile.exists() && registryFile.lastModified() >= mtime) {
            log.debug("SVG registry up-to-date.");
            return;
        }
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(registryFile), "UTF-8"))) {
            SVGTranscoder.writeRegistry(svgPackage, REGISTRY_CLASS_NAME, generated, w);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write SVG registry", ex);
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
            case LOTTIE_PACK:
                // .lottie (dotLottie ZIP) needs an extra archive-extract step
                // we don't perform here yet -- the parser treats the bytes as
                // a JSON document. Drop a plain Lottie JSON for now.
                LottieTranscoder.transcode(src, pkg, className, outFile);
                break;
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
