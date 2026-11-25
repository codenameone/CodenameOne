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
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runtime clazz synthesis, end to end: a class that exists only at runtime is
 * dispatched to, type-checked, and survives collection.
 *
 * <p>This is the load-bearing claim of the device-runtime design. An
 * interpreted {@code class MyForm extends Form} has to be an object the AOT
 * framework accepts as a {@code Form} and whose overrides the framework calls —
 * but ParparVM cannot generate classes, has no {@code defineClass}, and iOS
 * forbids writing executable memory. What it does have is a heap-allocated,
 * slot-indexed vtable per class, so a subclass can be built by copying the
 * parent's clazz and repointing the overridden slots.</p>
 *
 * <p>Everything here is exercised through the real translator and a real
 * compiled binary, because the properties under test — that the AOT caller's
 * dispatch reaches the trampoline, that {@code instanceof} still answers, that
 * the collector does not reclaim an object whose clazz it never saw at build
 * time — are all properties of generated code and the runtime, not of the
 * translator's text output.</p>
 *
 * <p>Runs the {@code clean} target rather than the iOS one: it emits the same C
 * against the same {@code cn1_globals} runtime, so the object model is
 * identical, and it runs on the host in seconds.</p>
 */
@EnabledOnOs({OS.MAC, OS.LINUX})
@EnabledIf("com.codename1.tools.translator.InterpHostVtableSynthesisIntegrationTest#hasToolchain")
class InterpHostVtableSynthesisIntegrationTest {

    static boolean hasToolchain() {
        return !CompilerHelper.getAvailableCompilers("1.8").isEmpty();
    }

    /**
     * {@code Base.greet()} is the method the synthetic subclass overrides.
     * {@code size()} is a second virtual method so the overridden one is not
     * trivially at slot 0, and {@code tag} is an inherited field the synthetic
     * object must still be able to hold at the offset AOT code expects.
     */
    private static final String APP_TEMPLATE =
            "public class Main {\n"
          + "    public static class Base {\n"
          + "        public int tag = 7;\n"
          + "        public int size() { return 1; }\n"
          + "        public String greet() { return \"base\"; }\n"
          + "        public String describe() { return greet() + \"/\" + size(); }\n"
          + "    }\n"
          + "    static native void installInterpSubclass(int slot, int slotCount);\n"
          + "    static native Object newInterpObject();\n"
          + "    static native int scanSlotOfBaseGreet(int slotCount);\n"
          + "\n"
          + "    public static void main(String[] args) {\n"
          + "        int slot = %SLOT%;\n"
          + "        int slotCount = %SLOTCOUNT%;\n"
          + "        System.out.println(\"SCAN:\" + scanSlotOfBaseGreet(slotCount));\n"
          + "        installInterpSubclass(slot, slotCount);\n"
          + "        Object raw = newInterpObject();\n"
          + "        System.out.println(\"INSTANCEOF:\" + (raw instanceof Base));\n"
          + "        Base b = (Base) raw;\n"
          + "        System.out.println(\"DISPATCH:\" + b.greet());\n"
          + "        System.out.println(\"INHERITED:\" + b.size());\n"
          + "        System.out.println(\"INTERNAL:\" + b.describe());\n"
          + "        Base plain = new Base();\n"
          + "        System.out.println(\"UNAFFECTED:\" + plain.greet());\n"
          + "        b.tag = 42;\n"
          + "        StringBuilder sink = new StringBuilder();\n"
          + "        for (int i = 0; i < 400000; i++) { sink.append('x'); if (sink.length() > 64) sink.setLength(0); }\n"
          + "        System.out.println(\"AFTERGC:\" + b.greet() + \":\" + b.tag);\n"
          + "        System.out.println(\"DONE\");\n"
          + "    }\n"
          + "}\n";

