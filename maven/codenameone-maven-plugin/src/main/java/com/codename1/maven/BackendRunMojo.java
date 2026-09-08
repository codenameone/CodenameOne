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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs a backend module on this JVM: `mvn cn1:backend`.
 *
 * The point is speed. The same handler translated to a native binary takes about
 * a minute and a half to build; here it starts in a couple of seconds, because
 * the shared runtime (`codenameone-backend`) is ordinary Java compiled against
 * the JDK and only the classes underneath it differ. Nothing in the protocol
 * layer is a stand-in -- it is the same source that ships -- so what runs here
 * behaves the way the deployed binary does.
 *
 * What this local runtime deliberately does NOT do is terminate TLS, and
 * therefore serve HTTP/2: Tls and Http2 refuse with a message saying so. A second
 * SSLEngine-based handshake would have its own bugs rather than production's,
 * which is worse than not having it because it looks like coverage. Run
 * `cn1:backend-package` when TLS is what you need to exercise.
 */
// Forks the lifecycle up to process-classes first, so `mvn cn1:backend` on its own
// does the obvious thing on a clean checkout instead of failing on an empty
// target/classes.
//
// process-classes rather than compile because that is where process-annotations is
// bound: a server written as @RestController classes has its router and its main
// GENERATED there, so stopping at compile would leave this goal looking for an entry
// point that the build had not produced yet.
@Execute(phase = LifecyclePhase.PROCESS_CLASSES)
@Mojo(name = "backend", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BackendRunMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The class to run. Found automatically when the module has exactly one class
     * with a main method, which is the usual shape.
     */
    @Parameter(property = "cn1.backend.mainClass")
    private String mainClass;

    /** Arguments for the program, space separated. */
    @Parameter(property = "cn1.backend.args")
    private String args;

    /** Extra JVM options, space separated. */
    @Parameter(property = "cn1.backend.jvmArgs")
    private String jvmArgs;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File classes = new File(project.getBuild().getOutputDirectory());
        if (!classes.isDirectory()) {
            throw new MojoFailureException("Nothing is compiled in "
                    + classes + "; run `mvn compile` first, or `mvn compile cn1:backend`");
        }

        List<String> classpath = new ArrayList<String>();
        classpath.add(classes.getAbsolutePath());
        try {
            for (Object element : project.getRuntimeClasspathElements()) {
                String path = String.valueOf(element);
                if (!classpath.contains(path)) {
                    classpath.add(path);
                }
            }
        } catch (Exception err) {
            throw new MojoExecutionException("Could not resolve the runtime classpath", err);
        }

        String main = mainClass;
        if (main == null || main.length() == 0) {
            main = findMainClass(classes);
        }

        List<String> command = new ArrayList<String>();
        command.add(javaExecutable());
        if (jvmArgs != null && jvmArgs.trim().length() > 0) {
            command.addAll(Arrays.asList(jvmArgs.trim().split("\\s+")));
        }
        command.add("-cp");
        command.add(join(classpath, File.pathSeparator));
        command.add(main);
        if (args != null && args.trim().length() > 0) {
            command.addAll(Arrays.asList(args.trim().split("\\s+")));
        }

        getLog().info("Running " + main + " on " + System.getProperty("java.version"));
        try {
            ProcessBuilder run = new ProcessBuilder(command);
            run.directory(project.getBasedir());
            // Inherited rather than captured: a server logs as it serves, and a
            // developer watching `cn1:backend` wants those lines as they happen.
            // It also means Ctrl-C reaches the server, so its shutdown handler
            // runs and in-flight requests finish.
            run.inheritIO();
            Process process = run.start();
            int status = process.waitFor();
            if (status != 0) {
                throw new MojoFailureException(main + " exited with status " + status);
            }
        } catch (IOException err) {
            throw new MojoExecutionException("Could not start " + main, err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while running " + main, err);
        }
    }

    /**
     * The one class in this module with a main method.
     *
     * Deliberately an error when there are several rather than a guess: picking
     * one and running it is how a developer ends up debugging the wrong process.
     */
    private String findMainClass(File classesDir) throws MojoFailureException {
        List<String> found = new ArrayList<String>();
        collectMainClasses(classesDir, classesDir, found);
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new MojoFailureException("No class with a main method under "
                    + classesDir + "; set -Dcn1.backend.mainClass");
        }
        throw new MojoFailureException("Several classes have a main method ("
                + join(found, ", ") + "); choose one with -Dcn1.backend.mainClass");
    }

    private void collectMainClasses(File root, File dir, List<String> found) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectMainClasses(root, child, found);
            } else if (child.getName().endsWith(".class") && child.getName().indexOf('$') < 0) {
                String name = child.getAbsolutePath()
                        .substring(root.getAbsolutePath().length() + 1)
                        .replace(File.separatorChar, '.');
                name = name.substring(0, name.length() - ".class".length());
                if (hasMainMethod(child)) {
                    found.add(name);
                }
            }
        }
    }

    /**
     * Whether the class DECLARES `public static void main(String[])`.
     *
     * Read from the class file rather than by loading it: loading runs the static
     * initialiser, and a backend's initialiser is as likely as not to open a
     * socket or a database. The method table is read with ASM rather than by
     * searching the bytes, because the constant pool of a class that merely CALLS
     * main carries the same two strings.
     */
    private boolean hasMainMethod(File classFile) {
        final boolean[] found = new boolean[1];
        try {
            InputStream in = new java.io.FileInputStream(classFile);
            try {
                new org.objectweb.asm.ClassReader(in).accept(
                        new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
                            @Override
                            public org.objectweb.asm.MethodVisitor visitMethod(int access,
                                    String name, String descriptor, String signature,
                                    String[] exceptions) {
                                int wanted = org.objectweb.asm.Opcodes.ACC_PUBLIC
                                        | org.objectweb.asm.Opcodes.ACC_STATIC;
                                if ("main".equals(name)
                                        && "([Ljava/lang/String;)V".equals(descriptor)
                                        && (access & wanted) == wanted) {
                                    found[0] = true;
                                }
                                return null;
                            }
                        },
                        org.objectweb.asm.ClassReader.SKIP_CODE
                                | org.objectweb.asm.ClassReader.SKIP_DEBUG
                                | org.objectweb.asm.ClassReader.SKIP_FRAMES);
            } finally {
                in.close();
            }
        } catch (Exception err) {
            return false;
        }
        return found[0];
    }

    private static String javaExecutable() {
        File home = new File(System.getProperty("java.home"));
        File candidate = new File(home, "bin/java");
        return candidate.isFile() ? candidate.getAbsolutePath() : "java";
    }

    private static String join(List<String> parts, String separator) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < parts.size(); iter++) {
            if (iter > 0) {
                out.append(separator);
            }
            out.append(parts.get(iter));
        }
        return out.toString();
    }
}
