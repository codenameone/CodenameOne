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

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.codename1.maven.PathUtil.path;

/**
 * Packages the application as a runnable desktop jar when it has no {@code javase} module,
 * the work that module's {@code executable-jar} profile does with the dependency, jar and
 * antrun plugins.
 *
 * <p>The output has the names that module produces, so scripts and documentation that look
 * for them still find them: {@code target/<app>-javase-<version>.jar}, its dependencies in
 * {@code target/libs/}, and both zipped together as {@code target/<app>-javase-<version>.zip}.
 * The jar holds {@code common}'s classes, the JavaSE natives with the desktop stub, the
 * generated icons and desktop properties, and the desktop resources.</p>
 *
 * <p>A goal rather than the module's plugin executions because the executable-jar profile is
 * also active in {@code common} when a {@code javase} module does exist, and there
 * maven-jar-plugin, which cannot be skipped, would rewrite {@code common}'s own jar.</p>
 */
@Mojo(name = "javase-executable-jar", defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class JavaSEExecutableJarMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isHosting()) {
            return;
        }
        String packageName = properties.getProperty("codename1.packageName");
        String mainName = properties.getProperty("codename1.mainName");
        if (packageName == null || mainName == null) {
            throw new MojoFailureException("codename1.packageName and codename1.mainName must be set in "
                    + "codenameone_settings.properties to build the desktop jar.");
        }
        File buildDir = new File(project.getBuild().getDirectory());
        String finalName = javaseFinalName(project.getArtifactId(), project.getVersion());
        File libs = new File(buildDir, "libs");
        File distDir = new File(buildDir, finalName);
        File distLibs = new File(distDir, "libs");
        File jar = new File(buildDir, finalName + ".jar");

        List<String> classPath = new ArrayList<String>();
        try {
            FileUtils.deleteQuietly(distDir);
            for (Artifact artifact : project.getArtifacts()) {
                File file = artifact.getFile();
                if (file == null || !file.isFile() || !"jar".equals(artifact.getType())) {
                    continue;
                }
                FileUtils.copyFileToDirectory(file, libs);
                FileUtils.copyFileToDirectory(file, distLibs);
                classPath.add("libs/" + file.getName());
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to copy the desktop app's dependencies", ex);
        }

        File root = getCN1ProjectDir().getParentFile();
        List<File> contents = new ArrayList<File>();
        contents.add(new File(project.getBuild().getOutputDirectory()));
        contents.add(hostedNativesDir());
        contents.add(hostedDesktopResourcesDir());
        contents.add(new File(getCN1ProjectDir(), path("src", "desktop", "resources")));
        contents.add(new File(root, path("javase", "src", "desktop", "resources")));

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.MAIN_CLASS, packageName + "." + mainName + "Stub");
        attributes.put(Attributes.Name.CLASS_PATH, join(classPath));
        try {
            writeJar(jar, manifest, contents);
            FileUtils.copyFileToDirectory(jar, distDir);
            zipDirectory(distDir, new File(buildDir, finalName + ".zip"));
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write the desktop jar " + jar, ex);
        }
        getLog().info("Desktop app: " + jar);
    }

    /** {@code myapp-common} is packaged as {@code myapp-javase-<version>}, the javase module's name. */
    static String javaseFinalName(String commonArtifactId, String version) {
        String base = commonArtifactId.endsWith("-common")
                ? commonArtifactId.substring(0, commonArtifactId.length() - "-common".length())
                : commonArtifactId;
        return base + "-javase-" + version;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p);
        }
        return sb.toString();
    }

    /** The jar of `dirs`, earlier directories winning when two hold the same entry. */
    static void writeJar(File jar, Manifest manifest, List<File> dirs) throws IOException {
        jar.getParentFile().mkdirs();
        Set<String> seen = new HashSet<String>();
        seen.add("META-INF/MANIFEST.MF");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar), manifest)) {
            for (File dir : dirs) {
                if (dir.isDirectory()) {
                    addTree(out, dir, "", seen);
                }
            }
        }
    }

    private static void addTree(ZipOutputStream out, File dir, String prefix, Set<String> seen) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        java.util.Arrays.sort(children);
        for (File child : children) {
            String name = prefix + child.getName();
            if (child.isDirectory()) {
                if (seen.add(name + "/")) {
                    out.putNextEntry(new JarEntry(name + "/"));
                    out.closeEntry();
                }
                addTree(out, child, name + "/", seen);
            } else if (seen.add(name)) {
                out.putNextEntry(new JarEntry(name));
                copy(child, out);
                out.closeEntry();
            }
        }
    }

    private static void zipDirectory(File dir, File zip) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            addTree(out, dir, "", new HashSet<String>());
        }
    }

    private static void copy(File f, OutputStream out) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }
}
