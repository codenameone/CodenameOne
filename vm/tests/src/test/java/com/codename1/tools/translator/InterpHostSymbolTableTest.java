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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An interpreted class that extends a framework class has to get its overrides
 * called by AOT code that was compiled long before that class existed. The
 * mechanism is runtime clazz synthesis: copy the parent's vtable, then patch the
 * slots the interpreted subclass overrides.
 *
 * <p>That is only possible if the device knows the vtable layout, which nothing
 * previously published — the symbol table described classes, methods, fields and
 * lines, but not slots. These pin the layout rows an interp-host build emits, and
 * pin that a plain debug build still does not pay for them.</p>
 *
 * <p>Runs the real translator end to end rather than a hand-built fixture,
 * because the property under test is about the relationship between the emitted
 * vtable initialisers and the emitted symbol table — two separate passes that a
 * fixture could keep consistent by accident.</p>
 */
@EnabledIf("com.codename1.tools.translator.InterpHostSymbolTableTest#hasCompiler")
class InterpHostSymbolTableTest {

    static boolean hasCompiler() {
        return !CompilerHelper.getAvailableCompilers("1.8").isEmpty();
    }

    private static final String APP =
            "public class Main {\n"
          + "    public static class Base {\n"
          + "        public void greet() { System.out.println(\"base\"); }\n"
          + "        public int size() { return 1; }\n"
          + "    }\n"
          + "    public static class Derived extends Base {\n"
          + "        public void greet() { System.out.println(\"derived\"); }\n"
          + "    }\n"
          + "    public static void main(String[] args) { new Derived().greet(); }\n"
          + "}\n";

    /**
     * Every class that can be subclassed publishes how many slots its vtable
     * has. The runtime mallocs a synthetic vtable of exactly this size and
     * memcpys the parent's into it; a missing or wrong count is a heap
     * overflow, not a missing feature.
     */
    @Test
    void anInterpHostBuildPublishesAVtableSizeForEveryConcreteClass() throws Exception {
        SymbolRows rows = translate(true);

        assertFalse(rows.vtableSizes.isEmpty(), "no vtsize rows were emitted");
        Integer derived = rows.vtableSizes.get(rows.classIdOf("Main$Derived"));
        assertNotNull(derived, "Main$Derived should publish a vtable size");
        assertTrue(derived > 0, "a concrete class should have a non-empty vtable, was " + derived);
    }

    /**
     * A slot row says "this method occupies this slot of this class's vtable".
     * An override has to land on the slot its parent's declaration occupies, or
     * patching it would redirect an unrelated method.
     */
    @Test
    void anOverrideOccupiesTheSameSlotAsTheMethodItOverrides() throws Exception {
        SymbolRows rows = translate(true);

        int baseId = rows.classIdOf("Main$Base");
        int derivedId = rows.classIdOf("Main$Derived");

        Integer baseSlot = rows.slotOfMethodNamed(baseId, "greet");
        Integer derivedSlot = rows.slotOfMethodNamed(derivedId, "greet");

        assertNotNull(baseSlot, "Base.greet should have a vtable slot, rows:\n" + rows);
        assertNotNull(derivedSlot, "Derived.greet should have a vtable slot, rows:\n" + rows);
        assertTrue(baseSlot.equals(derivedSlot),
                "Derived.greet should override Base.greet's slot " + baseSlot
                        + " but took " + derivedSlot + ", rows:\n" + rows);
    }

    /**
     * Slot rows are emitted only for methods a class declares or overrides —
     * exactly what __INIT_VTABLE_&lt;cls&gt; patches. Listing inherited slots
     * too would be O(classes x hierarchy depth); the runtime walks up the
     * superclass chain instead, which it must be able to do regardless.
     */
    @Test
    void inheritedSlotsAreNotRepeatedUnderTheSubclass() throws Exception {
        SymbolRows rows = translate(true);

        int derivedId = rows.classIdOf("Main$Derived");
        // Derived overrides greet() but inherits size() unchanged.
        assertNotNull(rows.slotOfMethodNamed(derivedId, "greet"),
                "the overridden method should be listed under the subclass");
        assertTrue(rows.slotOfMethodNamed(derivedId, "size") == null,
                "the inherited method should not be repeated under the subclass, rows:\n" + rows);
    }

