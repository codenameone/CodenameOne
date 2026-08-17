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
package com.codename1.interp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Compiles a small Java program, runs it twice -- once on this JVM, once in the
 * interpreter -- and compares what it printed.
 *
 * <p>Differential testing is the only practical way to be confident about an
 * interpreter. There are hundreds of behaviours to get right (integer overflow,
 * NaN comparisons, {@code dup2_x2} on mixed categories, which exception handler
 * wins) and a hand-written expectation for each is both laborious and only as
 * good as the author's memory of the spec. Running the same bytecode on a real
 * JVM produces the authoritative answer for free.</p>
 *
 * <p>The harness drives the real bundle pipeline -- javac, then the translator's
 * {@code InterpBundleWriter}, then {@link InterpBundleReader} -- so the format
 * and both of its ends are exercised on every case rather than being assumed.</p>
 *
 * @author Shai Almog
 */
final class InterpTestHarness {
    private InterpTestHarness() {
    }

    /** Output of one run: what the program printed, or how it failed. */
    static final class Result {
        final String output;
        final String failure;

        Result(String output, String failure) {
            this.output = output;
            this.failure = failure;
        }

        boolean failed() {
            return failure != null;
        }

        public String toString() {
            return failed() ? "FAILED: " + failure : output;
        }
    }

    /**
     * Compiles {@code source} (a single class named {@code className} with a
     * {@code main}), runs it on this JVM and in the interpreter, and returns
     * both results.
     */
    static Result[] runBoth(String className, String source) throws Exception {
        Path dir = Files.createTempDirectory("interp-conformance");
        Path src = dir.resolve(className + ".java");
        Files.write(src, source.getBytes(StandardCharsets.UTF_8));

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            throw new IllegalStateException("no system Java compiler; run tests on a JDK");
        }
        // -g keeps the line table, which the runtime requires: a bundle without
        // source information is refused, since the user has to be able to read
        // what runs.
        //
        // -XDstringConcat=inline is not optional. From JDK 9 onwards javac
        // compiles `"a" + b` to an invokedynamic against StringConcatFactory,
        // and ParparVM has no runtime invokedynamic at all -- the translator
        // desugars it at build time. A pushed bundle has no such pass, so the
        // push pipeline compiles concatenation the old way, to StringBuilder.
        // This is the same flag the real pipeline uses; the harness passes it
        // so the corpus is compiled exactly as pushed code will be.
        int rc = javac.run(null, null, null,
                "-g", "-nowarn", "-XDstringConcat=inline",
                "-d", dir.toString(), src.toString());
        if (rc != 0) {
            throw new IllegalStateException("fixture did not compile");
        }

        return new Result[]{
                runOnJvm(dir, className),
                runInInterpreter(dir, className, source)
        };
    }

    private static Result runOnJvm(Path dir, String className) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        URLClassLoader loader = null;
        try {
            loader = new URLClassLoader(new URL[]{dir.toUri().toURL()}, null);
            Class<?> c = Class.forName(className, true, loader);
            Method main = c.getMethod("main", String[].class);
            System.setOut(new PrintStream(captured, true, "UTF-8"));
            main.invoke(null, (Object) new String[0]);
            return new Result(captured.toString("UTF-8"), null);
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            try {
                return new Result(captured.toString("UTF-8"), describe(cause));
            } catch (Exception e) {
                return new Result("", describe(cause));
            }
        } finally {
            System.setOut(originalOut);
            if (loader != null) {
                try {
                    loader.close();
                } catch (Exception ignore) {
                    // nothing useful to do
                }
            }
        }
    }

    private static Result runInInterpreter(Path dir, String className, String source)
            throws Exception {
        byte[] bundleBytes = buildBundle(dir, className, source);
        InterpBundle bundle = InterpBundleReader.read(new ByteArrayInputStream(bundleBytes));

        ReflectionInterpLinker linker = new ReflectionInterpLinker();
        ProxyInterpObjectFactory factory = new ProxyInterpObjectFactory(linker);
        InterpRuntime runtime = new InterpRuntime(bundle, linker, factory);
        factory.attach(runtime);
        // The conformance corpus includes deliberately long loops; the EDT
        // budget is a device concern and would only make the suite flaky here.
        runtime.setEdtBudgetMs(0);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(captured, true, "UTF-8"));
            runtime.runMain(new String[0]);
            return new Result(captured.toString("UTF-8"), null);
        } catch (InterpThrowable it) {
            Object thrown = it.getThrown();
            return new Result(captured.toString("UTF-8"),
                    thrown instanceof Throwable ? describe((Throwable) thrown) : String.valueOf(thrown));
        } catch (Throwable t) {
            return new Result(captured.toString("UTF-8"), describe(t));
        } finally {
            System.setOut(originalOut);
        }
    }

    /** Builds a bundle from every class file under {@code dir}. */
    static byte[] buildBundle(Path dir, String mainClass, String source) throws Exception {
        // Loaded reflectively so core-unittests does not need a compile-time
        // dependency on the translator module.
        Class<?> writerClass = Class.forName("com.codename1.tools.translator.InterpBundleWriter");
        Object writer = writerClass.getDeclaredConstructor().newInstance();
        Method addClassFile = writerClass.getMethod("addClassFile", File.class);
        Method addSource = writerClass.getMethod("addSource", String.class, String.class);
        Method setMain = writerClass.getMethod("setMainClass", String.class);
        Method write = writerClass.getMethod("write", java.io.OutputStream.class);

        List<File> classFiles = new ArrayList<File>();
        collectClassFiles(dir.toFile(), classFiles);
        for (File f : classFiles) {
            addClassFile.invoke(writer, f);
        }
        addSource.invoke(writer, mainClass + ".java", source);
        setMain.invoke(writer, mainClass);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        write.invoke(writer, bos);
        return bos.toByteArray();
    }

    private static void collectClassFiles(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File f : kids) {
            if (f.isDirectory()) {
                collectClassFiles(f, out);
            } else if (f.getName().endsWith(".class")) {
                out.add(f);
            }
        }
    }

    /**
     * Renders a failure so the two runs can be compared. Only the type and
     * message are used: a JVM stack trace names the JVM's frames and an
     * interpreted one names the interpreter's, so including either would make
     * every failing case differ for the wrong reason.
     */
    private static String describe(Throwable t) {
        String msg = t.getMessage();
        return t.getClass().getName() + (msg == null ? "" : ": " + msg);
    }
}
