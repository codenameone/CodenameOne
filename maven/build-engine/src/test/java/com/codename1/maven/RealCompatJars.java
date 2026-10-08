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

import com.codename1.build.BuildArtifact;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/// The jars a desktop application is really built against -- the layers'
/// runtimes, the shared JDK classes, the framework and the device's class
/// library -- as this module's test classpath holds them.
///
/// A test that compiles a fixture against these and remaps it sees what a
/// project's build sees, where the other tests use a runtime of three
/// classes written with ASM.
final class RealCompatJars {

    private RealCompatJars() {
    }

    private static List<File> testClasspath() {
        List<File> out = new ArrayList<File>();
        // Surefire starts the tests from a jar whose manifest holds the
        // classpath, and says what that classpath is in a property.
        for (String property : new String[] {"surefire.test.class.path", "java.class.path"}) {
            String value = System.getProperty(property);
            if (value != null) {
                for (String entry : value.split(File.pathSeparator)) {
                    if (entry.length() > 0) {
                        out.add(new File(entry));
                    }
                }
            }
        }
        return out;
    }

    /// The jar of `artifactId`. A reactor build that stops before `package`
    /// has the module's classes directory on the classpath instead; that is
    /// packed into `scratch` under the name a build would give it, since the
    /// remap step knows a runtime by its jar's name.
    static File jar(String artifactId, File scratch) throws IOException {
        for (File f : testClasspath()) {
            if (f.isFile() && f.getName().startsWith(artifactId + "-") && f.getName().endsWith(".jar")
                    && Character.isDigit(f.getName().charAt(artifactId.length() + 1))) {
                return f;
            }
        }
        for (File f : testClasspath()) {
            // .../maven/<module>/target/classes
            File module = f.getParentFile() == null ? null : f.getParentFile().getParentFile();
            String moduleName = artifactId.startsWith("codenameone-")
                    ? artifactId.substring("codenameone-".length()) : artifactId;
            if (f.isDirectory() && "classes".equals(f.getName()) && module != null
                    && module.getName().equals(moduleName)) {
                File out = new File(scratch, artifactId + "-0.jar");
                if (!out.isFile()) {
                    JarOutputStream jar = new JarOutputStream(new FileOutputStream(out));
                    try {
                        pack(f, "", jar);
                    } finally {
                        jar.close();
                    }
                }
                return out;
            }
        }
        throw new AssertionError(artifactId + " is not on the test classpath: " + testClasspath());
    }

    private static void pack(File dir, String rel, JarOutputStream jar) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                pack(f, rel + f.getName() + "/", jar);
            } else {
                jar.putNextEntry(new JarEntry(rel + f.getName()));
                jar.write(Files.readAllBytes(f.toPath()));
                jar.closeEntry();
            }
        }
    }

    static File swing(File scratch) throws IOException {
        return jar(CompatLayers.SWING.artifactId(), scratch);
    }

    static File javafx(File scratch) throws IOException {
        return jar(CompatLayers.JAVAFX.artifactId(), scratch);
    }

    static File jdk(File scratch) throws IOException {
        return jar(Relocation.JDK_ARTIFACT, scratch);
    }

    static File core(File scratch) throws IOException {
        return jar("codenameone-core", scratch);
    }

    /// A project host whose dependencies are the real jars, scoped as the
    /// project templates scope them, for running the compliance check over
    /// `classes`.
    static TestProjectHost host(File classes, File buildDir, File scratch) throws IOException {
        TestProjectHost host = TestProjectHost.empty();
        host.buildDir = buildDir;
        host.outputDir = classes;
        host.log = CompatRemapperTest.LOG;
        add(host, "java-runtime", "provided", jar("java-runtime", scratch));
        add(host, "codenameone-core", "provided", core(scratch));
        add(host, CompatLayers.SWING.artifactId(), "provided", swing(scratch));
        add(host, CompatLayers.JAVAFX.artifactId(), "provided", javafx(scratch));
        add(host, Relocation.JDK_ARTIFACT, "provided", jdk(scratch));
        return host;
    }

    private static void add(TestProjectHost host, String artifactId, String scope, File jar) {
        host.artifacts.add(new BuildArtifact("com.codenameone", artifactId, "1", null, "jar", scope, jar, null));
    }
}
