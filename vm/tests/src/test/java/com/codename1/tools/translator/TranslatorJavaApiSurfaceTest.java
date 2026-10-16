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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The translator is also a ParparVM program: the Playground runs it in the browser,
/// translated by the JavaScript target into its open-world host bundle, and
/// vm/selfhost runs it natively. Either way it links against ParparVM's class
/// library (vm/JavaAPI), not the JDK it is compiled with -- so a JDK method JavaAPI
/// does not declare compiles here, passes every JVM-hosted test, and fails only when
/// the translated translator reaches the call.
///
/// That shipped once. A merge brought a `String.replaceAll` into
/// JavascriptMethodGenerator's peephole rules (JavaAPI's String has no regex
/// methods; the translator's own regex engine is what the neighbouring lines call
/// through `rx`), and every Playground snippet then failed in the browser with
/// "Missing virtual method cn1_s_replaceAll_java_lang_String_java_lang_String_R_java_lang_String
/// on java_lang_String". The self-host workflow that compiles the translator against
/// JavaAPI runs on PRs only behind a label, so nothing in default CI saw it.
///
/// This compiles the translator's sources against JavaAPI alone, with exactly the
/// source set vm/selfhost/build-selfhost.sh uses (its stubs replace the few classes
/// that cannot compile there), and fails on any error -- naming the call.
class TranslatorJavaApiSurfaceTest {

    @Test
    void translatorSourcesCompileAgainstJavaApiAlone() throws Exception {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "a JDK (not a JRE) is required to run this test");

        Path vm = Paths.get("..").toAbsolutePath().normalize();
        Path javaApiSrc = vm.resolve("JavaAPI/src");
        Path translatorSrc = vm.resolve("ByteCodeTranslator/src");
        Path stubs = vm.resolve("selfhost/stubs");
        assertTrue(Files.isDirectory(javaApiSrc), "missing " + javaApiSrc);
        assertTrue(Files.isDirectory(translatorSrc), "missing " + translatorSrc);
        assertTrue(Files.isDirectory(stubs), "missing " + stubs);

        // 1. JavaAPI, as build-selfhost.sh builds it: its sources over a Java 8 platform
        //    (they lean on it for a few types they do not declare, which the
        //    translator never reaches). Only its output is the next step's platform.
        Path work = Files.createTempDirectory("translator-javaapi-surface");
        Path javaApiClasses = Files.createDirectories(work.resolve("javaapi"));
        List<String> apiErrors = compile(javac, javaFiles(javaApiSrc), null, javaApiClasses);
        assertTrue(apiErrors.isEmpty(), "JavaAPI itself failed to compile:\n" + String.join("\n", apiErrors));

        // 2. The self-host source set (build-selfhost.sh step 4): every translator
        //    source except the ones a stub replaces and the two JVM-only CLIs, plus
        //    the stubs.
        Set<String> stubbed = new HashSet<String>();
        List<Path> stubSources = javaFiles(stubs);
        for (Path p : stubSources) {
            stubbed.add(p.getFileName().toString());
        }
        assertTrue(!stubbed.isEmpty(), "no stubs found under " + stubs);
        Set<String> jvmOnly = new HashSet<String>(Arrays.asList(
                "CastSemanticsVerifier.java", "NativeSignatureVerifierCli.java"));
        List<Path> sources = new ArrayList<Path>();
        for (Path p : javaFiles(translatorSrc)) {
            String name = p.getFileName().toString();
            if (!stubbed.contains(name) && !jvmOnly.contains(name)) {
                sources.add(p);
            }
        }
        sources.addAll(stubSources);

        Path translatorClasses = Files.createDirectories(work.resolve("translator"));
        List<String> errors = compile(javac, sources, javaApiClasses, translatorClasses);
        assertTrue(errors.isEmpty(), "The translator calls API that ParparVM's class library (vm/JavaAPI) does not"
                + " declare. It compiles against the JDK, but the translated translator (the Playground's"
                + " in-browser translator, vm/selfhost) fails at the call with \"Missing virtual method\"."
                + " Use what JavaAPI provides -- for regular expressions, the translator's own engine"
                + " (com.codename1.tools.translator.regex, e.g. JavascriptMethodGenerator.rx):\n"
                + String.join("\n", errors));
    }

    private static List<Path> javaFiles(Path root) throws Exception {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
    }

    private static List<String> compile(JavaCompiler javac, List<Path> sources, Path bootClassPath, Path out)
            throws Exception {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            List<File> files = new ArrayList<File>();
            for (Path p : sources) {
                files.add(p.toFile());
            }
            // No boot class path means the JDK's own Java 8 platform; with one, that
            // directory is the whole platform. The Java 8 platform is spelled two ways:
            // --release 8 on JDK 9 and later (plain -source 8 would see the module
            // system's java.base), and the running platform itself on JDK 8, whose
            // javac does not know --release -- vm-tests runs this module on JDK 8.
            boolean jdk8 = System.getProperty("java.specification.version", "").startsWith("1.");
            List<String> options = new ArrayList<String>(bootClassPath != null
                    ? Arrays.asList("-source", "1.8", "-target", "1.8", "-bootclasspath", bootClassPath.toString())
                    : jdk8 ? Arrays.asList("-source", "1.8", "-target", "1.8")
                    : Arrays.asList("--release", "8"));
            options.addAll(Arrays.asList("-encoding", "UTF-8", "-proc:none", "-nowarn", "-Xlint:-options",
                    "-Xmaxerrs", "100000", "-d", out.toString()));
            javac.getTask(null, fm, diagnostics, options, null, fm.getJavaFileObjectsFromFiles(files)).call();
        }
        List<String> errors = new ArrayList<String>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                String where = d.getSource() == null ? "" : d.getSource().getName() + ":" + d.getLineNumber() + ": ";
                errors.add(where + d.getMessage(null));
            }
        }
        return errors;
    }
}