    /**
     * The whole mechanism in one run. Asserted as a group because these are not
     * independent behaviours — they are the several ways a single synthetic
     * clazz has to behave like a real one, and a failure in any of them means
     * the same thing: the object model does not accept runtime subclasses.
     */
    @Test
    void aRuntimeSynthesizedSubclassBehavesLikeACompiledOne() throws Exception {
        Symbols symbols = translateProbeAndReadSymbols();

        int slotCount = symbols.vtableSizeOf("Main_Base");
        int slot = symbols.slotOf("Main_Base", "greet");
        assertTrue(slotCount > 0, "Main$Base should publish a vtable size");
        assertTrue(slot >= 0, "Main$Base.greet should publish a vtable slot");

        String output = buildAndRun(slot, slotCount);

        // The symbol table's claim about the slot, checked against the address
        // actually sitting in the built binary's vtable. If these disagree the
        // rows are worse than useless -- patching would redirect some other
        // method, silently.
        assertTrue(output.contains("SCAN:" + slot),
                "the symbol table says greet is at slot " + slot
                        + " but the binary's vtable disagrees, output:\n" + output);

        assertTrue(output.contains("INSTANCEOF:true"),
                "a synthetic subclass must still satisfy `instanceof Base`, output:\n" + output);
        assertTrue(output.contains("DISPATCH:interpreted"),
                "an AOT caller holding a Base reference must reach the trampoline, output:\n" + output);
        assertTrue(output.contains("INHERITED:1"),
                "unpatched slots must still reach the parent implementation, output:\n" + output);
        // describe() is AOT code inside the parent calling greet() on itself --
        // the override has to win there too, or a framework method that calls an
        // overridable method would silently get the base behaviour.
        assertTrue(output.contains("INTERNAL:interpreted/1"),
                "a self-call from AOT parent code must reach the override, output:\n" + output);
        assertTrue(output.contains("UNAFFECTED:base"),
                "patching the synthetic vtable must not disturb the parent class, output:\n" + output);
        assertTrue(output.contains("AFTERGC:interpreted:42"),
                "the synthetic clazz and its instance must survive collection, output:\n" + output);
        assertTrue(output.contains("DONE"),
                "the program should run to completion, output:\n" + output);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * First pass: translate the app with placeholder constants purely to obtain
     * the symbol table, which is where the vtable layout comes from. The
     * constants cannot be known before this, since the layout is decided by the
     * translator.
     */
    private Symbols translateProbeAndReadSymbols() throws Exception {
        Path out = translate(render(0, 0), Files.createTempDirectory("interp-vt-probe"), false);
        return Symbols.parse(readSymbolTable(out));
    }

    private String render(int slot, int slotCount) {
        return APP_TEMPLATE
                .replace("%SLOT%", Integer.toString(slot))
                .replace("%SLOTCOUNT%", Integer.toString(slotCount));
    }

    /**
     * Second pass: translate with the real constants, drop the interp runtime
     * into the generated source root, then build and run.
     *
     * <p>The C file is copied in after translation rather than before. The
     * translator's readNativeFiles pass only matters for dependency marking,
     * and an interp-host build has dead code elimination disabled — so there is
     * nothing to mark. CMake globs {@code *.c} out of the source root at
     * configure time, which happens later still.</p>
     */
    private String buildAndRun(int slot, int slotCount) throws Exception {
        Path outputDir = Files.createTempDirectory("interp-vt-run");
        translate(render(slot, slotCount), outputDir, true);

        Path srcRoot = Files.walk(outputDir)
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().endsWith("-src"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no -src root under " + outputDir));
        Files.copy(spikeSource(), srcRoot.resolve("cn1_interp_spike.c"),
                StandardCopyOption.REPLACE_EXISTING);

        Path distDir = outputDir.resolve("dist");
        CleanTargetIntegrationTest.replaceLibraryWithExecutableTarget(
                distDir.resolve("CMakeLists.txt"), srcRoot.getFileName().toString());

        Path buildDir = distDir.resolve("build");
        Files.createDirectories(buildDir);
        List<String> configure = new ArrayList<>(Arrays.asList(
                "cmake", "-S", distDir.toString(), "-B", buildDir.toString()));
        configure.addAll(CompilerHelper.cmakeToolchainArgs());
        CleanTargetIntegrationTest.runCommand(configure, distDir);
        CleanTargetIntegrationTest.runCommand(
                Arrays.asList("cmake", "--build", buildDir.toString()), distDir);

        Path exe = buildDir.resolve(CompilerHelper.executableName("InterpVtApp"));
        return CleanTargetIntegrationTest.runCommand(Arrays.asList(exe.toString()), buildDir);
    }

    private static Path spikeSource() {
        Path p = Paths.get("src", "test", "resources", "interp", "cn1_interp_spike.c")
                .toAbsolutePath();
        assertTrue(Files.exists(p), "spike runtime missing at " + p);
        return p;
    }

    /** Compiles the fixture and runs the translator over it. Returns outputDir. */
    private Path translate(String source, Path outputDir, boolean keepSources) throws Exception {
        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);

        Path sourceDir = Files.createTempDirectory("interp-vt-src");
        Path classesDir = Files.createTempDirectory("interp-vt-classes");
        Files.write(sourceDir.resolve("Main.java"), source.getBytes(StandardCharsets.UTF_8));

        Path javaApiDir = Files.createTempDirectory("interp-vt-api");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> args = new ArrayList<>(Arrays.asList(
                "-source", config.targetVersion, "-target", config.targetVersion));
        args.add(CompilerHelper.useClasspath(config) ? "-classpath" : "-bootclasspath");
        args.add(javaApiDir.toString());
        args.addAll(Arrays.asList("-d", classesDir.toString(),
                sourceDir.resolve("Main.java").toString()));
        if (CompilerHelper.compile(config.jdkHome, args) != 0) {
            throw new IllegalStateException("fixture did not compile: "
                    + CompilerHelper.getLastErrorLog());
        }
        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        String previousInterp = System.getProperty("cn1.interpHost");
        System.setProperty("cn1.interpHost", "true");
        // The spike's three natives are implemented in cn1_interp_spike.c, which
        // is copied into the generated source root *after* translation -- the
        // root does not exist until then, and cmake globs the directory at
        // configure time. So the native-signature check has nothing to find and
        // would abort a translation whose natives are in fact implemented. CI
        // sets CN1_NATIVE_VERIFY=strict for every forked translation, and the
        // property wins over the environment, which is what makes this local
        // opt-out possible.
        String previousVerify = System.getProperty(
                NativeSignatureVerifier.MODE_PROPERTY);
        System.setProperty(NativeSignatureVerifier.MODE_PROPERTY, "off");
        try {
            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "InterpVtApp");
        } finally {
            if (previousInterp == null) {
                System.clearProperty("cn1.interpHost");
            } else {
                System.setProperty("cn1.interpHost", previousInterp);
            }
            if (previousVerify == null) {
                System.clearProperty(NativeSignatureVerifier.MODE_PROPERTY);
            } else {
                System.setProperty(NativeSignatureVerifier.MODE_PROPERTY, previousVerify);
            }
        }
        return outputDir;
    }

