package com.codename1.maven;

import com.codename1.svg.transcoder.SVGTranscoder;
import com.codename1.svg.transcoder.SVGTranscoder.GeneratedClass;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans an application module for SVG files, transcodes each into a
 * {@code com.codename1.ui.GeneratedSVGImage} subclass under
 * {@code target/generated-sources/svg}, and emits a registry class that
 * installs each transcoded image into a Resources instance (and the global
 * fallback) under its source filename.
 *
 * <h3>Source layout</h3>
 * SVG files are picked up from both {@code src/main/css/} and
 * {@code src/main/svg/}. Drop your SVGs next to the CSS file that references
 * them; the mojo finds them either way. Theme CSS keeps the natural
 * {@code background: url(spinner.svg);} reference.
 *
 * <h3>CSS hints</h3>
 * For each {@code url(*.svg)} occurrence the mojo also looks at the rule's
 * {@code cn1-source-dpi:} declaration (the same hint used for multi-images).
 * The transcoded SVG is then constructed with that source density so its
 * intrinsic dimensions scale to the device-pixel size CN1 multi-images
 * normally produce. Without a {@code cn1-source-dpi} the SVG's declared
 * dimensions are treated as design pixels at {@code DENSITY_MEDIUM}.
 *
 * <h3>CSS placeholders</h3>
 * To keep the standalone CSS compiler from failing on the {@code .svg} URL
 * (it expects to rasterize the referenced file), the mojo emits a 1x1
 * transparent PNG next to each CSS-referenced SVG. The placeholder lands in
 * {@code target/css-resources/} (a directory added to the compile-time CSS
 * search path); the runtime SVGRegistry's {@code install()} then overrides
 * the placeholder entry in the resources bundle with the real transcoded
 * SVG instance.
 */
