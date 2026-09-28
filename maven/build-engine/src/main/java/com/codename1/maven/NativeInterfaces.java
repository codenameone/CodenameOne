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

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectLayout;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Finds a project's native interfaces and generates or checks their platform
/// implementations.
///
/// Where the implementations go is the project layout's business: the platform
/// modules of a Maven project, `src/<platform>/<lang>` in a Gradle one. Nothing
/// here creates a platform directory until a stub is written into it, so a
/// project that never declares a native interface never gets one.
public final class NativeInterfaces {
    private static final String NATIVE_INTERFACE = "com/codename1/system/NativeInterface";

    private final Log log;
    private final File classesDir;
    private final List<String> classpath;

    /// @param classesDir the project's compiled classes
    /// @param classpath the compile classpath the interfaces are loaded with
    public NativeInterfaces(Log log, File classesDir, List<String> classpath) {
        this.log = log;
        this.classesDir = classesDir;
        this.classpath = classpath == null ? Collections.<String>emptyList() : classpath;
    }

    /// The binary names of every type in the classes directory that directly
    /// extends `NativeInterface`.
    public List<String> find() throws IOException {
        List<String> out = new ArrayList<String>();
        scan(classesDir, out);
        Collections.sort(out);
        return out;
    }

    private static void scan(File dir, final List<String> out) throws IOException {
        File[] list = dir.listFiles();
        if (list == null) {
            return;
        }
        for (File current : list) {
            if (current.isDirectory()) {
                scan(current, out);
                continue;
            }
            if (!current.getName().endsWith(".class")) {
                continue;
            }
            InputStream is = new FileInputStream(current);
            try {
                new ClassReader(is).accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String signature, String superName,
                                      String[] interfaces) {
                        boolean found = NATIVE_INTERFACE.equals(superName);
                        if (interfaces != null) {
                            for (String s : interfaces) {
                                found |= NATIVE_INTERFACE.equals(s);
                            }
                        }
                        if (found) {
                            out.add(name.replace('/', '.'));
                        }
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            } finally {
                is.close();
            }
        }
    }

    private URLClassLoader loader() throws IOException {
        List<URL> urls = new ArrayList<URL>();
        urls.add(classesDir.toURI().toURL());
        for (String element : classpath) {
            File f = new File(element);
            if (f.exists() && !f.equals(classesDir)) {
                urls.add(f.toURI().toURL());
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), NativeInterfaces.class.getClassLoader());
    }

    private StubGenerator generatorFor(ClassLoader cl, String name, boolean swift, boolean kotlin)
            throws BuildExecutionException {
        Class<?> c;
        try {
            c = cl.loadClass(name);
        } catch (ClassNotFoundException | LinkageError ex) {
            throw new BuildExecutionException("Could not load native interface " + name
                    + ". Compile the project first.", ex);
        }
        StubGenerator g = StubGenerator.create(log, c, swift, kotlin);
        String problem = g.verify();
        if (problem != null) {
            throw new BuildFailureException("Native interface " + name + " is not valid: " + problem);
        }
        return g;
    }

    /// Writes stubs for every native interface, or only `only` (a simple or
    /// binary name) when it is not null, into `layout`.
    ///
    /// An interface whose implementation files already exist is left alone
    /// unless `overwrite` is set, as the Maven goal always did.
    ///
    /// @param overwriteHint how to ask for an overwrite, for the message
    /// @return the files written
    public List<File> generate(ProjectLayout layout, String only, boolean swift, boolean kotlin, boolean overwrite,
                               String overwriteHint) throws BuildExecutionException {
        List<File> written = new ArrayList<File>();
        List<String> names;
        try {
            names = find();
        } catch (IOException ex) {
            throw new BuildExecutionException("Failed to scan " + classesDir + " for native interfaces", ex);
        }
        boolean matched = false;
        URLClassLoader cl = null;
        try {
            cl = loader();
            for (String name : names) {
                if (only != null && !only.equals(name) && !name.endsWith("." + only)) {
                    continue;
                }
                matched = true;
                StubGenerator g = generatorFor(cl, name, swift, kotlin);
                List<File> before = g.getExistingFiles(layout);
                if (!overwrite && !before.isEmpty()) {
                    log.warn("Native interface files already exist for " + name + ". Skipping generation.");
                    for (File f : before) {
                        log.warn("Existing file: " + f.getAbsolutePath());
                    }
                    log.warn("To overwrite these files, run: " + overwriteHint);
                    continue;
                }
                g.generateCode(layout, overwrite);
                for (File f : g.getExistingFiles(layout)) {
                    if (overwrite || !before.contains(f)) {
                        written.add(f);
                    }
                }
            }
        } catch (IOException ex) {
            throw new BuildExecutionException("Failed to generate native interface stubs", ex);
        } finally {
            closeQuietly(cl);
        }
        if (only != null && !matched) {
            throw new BuildFailureException("No native interface named " + only + " was found in " + classesDir
                    + ". Native interfaces found: " + names);
        }
        if (names.isEmpty()) {
            log.info("No native interfaces found in " + classesDir + ". Declare one by extending "
                    + "com.codename1.system.NativeInterface, compile, and run this again.");
        }
        return written;
    }

    /// The implementation files `platform` is missing, one entry per native
    /// interface that has none, naming the file to create. Empty when every
    /// interface is implemented (or there are none).
    public List<String> missingImplementations(ProjectLayout layout, NativePlatform platform)
            throws BuildExecutionException {
        List<String> out = new ArrayList<String>();
        List<String> names;
        try {
            names = find();
        } catch (IOException ex) {
            throw new BuildExecutionException("Failed to scan " + classesDir + " for native interfaces", ex);
        }
        if (names.isEmpty()) {
            return out;
        }
        URLClassLoader cl = null;
        try {
            cl = loader();
            for (String name : names) {
                StubGenerator g = generatorFor(cl, name, false, false);
                if (!g.hasImplementation(layout, platform)) {
                    StringBuilder sb = new StringBuilder(name).append(" has no ").append(platform.id())
                            .append(" implementation; missing ");
                    boolean first = true;
                    for (File f : g.requiredFiles(layout, platform)) {
                        if (f.exists()) {
                            continue;
                        }
                        if (!first) {
                            sb.append(" and ");
                        }
                        first = false;
                        sb.append(f.getPath());
                    }
                    out.add(sb.toString());
                }
            }
        } catch (IOException ex) {
            throw new BuildExecutionException("Failed to load native interfaces", ex);
        } finally {
            closeQuietly(cl);
        }
        return out;
    }

    private static void closeQuietly(URLClassLoader cl) {
        if (cl == null) {
            return;
        }
        try {
            cl.close();
        } catch (IOException ignored) {
            // A class loader over local files; nothing to recover.
        }
    }
}