    private static String readSymbolTable(Path outputDir) throws Exception {
        Path symbols = Files.walk(outputDir)
                .filter(p -> p.getFileName().toString().equals("cn1_debug_symbols.c"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no symbol blob under " + outputDir));

        String c = new String(Files.readAllBytes(symbols), StandardCharsets.UTF_8);
        int start = c.indexOf('{', c.indexOf("cn1_debug_symbols_gz[]"));
        int end = c.indexOf("};", start);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        Matcher m = Pattern.compile("0x([0-9a-fA-F]{2})").matcher(c.substring(start, end));
        while (m.find()) {
            raw.write(Integer.parseInt(m.group(1), 16));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw.toByteArray()))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gz.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /** The vtable-layout view of the symbol table. */
    private static final class Symbols {
        private final Map<String, Integer> classIds = new HashMap<>();
        private final Map<Integer, Integer> vtableSizes = new HashMap<>();
        private final Map<String, Integer> slotByClassAndMethodId = new HashMap<>();
        private final Map<Integer, String> methodNames = new HashMap<>();

        static Symbols parse(String table) {
            Symbols s = new Symbols();
            for (String line : table.split("\n")) {
                String[] p = line.split("\t", -1);
                switch (p[0]) {
                    case "class":
                        if (p.length >= 3) s.classIds.put(p[2], Integer.parseInt(p[1]));
                        break;
                    case "vtsize":
                        if (p.length >= 3) {
                            s.vtableSizes.put(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                        }
                        break;
                    case "vtable":
                        if (p.length >= 4) {
                            s.slotByClassAndMethodId.put(p[1] + ":" + p[3], Integer.parseInt(p[2]));
                        }
                        break;
                    case "method":
                        if (p.length >= 4) s.methodNames.put(Integer.parseInt(p[1]), p[3]);
                        break;
                    default:
                        break;
                }
            }
            return s;
        }

        int vtableSizeOf(String mangledClass) {
            Integer id = classIds.get(mangledClass);
            assertNotNull(id, "no class row for " + mangledClass);
            Integer size = vtableSizes.get(id);
            assertNotNull(size, "no vtsize row for " + mangledClass);
            return size;
        }

        int slotOf(String mangledClass, String methodName) {
            Integer id = classIds.get(mangledClass);
            assertNotNull(id, "no class row for " + mangledClass);
            for (Map.Entry<String, Integer> e : slotByClassAndMethodId.entrySet()) {
                String[] parts = e.getKey().split(":");
                if (!parts[0].equals(String.valueOf(id))) {
                    continue;
                }
                if (methodName.equals(methodNames.get(Integer.parseInt(parts[1])))) {
                    return e.getValue();
                }
            }
            return -1;
        }
    }
}
