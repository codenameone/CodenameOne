/*
 * Test utility: compile in-memory Java sources to .class files on disk.
 *
 * Uses JSR 199 (javax.tools.JavaCompiler). Requires a JDK (not a JRE) — the
 * plugin already runs on JDK, so this is fine.
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
import java.io.PrintWriter;
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

    private JavaSourceCompiler() { }

    /// Compiles the given `fullyQualifiedName -> source` map into `.class` files
    /// rooted at `outputClassDir`. Adds `extraClasspath` (typically the plugin's
    /// own test-classes directory so the @Route + Form + Router stubs resolve).
    public static void compile(Map<String, String> sources, File outputClassDir, List<File> extraClasspath)
            throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "No JavaCompiler available — JSR 199 requires a JDK, not a JRE");
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

            // Build the classpath: pre-existing classpath + extras.
            String existing = System.getProperty("java.class.path", "");
            List<File> cp = new ArrayList<File>();
            if (existing.length() > 0) {
                for (String s : existing.split(File.pathSeparator)) {
                    cp.add(new File(s));
                }
            }
            if (extraClasspath != null) cp.addAll(extraClasspath);
            fm.setLocation(StandardLocation.CLASS_PATH, cp);

            List<JavaFileObject> compilationUnits = new ArrayList<JavaFileObject>();
            for (Map.Entry<String, String> e : sources.entrySet()) {
                compilationUnits.add(new InMemorySource(e.getKey(), e.getValue()));
            }
            StringWriter compilerOut = new StringWriter();
            JavaCompiler.CompilationTask task = compiler.getTask(
                    compilerOut, fm, diags,
                    Arrays.asList("-Xlint:none", "-proc:none"),
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
