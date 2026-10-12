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

    private static final ThreadLocal<File> FORKED_JDK = new ThreadLocal<File>();

    /// Generated classes remain Java 8; use this JDK to read newer application
    /// bytecode even when the build tool itself runs on an older JVM.
    public static CompilerScope useJdk(File jdk) {
        return new CompilerScope(jdk);
    }

    public static final class CompilerScope implements AutoCloseable {
        private final File previous = FORKED_JDK.get();
        private CompilerScope(File jdk) { FORKED_JDK.set(jdk); }
        @Override public void close() {
            if (previous == null) FORKED_JDK.remove(); else FORKED_JDK.set(previous);
        }
    }

    /// The project's compile classpath while [com.codename1.maven.AnnotationProcessing]
    /// runs, appended to every compile below. Processors compile their generated
    /// sources against the output directory, which under Maven holds every
    /// project class. Under Gradle it does not: Kotlin and Java compile into
    /// directories of their own, so a Kotlin `@RestClient` returning a Java type
    /// generated code naming a class the compile could not see. Set here, at the
    /// one call every processor makes, rather than in each processor.
    private static final ThreadLocal<List<File>> PROJECT_CLASSPATH = new ThreadLocal<List<File>>();

    /// Makes `classpath` part of every compile on this thread until
    /// [#clearProjectClasspath()].
    public static void setProjectClasspath(List<File> classpath) {
        PROJECT_CLASSPATH.set(new ArrayList<File>(classpath));
    }

    /// Undoes [#setProjectClasspath(List)].
    public static void clearProjectClasspath() {
        PROJECT_CLASSPATH.remove();
    }

    /// Compiles the given `fullyQualifiedName -> source` map into `.class` files
    /// rooted at `outputClassDir`. Adds `extraClasspath` (typically the plugin's
    /// own test-classes directory so the @Route + Form + Router stubs resolve).
    ///
    /// Generated source stays at Java 8 for compatibility. This does not constrain
    /// the application's language level. Backend packaging supplies its selected
    /// JDK through useJdk so generated sources can refer to modern app classes.
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
        File forkedJdk = FORKED_JDK.get();
        if (forkedJdk != null) {
            compileForked(forkedJdk, sources, outputClassDir, extraClasspath, sourceLevel);
            return;
        }
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
            List<File> project = PROJECT_CLASSPATH.get();
            if (project != null) {
                cp.addAll(project);
            }
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

    private static void compileForked(File jdk, Map<String, String> sources, File output,
            List<File> extraClasspath, String level) throws IOException {
        if (sources.isEmpty()) return;
        java.nio.file.Path temporary = java.nio.file.Files.createTempDirectory("cn1-generated-java-");
        try {
            java.nio.file.Files.createDirectories(output.toPath());
            List<String> command = new ArrayList<String>(Arrays.asList(
                    new File(jdk, "bin/javac").getAbsolutePath(), "-Xlint:none", "-proc:none",
                    "-encoding", "UTF-8", "-d", output.getAbsolutePath()));
            if (level != null) command.addAll(Arrays.asList("-source", level, "-target", level));
            List<File> cp = new ArrayList<File>();
            cp.add(output);
            if (extraClasspath != null) cp.addAll(extraClasspath);
            if (PROJECT_CLASSPATH.get() != null) cp.addAll(PROJECT_CLASSPATH.get());
            StringBuilder paths = new StringBuilder();
            for (File file : cp) {
                if (paths.length() > 0) paths.append(File.pathSeparator);
                paths.append(file.getAbsolutePath());
            }
            command.addAll(Arrays.asList("-classpath", paths.toString()));
            for (Map.Entry<String, String> entry : sources.entrySet()) {
                java.nio.file.Path file = temporary.resolve(entry.getKey().replace('.', '/') + ".java");
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.write(file, entry.getValue().getBytes(StandardCharsets.UTF_8));
                command.add(file.toString());
            }
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            java.io.ByteArrayOutputStream diagnostics = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream stream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) diagnostics.write(buffer, 0, count);
            }
            try {
                if (process.waitFor() != 0) {
                    throw new IOException("Generated backend source compilation failed:\n"
                            + diagnostics.toString("UTF-8"));
                }
            } catch (InterruptedException ex) {
                process.destroy();
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted compiling generated backend sources", ex);
            }
        } finally {
            org.apache.commons.io.FileUtils.deleteDirectory(temporary.toFile());
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
