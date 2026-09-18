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
package com.codename1.maven.annotations;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class JavaSourceCompiler {

    /// The level every generated source is compiled at. See #compile.
    private static final String GENERATED_SOURCE_LEVEL = "1.8";

    private JavaSourceCompiler() { }

    /// Compiles the given `fullyQualifiedName -> source` map into `.class` files
    /// rooted at `outputClassDir`. Adds `extraClasspath` (typically the plugin's
    /// own test-classes directory so the @Route + Form + Router stubs resolve).
    ///
    /// JAVA 8 BYTECODE, WHATEVER JDK IS RUNNING. Everything the annotation
    /// processors generate is Codename One code, and every consumer of it reads
    /// class file version 52: ParparVM translates an app's generated routers,
    /// mappers and bindings, and `cn1:backend-package` hands the generated entry
    /// point straight to the translator. The class file version used to be the
    /// running JDK's default, so packaging a backend on a JDK 25 died inside the
    /// translator with "Unsupported class file major version 69" -- naming ASM and
    /// no source, on a class the developer never wrote. It stayed invisible
    /// because that goal demanded a JDK 8 until this was fixed.
    ///
    /// Pass a level explicitly through the overload below to compile a source that
    /// needs a newer language than 8 -- a record, say. Only a test has reason to:
    /// generated source that cannot be translated is of no use to a build.
    public static void compile(Map<String, String> sources, File outputClassDir, List<File> extraClasspath)
            throws IOException {
        compile(sources, outputClassDir, extraClasspath, GENERATED_SOURCE_LEVEL);
    }

    /// As #compile, at a named `-source`/`-target` level. A null level leaves both
    /// out, which compiles at the running JDK's own default.
    ///
    /// -source/-target rather than --release, because this plugin still compiles
    /// and runs on a JDK 8. The obsolete-option warning newer compilers print for
    /// -source 8 is a lint warning, and -Xlint:none below turns those off.
    public static void compile(Map<String, String> sources, File outputClassDir,
            List<File> extraClasspath, String sourceLevel)
            throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "No JavaCompiler available -- JSR 199 requires a JDK, not a JRE");
        }
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fm = compiler.getStandardFileManager(
                diags, Locale.ROOT, StandardCharsets.UTF_8);
        try {
            if (!outputClassDir.exists() && !outputClassDir.mkdirs()) {
                throw new IOException("Could not create " + outputClassDir);
            }
            fm.setLocation(StandardLocation.CLASS_OUTPUT,
                    Collections.singletonList(outputClassDir));

            // Build the classpath: pre-existing classpath + extras. Surefire
            // sets `surefire.test.class.path` to the resolved test classpath
            // when it forks the JVM; under newer surefire releases
            // `java.class.path` only carries the surefire-booter jar, not the
            // project's deps. Prefer the surefire-provided value when set so
            // generated-source compilation can see test-scoped jars (e.g.
            // codenameone-core for @Route fixtures). Also walk the current
            // classloader's URL list as a last-resort fallback.
            String surefireCp = System.getProperty("surefire.test.class.path");
            String existing = (surefireCp != null && surefireCp.length() > 0)
                    ? surefireCp
                    : System.getProperty("java.class.path", "");
            List<File> cp = new ArrayList<File>();
            if (existing.length() > 0) {
                for (String s : existing.split(File.pathSeparator)) {
                    cp.add(new File(s));
                }
            }
            ClassLoader loader = JavaSourceCompiler.class.getClassLoader();
            if (loader instanceof java.net.URLClassLoader) {
                for (java.net.URL u : ((java.net.URLClassLoader) loader).getURLs()) {
                    if ("file".equals(u.getProtocol())) {
                        File f = urlToFile(u);
                        if (f != null) {
                            cp.add(f);
                        }
                    }
                }
            }
            if (extraClasspath != null) cp.addAll(extraClasspath);
            fm.setLocation(StandardLocation.CLASS_PATH, cp);

            List<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
            for (Map.Entry<String, String> e : sources.entrySet()) {
                compilationUnits.add(new InMemorySource(e.getKey(), e.getValue()));
            }
            StringWriter compilerOut = new StringWriter();
            List<String> options = new ArrayList<String>(
                    Arrays.asList("-Xlint:none", "-proc:none"));
            if (sourceLevel != null) {
                options.add("-source");
                options.add(sourceLevel);
                options.add("-target");
                options.add(sourceLevel);
            }
            JavaCompiler.CompilationTask task = compiler.getTask(
                    compilerOut, fm, diags, options,
                    /*classes*/ null, compilationUnits);
            Boolean ok = task.call();
            if (ok == null || !ok.booleanValue()) {
                StringBuilder sb = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                    sb.append("  ").append(d.toString()).append('\n');
                }
                sb.append("compiler output: ").append(compilerOut.toString());
                throw new IOException(sb.toString());
            }
        } finally {
            fm.close();
        }
    }

    /// Convert a `file:` URL to a `File`. Returns null when the URL can't be
    /// turned into a path (non-hierarchical URI, opaque URL, etc.) so callers
    /// can simply skip the entry instead of failing the build.
    private static File urlToFile(java.net.URL u) {
        try {
            return new File(u.toURI());
        } catch (java.net.URISyntaxException e) {
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Map<String, String> singleSource(String fqn, String src) {
        Map<String, String> m = new HashMap<String, String>();
        m.put(fqn, src);
        return m;
    }

    private static final class InMemorySource extends javax.tools.SimpleJavaFileObject {
        private final String content;

        InMemorySource(String fullyQualifiedName, String content) {
            super(URI.create("string:///" + fullyQualifiedName.replace('.', '/')
                    + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }
}
