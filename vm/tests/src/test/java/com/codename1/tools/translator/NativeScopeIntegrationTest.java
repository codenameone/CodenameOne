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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// What the natives name, and when that keeps it -- NativeBodies, NativeFeatureFilter,
/// the conditional static initializers and the owner-aware static calls of
/// ReachabilityCull, which between them took the feature packages a Flutter gallery never
/// uses (home, call, nearby, health...) out of its iOS binary.
///
/// Each rule is checked in both directions: what must stay is built with the cull trap on
/// and RUN, so a wrong cull aborts; what must go is read from the generated C as the culled
/// stub. A second translation with the native scoping switched off shows the stub
/// assertions are about these rules and nothing else.
class NativeScopeIntegrationTest {

    private static final String APP = "NativeScopeApp";

    @Test
    void theNativesKeepOnlyWhatCanRun() throws Exception {
        Parser.cleanup();
        Path sourceDir = Files.createTempDirectory("native-scope-sources");
        Path classesDir = Files.createTempDirectory("native-scope-classes");
        Path javaApiDir = Files.createTempDirectory("native-scope-java-api");
        Path source = sourceDir.resolve(APP + ".java");
        Files.write(source, load(APP + ".java").getBytes(StandardCharsets.UTF_8));

        CompilerHelper.CompilerConfig config = selectCompiler();
        CompilerHelper.compileJavaAPI(javaApiDir, config);
        List<String> args = new ArrayList<>(Arrays.asList("-source", config.targetVersion, "-target", config.targetVersion));
        if (CompilerHelper.useClasspath(config)) {
            args.addAll(Arrays.asList("-classpath", javaApiDir.toString()));
        } else {
            args.addAll(Arrays.asList("-bootclasspath", javaApiDir.toString(), "-Xlint:-options"));
        }
        args.addAll(Arrays.asList("-d", classesDir.toString(), source.toString()));
        assertTrue(CompilerHelper.compile(config.jdkHome, args) == 0,
                APP + " should compile: " + CompilerHelper.getLastErrorLog());
        CompilerHelper.copyDirectory(javaApiDir, classesDir);
        Files.write(classesDir.resolve("NativeScopeNative.c"), load("NativeScopeNative.c").getBytes(StandardCharsets.UTF_8));

        Path out = Files.createTempDirectory("native-scope-output");
        translate(classesDir, out, null);
        Path src = out.resolve("dist").resolve(APP + "-src");

        // Culled: named only by a native nothing calls, by a switched-off feature, by a
        // macro only that feature defines; an initializer nothing triggers; a static of the
        // right name in the wrong class.
        assertStub(src, "NativeScopeB.twin(I)I", "has the name and descriptor of the static main calls, on class A");
        assertStub(src, "NativeScopeDead.onEvent(I)I", "is named only by the body of a native Java never calls");
        assertStub(src, "NativeScopeSwitched.cb(I)I", "is named only inside #ifdef CN1_INCLUDE_SCOPETEST, which is off");
        assertStub(src, "NativeScopeDerived.cb(I)I", "is named only under CN1_SCOPETEST_HAS_EXTRA, defined only when the off switch is on");
        assertStub(src, "NativeScopeNeverInit.__CLINIT__()V", "belongs to a class nothing initializes");

        // Kept: reached through a live native's body, a static helper that body calls, a
        // function with external linkage, a touched class's initializer.
        assertKept(src, "NativeScopeFeature.onEvent(I)I");
        assertKept(src, "NativeScopeHelperTarget.viaHelper(I)I");
        assertKept(src, "NativeScopeRooted.fromFreeFunction(I)I");
        assertKept(src, "NativeScopeTouched.__CLINIT__()V");
        assertKept(src, "NativeScopeA.twin(I)I");

        Path dist = out.resolve("dist");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(dist.resolve("CMakeLists.txt"), APP + "-src");
        Path build = dist.resolve("build");
        Files.createDirectories(build);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "-S", dist.toString(), "-B", build.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), dist);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", build.toString()), dist);
        String run = CleanTargetIntegrationTest.runCommand(Arrays.asList(build.resolve(APP).toString()), build);
        assertFalse(run.contains("CN1 FATAL"), "No culled method may run: " + run);
        assertTrue(run.contains("CASE|feature|115"), "featureStart must reach both callbacks: " + run);
        assertTrue(run.contains("CASE|touched|42"), "A touched class must run its initializer: " + run);
        assertTrue(run.contains("CASE|twin|11"), run);
        assertTrue(run.contains("DONE"), run);

        // With the native scoping off, every name in the natives is a root again.
        Path plain = Files.createTempDirectory("native-scope-plain");
        translate(classesDir, plain, "false");
        Path plainSrc = plain.resolve("dist").resolve(APP + "-src");
        assertKept(plainSrc, "NativeScopeDead.onEvent(I)I");
    }

    private static void assertStub(Path src, String method, String why) throws Exception {
        assertTrue(read(fileOf(src, method)).contains(stub(method)), method + " must be culled: it " + why);
    }

    private static void assertKept(Path src, String method) throws Exception {
        assertFalse(read(fileOf(src, method)).contains(stub(method)), method + " must keep its body");
    }

    private static Path fileOf(Path src, String method) {
        return src.resolve(method.substring(0, method.indexOf('.')) + ".c");
    }

    private static void translate(Path classes, Path out, String bodyScope) throws Exception {
        String oldTrap = System.getProperty("cn1.cull.trap");
        String oldScope = System.getProperty("cn1.nativeBodyScope");
        System.setProperty("cn1.cull.trap", "true");
        if (bodyScope == null) {
            System.clearProperty("cn1.nativeBodyScope");
        } else {
            System.setProperty("cn1.nativeBodyScope", bodyScope);
        }
        try {
            Parser.cleanup();
            CleanTargetIntegrationTest.runTranslator(classes, out, APP);
        } finally {
            restore("cn1.cull.trap", oldTrap);
            restore("cn1.nativeBodyScope", oldScope);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static String stub(String method) {
        return "cn1CulledMethodCalled(\"" + method + "\")";
    }

    private static String read(Path p) throws Exception {
        assertTrue(Files.exists(p), p + " should have been generated");
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private String load(String name) throws Exception {
        java.io.InputStream in = NativeScopeIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/" + name);
        assertNotNull(in, name + " test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private CompilerHelper.CompilerConfig selectCompiler() {
        for (String target : new String[] {"11", "17", "21", "25", "1.8"}) {
            for (CompilerHelper.CompilerConfig config : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        fail("No compatible compiler available for the native scope test");
        return null;
    }
}
