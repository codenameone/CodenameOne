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
package com.codenameone.devruntime;

import com.codename1.io.Util;
import com.codename1.tools.javac.Diagnostic;
import com.codename1.tools.javac.JavaCompiler;
import com.codename1.tools.javac.StubLibrary;
import com.codename1.tools.translator.InterpBundleWriter;
import com.codename1.ui.Display;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Compiles a project on the device and packages it the way a desktop push
/// does, so what the editor runs is exactly what a push would have run.
///
/// The compiler is `codenameone-javac`, the same one the Playground runs in a
/// browser. It compiles against `/devruntime-api.cn1stubs`, a stub library of
/// the device's `java.*` and the Codename One API that the app build writes,
/// so a call that compiles here is one the runtime can link.
public final class IdeBuild {
    private static final String LIFECYCLE = "com/codename1/system/Lifecycle";
    private static StubLibrary library;

    private IdeBuild() {
    }

    /// The outcome of one build.
    public static final class Result {
        final List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        byte[] bundle;
        String mainClass;
        long compileMillis;
        long packageMillis;
        String failure;

        public boolean isSuccess() {
            return bundle != null;
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        public byte[] getBundle() {
            return bundle;
        }

        public String getMainClass() {
            return mainClass;
        }

        /// One line saying what happened, for the console.
        public String summary() {
            if (failure != null) {
                return failure;
            }
            int errors = 0;
            for (Diagnostic d : diagnostics) {
                if (d.error) {
                    errors++;
                }
            }
            if (bundle == null) {
                return errors + (errors == 1 ? " error" : " errors") + " (compiled in "
                        + compileMillis + "ms)";
            }
            return "Compiled in " + compileMillis + "ms, packaged in " + packageMillis + "ms";
        }
    }

    /// The API stub library, read once. Several megabytes, so it is kept.
    static synchronized StubLibrary library() throws IOException {
        if (library == null) {
            InputStream in = Display.getInstance().getResourceAsStream(IdeBuild.class,
                    "/devruntime-api.cn1stubs");
            if (in == null) {
                throw new IOException("the API stub library is missing from this build");
            }
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Util.copy(in, bos);
                library = new StubLibrary(bos.toByteArray());
            } finally {
                Util.cleanup(in);
            }
        }
        return library;
    }

    /// Compiles every `.java` file in the project. Never throws: a broken
    /// project is a result with diagnostics, not an exception.
    public static Result build(IdeProject project) {
        Result r = new Result();
        try {
            long start = System.currentTimeMillis();
            JavaCompiler compiler = new JavaCompiler(library());
            List<String> sources = new ArrayList<String>();
            for (String path : project.getPaths()) {
                if (path.endsWith(".java")) {
                    compiler.addSource(path, project.getText(path));
                    sources.add(path);
                }
            }
            if (sources.isEmpty()) {
                r.failure = "The project has no .java files";
                return r;
            }
            JavaCompiler.Result compiled = compiler.compile();
            r.compileMillis = System.currentTimeMillis() - start;
            r.diagnostics.addAll(compiled.getDiagnostics());
            if (!compiled.isSuccess()) {
                return r;
            }
            Map<String, byte[]> classes = compiled.getClasses();
            r.mainClass = findEntryPoint(compiled);
            if (r.mainClass == null) {
                r.failure = "Nothing to run: no class has a main method or extends Lifecycle";
                return r;
            }

            start = System.currentTimeMillis();
            InterpBundleWriter w = new InterpBundleWriter();
            for (byte[] c : classes.values()) {
                w.addClass(c);
            }
            // The runtime refuses a bundle whose classes are not all covered by
            // source, and keys each one on the SourceFile attribute: the file's
            // own name, in its package.
            for (String path : sources) {
                String text = project.getText(path);
                String file = path.substring(path.lastIndexOf('/') + 1);
                w.addSource(InterpBundleWriter.sourceKey(
                        InterpBundleWriter.packageOf(text, file), file), text);
            }
            w.setMainClass(r.mainClass);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            w.write(bos);
            r.bundle = bos.toByteArray();
            r.packageMillis = System.currentTimeMillis() - start;
        } catch (Throwable t) {
            r.bundle = null;
            r.failure = "Build failed: " + t;
        }
        return r;
    }

    /// A class with `static void main(String[])`, else a `Lifecycle`
    /// subclass -- the same rule a desktop push applies.
    static String findEntryPoint(JavaCompiler.Result compiled) {
        List<JavaCompiler.ClassInfo> infos = compiled.getClassInfos();
        for (JavaCompiler.ClassInfo c : infos) {
            if (c.isTopLevel() && c.declares("main", "([Ljava/lang/String;)V", true)) {
                return c.getName();
            }
        }
        for (JavaCompiler.ClassInfo c : infos) {
            if (!c.isAbstract() && extendsLifecycle(c, infos, 0)) {
                return c.getName();
            }
        }
        return null;
    }

    private static boolean extendsLifecycle(JavaCompiler.ClassInfo c,
                                            List<JavaCompiler.ClassInfo> infos, int depth) {
        String sup = c.getSuperclass();
        if (sup == null || depth > 32) {
            return false;
        }
        if (LIFECYCLE.equals(sup)) {
            return true;
        }
        for (JavaCompiler.ClassInfo o : infos) {
            if (sup.equals(o.getName())) {
                return extendsLifecycle(o, infos, depth + 1);
            }
        }
        return false;
    }
}