    /**
     * The layout rows exist for the device runtime host and nothing else. A
     * plain on-device-debug build links its symbol table into the binary, so
     * paying for rows jdb never reads would grow every debug build.
     */
    @Test
    void aPlainDebugBuildEmitsNoVtableRows() throws Exception {
        SymbolRows rows = translate(false);

        assertTrue(rows.vtableSizes.isEmpty(),
                "a debug build should emit no vtsize rows, got " + rows.vtableSizes.size());
        assertTrue(rows.slots.isEmpty(),
                "a debug build should emit no vtable rows, got " + rows.slots.size());
        assertFalse(rows.classIds.isEmpty(),
                "a debug build should still emit the rest of the symbol table");
    }

    /**
     * Pushed code may call any part of the API, including classes the host app
     * itself never mentions. An interp-host build therefore keeps every class
     * the translator saw, where a normal build culls the unreachable ones.
     */
    @Test
    void anInterpHostBuildRetainsClassesANormalBuildWouldCull() throws Exception {
        int keptWithInterpHost = translate(true).classIds.size();
        int keptNormally = translate(false).classIds.size();

        assertTrue(keptWithInterpHost > keptNormally,
                "interp-host should retain more classes than a culling build ("
                        + keptWithInterpHost + " vs " + keptNormally + ")");
    }

    /**
     * The thunks, field tables and vtable rows are all emitted behind {@code
     * #ifdef CN1_ON_DEVICE_DEBUG}, and the macro itself lives commented-out in
     * cn1_globals.h until the translator uncomments it. An interp-host build
     * enables on-device-debug emission implicitly rather than through the
     * cn1.onDeviceDebug property, so a rewrite keyed on that property leaves
     * every thunk compiling to nothing — a mechanism that is entirely absent at
     * runtime while looking present in the generated sources.
     */
    @Test
    void anInterpHostBuildUncommentsBothMacros() throws Exception {
        translate(true);
        String globals = readGlobalsHeader(lastOutputDir);

        assertTrue(globals.contains("\n#define CN1_ON_DEVICE_DEBUG"),
                "interp-host implies on-device-debug, so its macro must be enabled");
        assertTrue(globals.contains("\n#define CN1_INTERP_HOST"),
                "the interp-host macro must be enabled");
    }

    /** A normal build leaves both commented out and pays for neither. */
    @Test
    void aNormalBuildLeavesTheInterpHostMacroOff() throws Exception {
        translate(false);
        String globals = readGlobalsHeader(lastOutputDir);

        assertTrue(globals.contains("//#define CN1_INTERP_HOST"),
                "a non-interp-host build must leave the macro commented out");
    }

    // ---------------------------------------------------------------- helpers

    /** Output directory of the most recent {@link #translate} call. */
    private Path lastOutputDir;

