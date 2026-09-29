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

import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// Finds the class a backend runs: the entry point annotation processing
/// generated for its `@RestController`s, else the one class that declares a
/// `main` method. Shared by the Maven `cn1:backend` goal and the Gradle
/// `runBackend` task.
public final class BackendMainClass {
    private final Log log;
    private final String mainClassOption;

    /// @param mainClassOption how the build tool lets a user name the class, for
    ///        the messages (`-Dcn1.backend.mainClass`, `-Pcn1.backend.mainClass`)
    public BackendMainClass(Log log, String mainClassOption) {
        this.log = log;
        this.mainClassOption = mainClassOption;
    }

    /// `explicit` when set, else the generated entry point, else the one class
    /// with a main method.
    public String resolve(File classesDir, String explicit) throws BuildFailureException {
        if (explicit != null && explicit.length() > 0) {
            return explicit;
        }
        String main = generatedMainClass(classesDir);
        return main != null && main.length() > 0 ? main : findMainClass(classesDir);
    }

    /**
     * The one class in this module with a main method.
     *
     * Deliberately an error when there are several rather than a guess: picking
     * one and running it is how a developer ends up debugging the wrong process.
     */
    /**
     * The entry point annotation processing generated, or null when this module
     * has none -- one written by hand, with no @RestController in it, has no
     * marker and falls through to the scan below.
     */
    public String generatedMainClass(File classesDir) {
        File marker = new File(classesDir,
                com.codename1.maven.processors.RestControllerAnnotationProcessor
                        .MAIN_CLASS_RESOURCE.replace('/', File.separatorChar));
        if (!marker.isFile()) {
            return null;
        }
        try {
            byte[] raw = new byte[(int) marker.length()];
            InputStream in = new java.io.FileInputStream(marker);
            try {
                int at = 0;
                while (at < raw.length) {
                    int n = in.read(raw, at, raw.length - at);
                    if (n <= 0) {
                        break;
                    }
                    at += n;
                }
            } finally {
                in.close();
            }
            String name = new String(raw, "UTF-8").trim();
            return name.length() == 0 ? null : name;
        } catch (IOException err) {
            // Unreadable is not the same as absent, and the scan below still has
            // a fair chance of being right; refusing outright would be worse.
            log.warn("cn1: could not read " + marker + ": " + err);
            return null;
        }
    }

    public String findMainClass(File classesDir) throws BuildFailureException {
        return findMainClass(java.util.Collections.singletonList(classesDir));
    }

    /// The one class with a main method across `classesDirs` -- Gradle compiles
    /// Java and Kotlin into separate directories, and a main in each is as
    /// ambiguous as two in one.
    public String findMainClass(List<File> classesDirs) throws BuildFailureException {
        List<String> found = new ArrayList<String>();
        for (File classesDir : classesDirs) {
            collectMainClasses(classesDir, classesDir, found);
        }
        File classesDir = classesDirs.size() == 1 ? classesDirs.get(0) : null;
        if (found.size() == 1) {
            return found.get(0);
        }
        if (found.isEmpty()) {
            throw new BuildFailureException("No class with a main method under "
                    + (classesDir != null ? classesDir : classesDirs) + "; set " + mainClassOption);
        }
        throw new BuildFailureException("Several classes have a main method ("
                + join(found, ", ") + "); choose one with " + mainClassOption);
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

    private static String join(List<String> parts, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() > 0) {
                sb.append(separator);
            }
            sb.append(p);
        }
        return sb.toString();
    }
}