@Mojo(name = "transcode-svg", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class TranscodeSVGMojo extends AbstractCN1Mojo {

    private static final String[] DEFAULT_SVG_DIRS = { "src/main/svg", "src/main/css" };

    private static final String DEFAULT_PACKAGE = "com.codename1.generated.svg";

    private static final String REGISTRY_CLASS_NAME = "SVGRegistry";

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

    @Parameter(property = "cn1.svg.sourceDirs")
    private List<String> svgSourceDirs;

    @Parameter(property = "cn1.svg.outputDir",
            defaultValue = "${project.build.directory}/generated-sources/svg")
    private File svgOutputDir;

    @Parameter(property = "cn1.svg.placeholderDir",
            defaultValue = "${project.build.directory}/css-resources")
    private File svgPlaceholderDir;

    @Parameter(property = "cn1.svg.package", defaultValue = DEFAULT_PACKAGE)
    private String svgPackage;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        List<File> svgs = locateSvgs();
        Map<String, Integer> cssHints = scanCssHints();

        if (svgs.isEmpty() && cssHints.isEmpty()) {
            getLog().debug("No SVGs found and no CSS references -- skipping SVG transcoding.");
            registerSourceRoot();
            return;
        }
        if (svgs.isEmpty()) {
            getLog().warn("CSS references " + cssHints.size()
                    + " SVG(s) but no .svg files were found under "
                    + String.join(", ", effectiveSourceDirs()));
            registerSourceRoot();
            return;
        }

        File packageDir = new File(svgOutputDir, svgPackage.replace('.', '/'));
        packageDir.mkdirs();
        long registrySrcMtime = lastModified(svgs);
        List<GeneratedClass> generated = new ArrayList<GeneratedClass>();
        Set<String> usedClassNames = new HashSet<String>();

        for (File svg : svgs) {
            String resourceName = svg.getName();
            String className = uniqueClassName(SVGTranscoder.classNameFor(resourceName), usedClassNames);
            usedClassNames.add(className);
            File outFile = new File(packageDir, className + ".java");
            if (outFile.exists() && outFile.lastModified() >= svg.lastModified()) {
                getLog().debug("SVG transcoder up-to-date for " + svg.getName());
            } else {
                getLog().info("Transcoding SVG " + svg.getName() + " -> " + className + ".java");
                try {
                    SVGTranscoder.transcode(svg, svgPackage, className, outFile);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to transcode " + svg, ex);
                }
            }
            int sourceDensity = cssHints.getOrDefault(resourceName, 0);
            generated.add(new GeneratedClass(svgPackage, className, resourceName, sourceDensity));
        }

        emitRegistry(packageDir, generated, registrySrcMtime);
        emitPlaceholders(cssHints.keySet());
        registerSourceRoot();
    }

    /** Walks {@link #effectiveSourceDirs} for *.svg files. */
    private List<File> locateSvgs() {
        Map<String, File> byName = new LinkedHashMap<String, File>();
        for (String dir : effectiveSourceDirs()) {
            File d = new File(project.getBasedir(), dir);
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
        if (svgSourceDirs != null && !svgSourceDirs.isEmpty()) {
            return svgSourceDirs;
        }
        return Arrays.asList(DEFAULT_SVG_DIRS);
    }

    /** Scans theme CSS files for {@code url(*.svg)} and the rule's
     *  {@code cn1-source-dpi}. Returns a map of svgFilename -> sourceDensity
     *  (one of the CN1Constants.DENSITY_* codes; 0 when unspecified). */
    private Map<String, Integer> scanCssHints() throws MojoExecutionException {
        Map<String, Integer> result = new HashMap<String, Integer>();
        File cssDir = new File(project.getBasedir(), "src/main/css");
        if (!cssDir.isDirectory()) {
            return result;
        }
        List<File> cssFiles = new ArrayList<File>();
        collectCss(cssDir, cssFiles);
        Pattern blockPattern = Pattern.compile("\\{([^}]*)\\}", Pattern.DOTALL);
        Pattern svgUrlPattern = Pattern.compile(
                "url\\(\\s*['\"]?\\s*([^'\")\\s]+?\\.svg)\\s*['\"]?\\s*\\)",
                Pattern.CASE_INSENSITIVE);
        Pattern dpiPattern = Pattern.compile(
                "cn1-source-dpi\\s*:\\s*([\\w-]+)\\s*;?",
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
                Matcher svgUrls = svgUrlPattern.matcher(block);
                Matcher dpis = dpiPattern.matcher(block);
                int dpi = dpis.find() ? densityForCssValue(dpis.group(1)) : 0;
                while (svgUrls.find()) {
                    String name = trimToFileName(svgUrls.group(1));
                    int existing = result.getOrDefault(name, 0);
                    // Last-write wins when the same SVG appears in multiple
                    // rules with different cn1-source-dpi hints -- but a
                    // specified density always beats the default 0.
                    if (dpi != 0 || existing == 0) {
                        result.put(name, dpi == 0 ? existing : dpi);
                    }
                }
            }
        }
        return result;
    }

    /** Drop a 1x1 transparent PNG under each referenced SVG name so the CSS
     *  compiler's url(...) resolver succeeds. We also put the original
     *  SVG-named copy alongside in case the compiler insists on the .svg
     *  extension. */
    private void emitPlaceholders(Set<String> names) throws MojoExecutionException {
        if (names.isEmpty()) {
            return;
        }
        if (!svgPlaceholderDir.isDirectory() && !svgPlaceholderDir.mkdirs()) {
            getLog().warn("Could not create placeholder dir " + svgPlaceholderDir);
            return;
        }
        for (String name : names) {
            File out = new File(svgPlaceholderDir, name);
            if (!out.exists() || out.length() != PLACEHOLDER_PNG.length) {
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(PLACEHOLDER_PNG);
                } catch (IOException ex) {
                    throw new MojoExecutionException("Failed to write placeholder " + out, ex);
                }
            }
        }
        getLog().debug("Wrote " + names.size() + " SVG placeholder PNG(s) to " + svgPlaceholderDir);
    }

    private void emitRegistry(File packageDir, List<GeneratedClass> generated, long mtime) throws MojoExecutionException {
        File registryFile = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
        if (registryFile.exists() && registryFile.lastModified() >= mtime) {
            getLog().debug("SVG registry up-to-date.");
            return;
        }
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(registryFile), "UTF-8"))) {
            SVGTranscoder.writeRegistry(svgPackage, REGISTRY_CLASS_NAME, generated, w);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write SVG registry", ex);
        }
        getLog().info("Wrote SVG registry " + registryFile.getName() + " with " + generated.size() + " image(s)");
    }

    /** Map a {@code cn1-source-dpi} CSS keyword or number to a density code.
     *  Mirrors the keyword set the CN1 CSS compiler accepts for multi-image
     *  density buckets. */
    private static int densityForCssValue(String value) {
        if (value == null) {
            return 0;
        }
        String v = value.trim().toLowerCase().replace('_', '-');
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

    private void registerSourceRoot() {
        String path = svgOutputDir.getAbsolutePath();
        if (!project.getCompileSourceRoots().contains(path)) {
            project.addCompileSourceRoot(path);
            getLog().debug("Added compile source root " + path);
        }
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
            } else if (f.getName().toLowerCase().endsWith(".svg")) {
                out.add(f);
            }
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
            } else if (f.getName().toLowerCase().endsWith(".css")) {
                out.add(f);
            }
        }
    }

    private static String readFile(File f) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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

    private static long lastModified(List<File> files) {
        long max = 0;
        for (File f : files) {
            if (f.lastModified() > max) {
                max = f.lastModified();
            }
        }
        return max;
    }
}
