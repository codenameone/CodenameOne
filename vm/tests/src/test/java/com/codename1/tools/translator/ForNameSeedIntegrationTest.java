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
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// The reflection seed of ReachabilityCull, one Class.forName site at a time.
///
/// Class.forName is the only way a program gets a Class object for a class it never
/// allocated, and each site the translator knows about keeps only the concrete classes
/// assignable to the one type its result is used as (FOR_NAME_SITES). For every such site
/// this translates a program where that site is the ONLY live forName and a listener is
/// created solely by name, builds it with the cull trap on and runs it: the listener must
/// run. It then checks the other seed classes' methods are culled stubs -- otherwise the
/// site would not be narrowed at all, and the test would pass on the old, wide seed.
///
/// The sites are stand-ins with the real class and method names (forname/ForNameSites.txt),
/// since the real ones sit in the core and the iOS port. The last test translates the real
/// core and port and checks every forName they make reachable IS narrowed, so a renamed
/// method or a new forName call fails here rather than silently widening the seed again.
class ForNameSeedIntegrationTest {

    private static final String[] SEEDS = {
        "SeedGeofence.onEntered(Ljava/lang/String;)V",
        "SeedLocation.providerStateChanged(I)V",
        "SeedWorker.performWork(Ljava/lang/String;)V",
        "SeedTest.shouldExecuteOnEDT()Z",
        "SeedNativeImpl.isSupported()Z",
        "SeedOther.probe()V",
    };

    @Test
    void eachNarrowedSiteKeepsItsListenerAndNothingElse() throws Exception {
        Fixture f = compileFixture();
        check(f, "SiteGeofenceManager", "SeedGeofence", "CASE|geofence|x");
        check(f, "SiteIosGeofence", "SeedGeofence", "CASE|geofence|x");
        check(f, "SiteBackgroundLocation", "SeedLocation", "CASE|location|1");
        check(f, "SiteBackgroundWorker", "SeedWorker", "CASE|worker|x");
        check(f, "SiteDeviceRunner", "SeedTest", "CASE|test|true");
        check(f, "SiteNativeLookup", "SeedNativeImpl", "CASE|native|true");
    }

    @Test
    void anUnlistedSiteFallsBackToEveryNoArgClass() throws Exception {
        Fixture f = compileFixture();
        String log = translate(f, "SiteUnlisted");
        assertTrue(log.contains("Class.forName in [SiteUnlisted.main] is not narrowed"),
                "An unknown forName caller must be reported. Translator output:\n" + log);
        Path src = f.out.resolve("SiteUnlisted").resolve("dist").resolve("SiteUnlisted-src");
        for (String seed : SEEDS) {
            assertFalse(read(src, seed).contains(stub(seed)),
                    seed + " must be kept when a forName site is not narrowed");
        }
        assertTrue(buildAndRun(f, "SiteUnlisted").contains("CASE|other|probed"));
    }

