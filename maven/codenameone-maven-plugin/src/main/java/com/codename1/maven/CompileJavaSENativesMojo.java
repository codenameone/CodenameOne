/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectLayout;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Expand;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.codename1.maven.PathUtil.path;

/**
 * Compiles an application's JavaSE native code when it has no {@code javase} module.
 *
 * <p>In the full layout the {@code javase} module compiles {@code javase/src/main/java}
 * (the simulator's native-interface implementations), the {@code nativese} sources of its
 * cn1libs and the desktop stub, against the JavaSE port. An application without that
 * module still has all of them -- {@code generate-native-interfaces} writes the stubs to
 * {@code javase/src/main/java} whether or not a pom sits beside it -- and this goal compiles
 * them from {@code common} into {@code target/cn1-javase/classes}. The simulator, the JUnit
 * tests, the desktop app and the desktop builds put that directory on their classpath.</p>
 *
 * <p>Never into {@code target/classes}: that directory is what every device build uploads,
 * and an Android or iOS build must not carry Swing code. Nor through the main compile, whose
 * bytecode-compliance check would reject the JDK APIs native code exists to call.</p>
 */
@Mojo(name = "compile-javase-natives", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class CompileJavaSENativesMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isHosting()) {
            if (hostedPlatform == null) {
                getLog().warn("compile-javase-natives needs <hostedPlatform>javase</hostedPlatform>; skipping.");
            }
            return;
        }
        ProjectLayout layout = projectHost().layout();
        File out = hostedNativesDir();
        File buildDir = new File(project.getBuild().getDirectory());

        List<String> classpath = new ArrayList<String>();
        File cn1libSources = new File(buildDir, path("generated-sources", "cn1libs-javase"));
        // Rebuilt from the current archives every time, so a library that was upgraded
        // or removed leaves none of its old native sources behind to be compiled. The
        // archives keep their entries' timestamps, so an unchanged library still reads
        // as unchanged below.
        FileUtils.deleteQuietly(cn1libSources);
        try {
            String testOutput = new File(project.getBuild().getTestOutputDirectory()).getAbsolutePath();
            for (String element : project.getTestClasspathElements()) {
                File f = new File(element);
                if (f.getAbsolutePath().equals(testOutput)) {
                    continue;
                }
                if ("nativese.zip".equals(f.getName())) {
                    extract(f, cn1libSources);
                    continue;
                }
                classpath.add(element);
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to resolve the test classpath", ex);
        }

        List<File> roots = new ArrayList<File>();
        roots.add(layout.nativeSourceDir(NativePlatform.JAVASE));
        roots.add(cn1libSources);
        roots.add(new File(buildDir, path("generated-sources", "cn1-desktop")));
        roots.add(new File(layout.rootDir(), path("javase", "src", "desktop", "java")));
        List<File> sources = new ArrayList<File>();
        for (File root : roots) {
            collectJavaFiles(root, sources);
        }
        Collections.sort(sources);
        File resources = layout.nativeResourcesDir(NativePlatform.JAVASE);

        if (sources.isEmpty() && !resources.isDirectory()) {
            if (out.exists()) {
                FileUtils.deleteQuietly(out);
            }
            getLog().debug("No JavaSE native sources to compile");
            return;
        }

        String[] level = sourceLevel();
        File stamp = new File(out.getParentFile(), "inputs.txt");
        List<File> resourceFiles = new ArrayList<File>();
        collectFiles(resources, resourceFiles);
        Collections.sort(resourceFiles);
        String inputs = describeInputs(sources, resourceFiles, classpath, level);
        if (isUpToDate(stamp, inputs, sources, resources)) {
            getLog().debug("JavaSE native code is up to date at " + out);
            return;
        }

        FileUtils.deleteQuietly(out);
        if (!out.mkdirs() && !out.isDirectory()) {
            throw new MojoExecutionException("Could not create " + out);
        }
        try {
            if (resources.isDirectory()) {
                FileUtils.copyDirectory(resources, out);
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to copy " + resources + " to " + out, ex);
        }
        if (!sources.isEmpty()) {
            compile(sources, classpath, out, level);
        }
        try {
            FileUtils.writeStringToFile(stamp, inputs, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write " + stamp, ex);
        }
        getLog().info("Compiled " + sources.size() + " JavaSE native source file(s) into " + out);
    }

    private void compile(List<File> sources, List<String> classpath, File out, String[] level)
            throws MojoExecutionException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new MojoExecutionException("No Java compiler is available to compile the JavaSE native sources. "
                    + "Run Maven on a JDK, not a JRE.");
        }
        List<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(out.getAbsolutePath());
        args.add("-encoding");
        args.add("UTF-8");
        if (level[0] != null) {
            args.add("--release");
            args.add(level[0]);
        } else {
            args.add("-source");
            args.add(level[1]);
            args.add("-target");
            args.add(level[2]);
        }
        args.add("-nowarn");
        args.add("-cp");
        StringBuilder cp = new StringBuilder();
        for (String element : classpath) {
            if (cp.length() > 0) {
                cp.append(File.pathSeparator);
            }
            cp.append(element);
        }
        args.add(cp.toString());
        for (File source : sources) {
            args.add(source.getAbsolutePath());
        }
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int result = javac.run(null, null, err, args.toArray(new String[args.size()]));
        String output = new String(err.toByteArray(), StandardCharsets.UTF_8).trim();
        if (result != 0) {
            throw new MojoExecutionException("Compiling the JavaSE native sources failed:\n" + output);
        }
        if (output.length() > 0) {
            getLog().debug(output);
        }
    }

    /**
     * The level the application compiles at, read from its maven-compiler-plugin settings so
     * the native code matches it: {@code [release, source, target]}, release null unless set.
     */
    private String[] sourceLevel() {
        String release = project.getProperties().getProperty("maven.compiler.release");
        String source = project.getProperties().getProperty("maven.compiler.source", "1.8");
        String target = project.getProperties().getProperty("maven.compiler.target", source);
        for (Plugin plugin : project.getBuildPlugins()) {
            if (!"maven-compiler-plugin".equals(plugin.getArtifactId())
                    || !(plugin.getConfiguration() instanceof Xpp3Dom)) {
                continue;
            }
            Xpp3Dom config = (Xpp3Dom) plugin.getConfiguration();
            release = valueOf(config, "release", release);
            source = valueOf(config, "source", source);
            target = valueOf(config, "target", target);
        }
        return new String[] {release, source, target};
    }

    private static String valueOf(Xpp3Dom config, String name, String fallback) {
        Xpp3Dom child = config.getChild(name);
        if (child == null || child.getValue() == null) {
            return fallback;
        }
        String v = child.getValue().trim();
        return v.length() == 0 || v.startsWith("${") ? fallback : v;
    }

    /**
     * Everything the compile read, by name: the level, the classpath, the sources and the
     * resources. Names catch what timestamps cannot, a file that was deleted.
     */
    static String describeInputs(List<File> sources, List<File> resources, List<String> classpath,
                                 String[] level) {
        StringBuilder sb = new StringBuilder();
        sb.append("level=").append(level[0]).append('/').append(level[1]).append('/').append(level[2]).append('\n');
        for (String element : classpath) {
            sb.append("cp=").append(element).append('\n');
        }
        for (File source : sources) {
            sb.append("src=").append(source.getAbsolutePath()).append('\n');
        }
        for (File resource : resources) {
            sb.append("res=").append(resource.getAbsolutePath()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Up to date when the last compile saw exactly these inputs and nothing it read has
     * changed since: no source or resource is newer than the stamp, and no classpath
     * element either (a rebuilt {@code target/classes} changes what the natives link to).
     */
    static boolean isUpToDate(File stamp, String inputs, List<File> sources, File resources) {
        if (!stamp.isFile()) {
            return false;
        }
        try {
            if (!inputs.equals(FileUtils.readFileToString(stamp, StandardCharsets.UTF_8))) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
        long stamped = stamp.lastModified();
        for (File source : sources) {
            if (source.lastModified() > stamped) {
                return false;
            }
        }
        if (resources.isDirectory() && lastModifiedRecursive(resources) > stamped) {
            return false;
        }
        for (String line : inputs.split("\n")) {
            if (line.startsWith("cp=")) {
                File element = new File(line.substring(3));
                long modified = element.isDirectory() ? lastModifiedRecursive(element) : element.lastModified();
                if (modified > stamped) {
                    return false;
                }
            }
        }
        return true;
    }

    private void extract(File zip, File dest) {
        Expand unzip = (Expand) antProject.createTask("unzip");
        unzip.setSrc(zip);
        unzip.setDest(dest);
        unzip.execute();
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, out);
            } else {
                out.add(child);
            }
        }
    }

    private static void collectJavaFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectJavaFiles(child, out);
            } else if (child.getName().endsWith(".java")) {
                out.add(child);
            }
        }
    }
}
