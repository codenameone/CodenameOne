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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scans an application module for SVG files, transcodes each to a Codename One
 * {@code Image} subclass under {@code target/generated-sources/svg}, and emits
 * a registry class that auto-installs the generated images into any
 * {@code Resources} object opened at runtime.
 *
 * <p>SVG sources are picked up from {@code src/main/svg/} by default. The generated
 * sources directory is automatically added to the project's compile source roots
 * so the resulting Java classes participate in the normal compilation pass.</p>
 *
 * <p>This mojo intentionally does not require a network or any external tool --
 * SVG parsing is done in-process by {@code codenameone-svg-transcoder} using
 * the JDK's StAX implementation.</p>
 */
@Mojo(name = "transcode-svg", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.NONE,
        requiresDependencyCollection = ResolutionScope.NONE)
public class TranscodeSVGMojo extends AbstractCN1Mojo {

    /** Default location for SVG sources, relative to the project base. */
    private static final String DEFAULT_SVG_DIR = "src/main/svg";

    /** Default package for generated SVG image classes. */
    private static final String DEFAULT_PACKAGE = "com.codename1.generated.svg";

    /** Class name used for the auto-generated registry -- must match the
     *  class looked up reflectively by {@code Resources.ensureGeneratedSVGsInstalled}. */
    private static final String REGISTRY_CLASS_NAME = "SVGRegistry";

    @Parameter(property = "cn1.svg.sourceDir")
    private File svgSourceDir;

    @Parameter(property = "cn1.svg.outputDir",
            defaultValue = "${project.build.directory}/generated-sources/svg")
    private File svgOutputDir;

    @Parameter(property = "cn1.svg.package", defaultValue = DEFAULT_PACKAGE)
    private String svgPackage;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File srcDir = svgSourceDir != null ? svgSourceDir : new File(project.getBasedir(), DEFAULT_SVG_DIR);
        if (!srcDir.isDirectory()) {
            getLog().debug("No SVG source directory at " + srcDir + " -- skipping SVG transcoding.");
            registerSourceRoot();
            return;
        }

        List<File> svgs = new ArrayList<File>();
        collect(srcDir, svgs);
        if (svgs.isEmpty()) {
            getLog().debug("No .svg files found under " + srcDir);
            registerSourceRoot();
            return;
        }
        // sort so generated output is deterministic
        svgs.sort(new Comparator<File>() {
            @Override
            public int compare(File a, File b) { return a.getAbsolutePath().compareTo(b.getAbsolutePath()); }
        });

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
            generated.add(new GeneratedClass(svgPackage, className, resourceName));
        }

        File registryFile = new File(packageDir, REGISTRY_CLASS_NAME + ".java");
        if (!registryFile.exists() || registryFile.lastModified() < registrySrcMtime) {
            try {
                Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(registryFile), "UTF-8"));
                try {
                    SVGTranscoder.writeRegistry(svgPackage, REGISTRY_CLASS_NAME, generated, w);
                } finally {
                    w.close();
                }
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to write SVG registry", ex);
            }
            getLog().info("Wrote SVG registry " + registryFile.getName() + " with " + generated.size() + " image(s)");
        } else {
            getLog().debug("SVG registry up-to-date.");
        }

        registerSourceRoot();
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