    /// The real core and iOS port: every forName they make reachable must be in the table.
    @Test
    void theRealForNameSitesAreAllNarrowed() throws Exception {
        Path core = Paths.get("..", "..", "maven", "core", "target", "classes").normalize().toAbsolutePath();
        Path ios = Paths.get("..", "..", "maven", "ios", "target", "classes").normalize().toAbsolutePath();
        org.junit.jupiter.api.Assumptions.assumeTrue(Files.exists(core.resolve("com/codename1/ui/Form.class")),
                "codenameone-core must be built (maven/core/target/classes)");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                Files.exists(ios.resolve("com/codename1/impl/ios/IOSImplementation.class")),
                "the iOS port must be built (maven/ios/target/classes)");
        CompilerHelper.CompilerConfig config = selectCompiler();
        Parser.cleanup();
        Path javaApi = Files.createTempDirectory("forname-core-japi");
        CompilerHelper.compileJavaAPI(javaApi, config);
        Path srcDir = Files.createTempDirectory("forname-core-src");
        Path classes = Files.createTempDirectory("forname-core-classes");
        // Reaches every forName in the port and the core the iOS location, geofence,
        // background and native-interface paths lead to.
        String app = "package com.codename1.impl.ios;\n"
                + "public class RealSites {\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        IOSImplementation impl = new IOSImplementation();\n"
                + "        IOSImplementation.Loc loc = impl.new Loc();\n"
                + "        loc.getBackgroundLocationListener();\n"
                + "        loc.getGeofenceListener(\"x\");\n"
                + "        IOSImplementation.runBackgroundProcessing(\"x\");\n"
                + "        com.codename1.location.GeofenceManager.getInstance().getListenerClass();\n"
                + "        com.codename1.system.NativeLookup.create(com.codename1.system.NativeInterface.class);\n"
                + "    }\n"
                + "}\n";
        Path appDir = srcDir.resolve("com/codename1/impl/ios");
        Files.createDirectories(appDir);
        Files.write(appDir.resolve("RealSites.java"), app.getBytes(StandardCharsets.UTF_8));
        assertEquals(0, CompilerHelper.compile(config.jdkHome, Arrays.asList(
                "-source", config.targetVersion, "-target", config.targetVersion,
                "-classpath", core + java.io.File.pathSeparator + ios,
                "-d", classes.toString(), appDir.resolve("RealSites.java").toString())),
                "RealSites should compile against core + iOS port:\n" + CompilerHelper.getLastErrorLog());
        Path out = Files.createTempDirectory("forname-core-out");
        String sources = classes + ";" + core + ";" + ios + ";" + javaApi;
        String log = captureTranslation(() -> CleanTargetIntegrationTest.runTranslatorMultiSource(
                sources, out, "RealSites", "ios"));
        assertFalse(log.contains("is not narrowed"),
                "A Class.forName in the core or iOS port is missing from ReachabilityCull.FOR_NAME_SITES:\n"
                        + Arrays.stream(log.split("\n")).filter(l -> l.contains("is not narrowed"))
                                .collect(Collectors.joining("\n")));
        // Not vacuous: every site really is live in this program.
        Path gen = out.resolve("dist").resolve("RealSites-src");
        for (String site : new String[] {"com_codename1_impl_ios_IOSImplementation_Loc",
                "com_codename1_impl_ios_IOSImplementation", "com_codename1_location_GeofenceManager",
                "com_codename1_system_NativeLookup"}) {
            Path p = gen.resolve(site + ".c");
            assertTrue(Files.exists(p), p + " should have been generated");
            assertTrue(new String(Files.readAllBytes(p), StandardCharsets.UTF_8)
                            .contains("java_lang_Class_forName___java_lang_String_R_java_lang_Class("),
                    site + " should still call Class.forName in this program");
        }
    }

    private void check(Fixture f, String site, String kept, String expectedLine) throws Exception {
        String log = translate(f, site);
        assertFalse(log.contains("is not narrowed"), site + " must be narrowed. Translator output:\n" + log);
        Path src = f.out.resolve(site).resolve("dist").resolve(site + "-src");
        for (String seed : SEEDS) {
            boolean stubbed = read(src, seed).contains(stub(seed));
            if (seed.startsWith(kept + ".")) {
                assertFalse(stubbed, site + ": " + seed + " is created by name there and must be kept");
            } else {
                assertTrue(stubbed, site + ": " + seed + " is not assignable to what " + site
                        + " creates, so the narrowed seed must leave it culled");
            }
        }
        String run = buildAndRun(f, site);
        assertTrue(run.contains(expectedLine), site + ": the listener created by name must run. Output:\n" + run);
        assertFalse(run.contains("CN1 FATAL"), site + ": no culled method may run. Output:\n" + run);
    }

    private static final class Fixture {
        Path classes;
        Path out;
        CompilerHelper.CompilerConfig config;
    }

    private Fixture compileFixture() throws Exception {
        Fixture f = new Fixture();
        f.config = selectCompiler();
        Path src = Files.createTempDirectory("forname-src");
        f.classes = Files.createTempDirectory("forname-classes");
        f.out = Files.createTempDirectory("forname-out");
        Path javaApi = Files.createTempDirectory("forname-japi");
        CompilerHelper.compileJavaAPI(javaApi, f.config);

        List<String> files = new ArrayList<>();
        String current = null;
        StringBuilder body = new StringBuilder();
        for (String line : loadResource("forname/ForNameSites.txt").split("\n", -1)) {
            if (line.startsWith("//// FILE ")) {
                if (current != null) {
                    files.add(write(src, current, body.toString()));
                }
                current = line.substring("//// FILE ".length()).trim();
                body.setLength(0);
            } else if (current != null) {
                body.append(line).append('\n');
            }
        }
        files.add(write(src, current, body.toString()));

        List<String> args = new ArrayList<>(Arrays.asList(
                "-source", f.config.targetVersion, "-target", f.config.targetVersion));
        if (CompilerHelper.useClasspath(f.config)) {
            args.addAll(Arrays.asList("-classpath", javaApi.toString()));
        } else {
            args.addAll(Arrays.asList("-bootclasspath", javaApi.toString(), "-Xlint:-options"));
        }
        args.addAll(Arrays.asList("-d", f.classes.toString()));
        args.addAll(files);
        assertEquals(0, CompilerHelper.compile(f.config.jdkHome, args),
                "The forName fixture should compile:\n" + CompilerHelper.getLastErrorLog());
        CompilerHelper.copyDirectory(javaApi, f.classes);
        return f;
    }

    private static String write(Path root, String rel, String content) throws Exception {
        Path p = root.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        return p.toString();
    }

    private String translate(Fixture f, String main) throws Exception {
        Path out = f.out.resolve(main);
        Files.createDirectories(out);
        // The translator takes one main class, so each site gets the fixture without the
        // other sites' mains.
        Path classes = Files.createTempDirectory("forname-" + main);
        CompilerHelper.copyDirectory(f.classes, classes);
        try (java.util.stream.Stream<Path> s = Files.list(classes)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String n = p.getFileName().toString();
                if (n.startsWith("Site") && n.endsWith(".class") && !n.equals(main + ".class")) {
                    Files.delete(p);
                }
            }
        }
        return captureTranslation(() -> {
            Parser.cleanup();
            CleanTargetIntegrationTest.runTranslator(classes, out, main);
        });
    }

    private String buildAndRun(Fixture f, String main) throws Exception {
        Path dist = f.out.resolve(main).resolve("dist");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(dist.resolve("CMakeLists.txt"), main + "-src");
        Path build = dist.resolve("build");
        Files.createDirectories(build);
        CleanTargetIntegrationTest.runCommand(Arrays.asList(
                "cmake", "-S", dist.toString(), "-B", build.toString(),
                "-DCMAKE_C_COMPILER=clang", "-DCMAKE_OBJC_COMPILER=clang"), dist);
        CleanTargetIntegrationTest.runCommand(Arrays.asList("cmake", "--build", build.toString()), dist);
        return CleanTargetIntegrationTest.runCommand(Arrays.asList(build.resolve(main).toString()), build);
    }

    private interface Translation {
        void run() throws Exception;
    }

    /// Runs a translation with the cull trap on and returns what it printed; the
    /// translator reports an unnarrowed forName site on standard output.
    private static String captureTranslation(Translation t) throws Exception {
        String oldTrap = System.getProperty("cn1.cull.trap");
        String oldVerify = System.getProperty(NativeSignatureVerifier.MODE_PROPERTY);
        System.setProperty("cn1.cull.trap", "true");
        // The core and port are translated without their Objective-C, which only a device
        // build supplies; the natives are not what this test is about.
        System.setProperty(NativeSignatureVerifier.MODE_PROPERTY, "off");
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(new TeeStream(original, captured), true, "UTF-8"));
        try {
            t.run();
        } finally {
            System.setOut(original);
            restore("cn1.cull.trap", oldTrap);
            restore(NativeSignatureVerifier.MODE_PROPERTY, oldVerify);
        }
        return new String(captured.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class TeeStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int c) throws java.io.IOException {
            a.write(c);
            b.write(c);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws java.io.IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
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

    /// The generated C of the class that declares {@code method}.
    private static String read(Path src, String method) throws Exception {
        Path p = src.resolve(method.substring(0, method.indexOf('.')) + ".c");
        assertTrue(Files.exists(p), p + " should have been generated");
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    private String loadResource(String name) throws Exception {
        java.io.InputStream in = ForNameSeedIntegrationTest.class
                .getResourceAsStream("/com/codename1/tools/translator/" + name);
        assertNotNull(in, name + " test resource should exist");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n")) + "\n";
        }
    }

    private CompilerHelper.CompilerConfig selectCompiler() {
        String[] preferredTargets = {"11", "17", "21", "25", "1.8"};
        for (String target : preferredTargets) {
            for (CompilerHelper.CompilerConfig config : CompilerHelper.getAvailableCompilers(target)) {
                if (CompilerHelper.isJavaApiCompatible(config)) {
                    return config;
                }
            }
        }
        fail("No compatible compiler available for the forName seed test");
        return null;
    }
}