    private static String readGlobalsHeader(Path outputDir) throws Exception {
        Path h = Files.walk(outputDir)
                .filter(p -> p.getFileName().toString().equals("cn1_globals.h"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no cn1_globals.h under " + outputDir));
        return new String(Files.readAllBytes(h), StandardCharsets.UTF_8);
    }

    private SymbolRows translate(boolean interpHost) throws Exception {
        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);

        Path sourceDir = Files.createTempDirectory("interp-sym-src");
        Path classesDir = Files.createTempDirectory("interp-sym-classes");
        Path outputDir = Files.createTempDirectory("interp-sym-out");
        lastOutputDir = outputDir;

        Files.write(sourceDir.resolve("Main.java"), APP.getBytes(StandardCharsets.UTF_8));

        Path javaApiDir = Files.createTempDirectory("interp-sym-api");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        List<String> args = new ArrayList<>(Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion));
        args.add(CompilerHelper.useClasspath(config) ? "-classpath" : "-bootclasspath");
        args.add(javaApiDir.toString());
        args.addAll(Arrays.asList("-d", classesDir.toString(),
                sourceDir.resolve("Main.java").toString()));
        if (CompilerHelper.compile(config.jdkHome, args) != 0) {
            throw new IllegalStateException("fixture did not compile: "
                    + CompilerHelper.getLastErrorLog());
        }
        CompilerHelper.copyDirectory(javaApiDir, classesDir);

        // The translator reads its flags from system properties in a static
        // initialiser, and runTranslator loads it in a fresh URLClassLoader --
        // so setting them here takes effect for exactly this translation.
        String previousInterp = System.getProperty("cn1.interpHost");
        String previousDebug = System.getProperty("cn1.onDeviceDebug");
        System.setProperty("cn1.interpHost", Boolean.toString(interpHost));
        System.setProperty("cn1.onDeviceDebug", "true");
        try {
            CleanTargetIntegrationTest.runTranslator(classesDir, outputDir, "InterpSymApp");
        } finally {
            restore("cn1.interpHost", previousInterp);
            restore("cn1.onDeviceDebug", previousDebug);
        }

        return SymbolRows.parse(readSymbolTable(outputDir));
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    /** Finds cn1_debug_symbols.c, extracts its byte array and gunzips it. */
    private static String readSymbolTable(Path outputDir) throws Exception {
        Path symbols = Files.walk(outputDir)
                .filter(p -> p.getFileName().toString().equals("cn1_debug_symbols.c"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "no cn1_debug_symbols.c under " + outputDir));

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

    /** The subset of symbol-table rows these tests reason about. */
    private static final class SymbolRows {
        /** mangled class name -> classId */
        final Map<String, Integer> classIds = new HashMap<>();
        /** classId -> vtable slot count */
        final Map<Integer, Integer> vtableSizes = new HashMap<>();
        /** "classId:slot" -> methodId */
        final Map<String, Integer> slots = new HashMap<>();
        /** methodId -> method name */
        final Map<Integer, String> methodNames = new HashMap<>();
        /** methodId -> declaring classId */
        final Map<Integer, Integer> methodOwners = new HashMap<>();

        static SymbolRows parse(String table) {
            SymbolRows r = new SymbolRows();
            for (String line : table.split("\n")) {
                String[] p = line.split("\t", -1);
                switch (p[0]) {
                    case "class":
                        if (p.length >= 3) r.classIds.put(p[2], Integer.parseInt(p[1]));
                        break;
                    case "vtsize":
                        if (p.length >= 3) {
                            r.vtableSizes.put(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                        }
                        break;
                    case "vtable":
                        if (p.length >= 4) {
                            r.slots.put(p[1] + ":" + p[2], Integer.parseInt(p[3]));
                        }
                        break;
                    case "method":
                        if (p.length >= 4) {
                            int mid = Integer.parseInt(p[1]);
                            r.methodOwners.put(mid, Integer.parseInt(p[2]));
                            r.methodNames.put(mid, p[3]);
                        }
                        break;
                    default:
                        break;
                }
            }
            return r;
        }

        int classIdOf(String jvmSimpleName) {
            String mangled = jvmSimpleName.replace('$', '_');
            Integer id = classIds.get(mangled);
            assertNotNull(id, "no class row for " + mangled + ", had: " + classIds.keySet());
            return id;
        }

        /** The vtable slot the given class assigns to a method of that name. */
        Integer slotOfMethodNamed(int classId, String methodName) {
            for (Map.Entry<String, Integer> e : slots.entrySet()) {
                if (!e.getKey().startsWith(classId + ":")) {
                    continue;
                }
                if (methodName.equals(methodNames.get(e.getValue()))) {
                    return Integer.parseInt(e.getKey().split(":")[1]);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            Set<String> vt = new HashSet<>(slots.keySet());
            return "classes=" + classIds.size() + " vtsize=" + vtableSizes.size()
                    + " slots=" + vt.size();
        }
    }
}
