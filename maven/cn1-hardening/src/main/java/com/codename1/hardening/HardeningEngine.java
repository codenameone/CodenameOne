/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The one public entry point of the hardening engine. Given the merged application
 * jar and a resolved config, it produces a hardened jar plus a cross-platform
 * ProGuard mapping.
 *
 * <p>The pipeline is: demux the fat jar to a class-only jar (non-class entries
 * preserved byte-for-byte); assemble keep rules; rename with ProGuard using the
 * prefixed dictionary (skipped on Android, where R8 remains the sole renamer);
 * encrypt strings; guard against ParparVM mangle collisions; verify every class;
 * rebuild the output jar; and finalize the mapping.
 */
public final class HardeningEngine {

    public static final String ENGINE_VERSION = "1.0.0";
    public static final String PROGUARD_VERSION = "7.3.2";
    /** Highest Java feature version whose class files ProGuard 7.3.2 can read. */
    public static final int PROGUARD_MAX_JDK = 20;

    private HardeningEngine() {
    }

    /**
     * Whether ProGuard can run on the current JVM. 7.3.2 cannot read class files newer than
     * JDK 20 (it fails on the JDK's own module classes), so renaming must run on JDK 8-20 --
     * the cloud daemon forks the engine on JDK 17. String encryption and control flow have no
     * such limit.
     */
    public static boolean proguardCanRunHere() {
        return currentJdkFeature() <= PROGUARD_MAX_JDK;
    }

    static int currentJdkFeature() {
        String v = System.getProperty("java.specification.version", "1.8");
        if (v.startsWith("1.")) {
            v = v.substring(2);
        }
        int dot = v.indexOf('.');
        if (dot >= 0) {
            v = v.substring(0, dot);
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    public static String engineVersion() {
        return ENGINE_VERSION;
    }

    public static HardeningResult harden(HardeningRequest req) throws HardeningException {
        HardeningConfig cfg = req.getConfig();
        require(req.getInputJar() != null && req.getInputJar().isFile(), "input jar is missing");
        require(req.getOutputJar() != null, "output jar path is missing");
        require(cfg != null, "config is missing");

        if (cfg.getProfile() == HardeningProfile.OFF) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, req.getInputJar());
        }
        if (!cfg.isPlatformEnabled()) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_PLATFORM_DISABLED, req.getInputJar());
        }
        // A non-off level whose transforms are all individually disabled (e.g. harden.rename=false +
        // harden.strings=off + no control flow) does nothing -- don't rebuild the jar and stamp it
        // "hardened" with an empty transform set. Treat it as not requested.
        if (!willApplyAnyTransform(cfg)) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, req.getInputJar());
        }

        File workDir = req.getWorkDir();
        if (workDir == null) {
            workDir = req.getOutputJar().getAbsoluteFile().getParentFile();
        }
        workDir.mkdirs();

        try {
            return run(req, cfg, workDir);
        } catch (IOException e) {
            throw new HardeningException("Hardening failed: " + e.getMessage(), e);
        }
    }

    private static HardeningResult run(HardeningRequest req, HardeningConfig cfg, File workDir)
            throws HardeningException, IOException {
        File classesJar = new File(workDir, "classes-in.jar");
        JarDemuxer.NonClassEntries nonClass = JarDemuxer.split(req.getInputJar(), classesJar);
        Map<String, byte[]> inClasses = JarDemuxer.readClasses(classesJar);
        int classesIn = inClasses.size();

        List<String> keepRules = new ArrayList<String>();
        keepRules.addAll(BuiltinKeepRules.rules(req.getMainClass()));
        InputJarKeepScanner scanner = new InputJarKeepScanner();
        scanner.scan(inClasses);
        keepRules.addAll(scanner.keepRules());
        // Keep classes named by META-INF/services descriptors: those files are copied verbatim, so
        // ServiceLoader would fail if the service interface or a provider class were renamed.
        keepRules.addAll(serviceDescriptorKeeps(nonClass));
        keepRules.addAll(cfg.getExtraKeepRules());

        // Export the derived keep rules for a downstream renamer the engine doesn't drive itself.
        // On Android R8 is the sole renamer (isRenameEnabled()==false), so without this the classes
        // the scanner found reflectively (Class.forName targets, service providers, name-bound
        // property objects, the app's own harden.keep) would be invisible to R8 and get renamed.
        if (req.getR8KeepFile() != null) {
            StringBuilder r8 = new StringBuilder();
            for (String rule : keepRules) {
                r8.append(rule).append('\n');
            }
            writeText(req.getR8KeepFile(), r8.toString());
        }

        Map<String, byte[]> renamed;
        int renamedCount = 0;
        File mappingFile = req.getMappingFile();
        File hierarchyJar;

        if (cfg.isRenameEnabled()) {
            if (!proguardCanRunHere()) {
                throw new HardeningException("App hardening's renamer (ProGuard " + PROGUARD_VERSION
                        + ") must run on JDK 8-" + PROGUARD_MAX_JDK + ", but this JVM is JDK "
                        + currentJdkFeature() + ". The Codename One build server runs the engine on "
                        + "JDK 17; for a local hardened build, run it on JDK 8-" + PROGUARD_MAX_JDK + ".");
            }
            File dict = new File(workDir, "cn1-dict.txt");
            // Seed the dictionary so harden.seed / the build key actually changes the mapping.
            Cn1NameFactory.writeDictionary(dict, Cn1NameFactory.dictionarySizeFor(classesIn),
                    deriveSeed(cfg, req.getBuildKey()));
            File renamedJar = new File(workDir, "renamed.jar");
            ProGuardRunner.rename(classesJar, renamedJar, mappingFile,
                    req.getLibraryJars(), keepRules, dict, workDir, cfg.getPlatform());
            renamed = JarDemuxer.readClasses(renamedJar);
            renamedCount = countRenamed(inClasses.keySet(), renamed.keySet());
            hierarchyJar = renamedJar;
        } else {
            renamed = new LinkedHashMap<String, byte[]>(inClasses);
            hierarchyJar = classesJar;
            if (mappingFile != null) {
                writeText(mappingFile, "");
            }
        }

        // Classloader over the (renamed) app classes plus the library jars, so stack-map frame
        // computation resolves the class hierarchy without loading types through the engine's own
        // classloader (see FrameClassWriter).
        ClassLoader hierarchy = buildHierarchyLoader(hierarchyJar, req.getLibraryJars());

        int seed = deriveSeed(cfg, req.getBuildKey());
        int encryptedStrings = 0;
        int concatLiterals = 0;
        int legacyInterfaceConstants = 0;
        int oversizedLiterals = 0;
        int condyLiterals = 0;
        int clinitFullLiterals = 0;
        boolean stringsApplied = cfg.isAnyStringEncryption() && stringEncryptionSafeFor(cfg.getPlatform());
        if (stringsApplied) {
            // In "constants" mode, first collect the values declared as static-final String
            // constants across the whole jar, so we encrypt exactly those (and javac's inlined
            // copies) and nothing incidental.
            java.util.Set<String> constantValues = null;
            if (!cfg.isEncryptAllStrings()) {
                constantValues = new java.util.HashSet<String>();
                for (byte[] cls : renamed.values()) {
                    StringEncryptTransform.collectConstantValues(cls, constantValues);
                }
            }
            for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                StringEncryptTransform t = new StringEncryptTransform(
                        cfg.isEncryptAllStrings(), seed, hierarchy, constantValues);
                byte[] out = t.transform(e.getValue());
                if (out != e.getValue()) {
                    e.setValue(out);
                }
                encryptedStrings += t.getEncryptedCount();
                concatLiterals += t.getConcatLiteralCount();
                legacyInterfaceConstants += t.getLegacyInterfaceConstantCount();
                oversizedLiterals += t.getOversizedLiteralCount();
                condyLiterals += t.getCondyLiteralCount();
                clinitFullLiterals += t.getClinitFullLiteralCount();
            }
        }

        int guardedMethods = 0;
        int oversizedGuardMethods = 0;
        boolean controlFlowApplied = cfg.isControlFlow() && controlFlowSafeFor(cfg.getPlatform());
        if (controlFlowApplied) {
            for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                ControlFlowTransform t = new ControlFlowTransform(hierarchy, cfg.getControlFlowIntensity());
                byte[] out = t.transform(e.getValue());
                if (out != e.getValue()) {
                    e.setValue(out);
                }
                oversizedGuardMethods += t.getOversizedMethods();
                guardedMethods += t.getGuardedMethods();
            }
        }

        // Even when the config asked for a transform, the input may contain nothing eligible (e.g.
        // rename off, and no static-final string longer than two characters to encrypt): every
        // counter stays zero and no transform actually ran. Stamping cn1.hardened=true for a
        // byte-unchanged app would be dishonest, so report SKIPPED and let the caller keep the input.
        // Android still counts as hardened here because R8 renames downstream (isRenameRequested).
        boolean anyApplied = cfg.isRenameEnabled()
                || cfg.isRenameRequested()
                || (stringsApplied && encryptedStrings > 0)
                || (controlFlowApplied && guardedMethods > 0);
        if (!anyApplied) {
            return HardeningResult.skipped(HardeningResult.Outcome.SKIPPED_NOT_REQUESTED, req.getInputJar());
        }

        // The a.b_c / a.b.c -> a_b_c collision only exists in the ParparVM C symbol mangle. On
        // Android (R8/DEX -- and the engine does not even rename there) and JavaSE (plain JVM),
        // '.' vs '_' stay distinct, so two legal classes must not abort the build.
        if (translatesThroughParparVMC(cfg.getPlatform())) {
            MangleCollisionCheck.check(renamed.keySet());
        }
        OutputVerifier.verify(renamed, hierarchy);

        // Idempotence marker: a nested builder delegation must not harden twice.
        nonClass.asMap().put("META-INF/CN1-HARDENED",
                (ENGINE_VERSION + " " + cfg.getProfile().name().toLowerCase())
                        .getBytes(java.nio.charset.Charset.forName("UTF-8")));

        JarDemuxer.rebuild(req.getOutputJar(), renamed, nonClass);

        // Only the engine's own rename produces a mapping worth an id. On Android the engine does
        // not rename (R8 is the sole renamer and produces the real per-build mapping later), so the
        // engine mapping is empty -- hashing it would stamp a meaningless constant id. Leave it empty.
        String mappingId = "";
        if (mappingFile != null && cfg.isRenameEnabled()) {
            mappingId = MappingWriter.finalizeMapping(mappingFile, ENGINE_VERSION, PROGUARD_VERSION,
                    cfg.getPlatform(), req.getBuildKey());
        }

        HardeningResult result = HardeningResult.hardened(req.getOutputJar(), mappingFile);
        result.setClassesIn(classesIn);
        result.setClassesOut(renamed.size());
        result.setRenamedClasses(renamedCount);
        result.setEncryptedStrings(encryptedStrings);
        result.setMappingId(mappingId);
        // Report only what actually ran, so a "half-hardened" build can never claim a
        // transform it skipped. This is what the downstream verifier checks against.
        if (cfg.isRenameEnabled()) {
            result.getTransformsApplied().add("rename");
        } else if (cfg.isRenameRequested()) {
            // Android: the engine doesn't rename, R8 does. Still a rename, still hardened.
            result.getTransformsApplied().add("rename:r8");
        }
        if (stringsApplied && encryptedStrings > 0) {
            result.getTransformsApplied().add(cfg.isEncryptAllStrings() ? "strings:all" : "strings:constants");
        }
        if (controlFlowApplied && guardedMethods > 0) {
            result.getTransformsApplied().add(cfg.getControlFlowIntensity() >= 2
                    ? "controlFlow:intense" : "controlFlow");
        }
        if (cfg.isControlFlow() && !controlFlowApplied) {
            result.getWarnings().add("control-flow obfuscation is not applied on platform '"
                    + cfg.getPlatform() + "' (unsafe for the ParparVM optimizer); skipped");
        }
        if (cfg.isAnyStringEncryption() && !stringsApplied) {
            result.getWarnings().add("string encryption is not applied on platform '"
                    + cfg.getPlatform() + "' (would break the JavaScript native bridge); skipped");
        }
        if (stringsApplied && concatLiterals > 0) {
            // javac from JDK 9 compiles string concatenation to an invokedynamic whose literal
            // fragments live in the StringConcatFactory recipe, not in an LDC or a ConstantValue, so
            // the string channels cannot reach them. Report it rather than let a build believe it is
            // fully string-encrypted; -XDstringConcat=inline (or an older -target) emits StringBuilder
            // the engine does encrypt.
            result.getWarnings().add(concatLiterals + " string-concatenation literal group(s) compiled "
                    + "to invokedynamic (JDK 9+ javac) were not encrypted; compile with "
                    + "-XDstringConcat=inline or an older -target to encrypt concatenation literals");
        }
        if (stringsApplied && legacyInterfaceConstants > 0) {
            // A pre-Java-8 interface cannot host a <clinit>/decoder, so its own static-final String
            // constants stay plaintext in that class file. Reads elsewhere were inlined and are
            // encrypted; report the declaring-interface leak rather than ship it unremarked.
            result.getWarnings().add(legacyInterfaceConstants + " static-final String constant(s) on "
                    + "pre-Java-8 interface(s) were not encrypted (such interfaces cannot host the "
                    + "decoder); recompile the interface at -target 8+ to encrypt its constant pool");
        }
        if (stringsApplied && condyLiterals > 0) {
            // Java 11+ can carry plaintext through an LDC constant-dynamic whose bootstrap arguments
            // hold the string; it is resolved at link time, so it cannot be rewritten to a decode call.
            result.getWarnings().add(condyLiterals + " constant-dynamic (LDC ConstantDynamic) site(s) "
                    + "carrying string bootstrap arguments were not encrypted; such constants are "
                    + "resolved at link time and remain in plaintext");
        }
        if (stringsApplied && oversizedLiterals > 0) {
            // A literal longer than ~21,845 chars can widen to a 3-byte-per-char constant whose
            // ciphertext overflows the 65535-byte constant pool, so it is left plaintext. Report it
            // rather than let an strings:all build claim it encrypted everything.
            result.getWarnings().add(oversizedLiterals + " string literal(s) were too large to encrypt "
                    + "(their ciphertext would overflow the 65535-byte constant pool) and remain in "
                    + "plaintext; move a large embedded secret/blob out of a string constant to hide it");
        }
        if (stringsApplied && clinitFullLiterals > 0) {
            result.getWarnings().add(clinitFullLiterals + " string literal(s) were left in plaintext "
                    + "because the class's static initializer is already near the 65535-byte method "
                    + "limit and could not hold the decode step");
        }
        if (controlFlowApplied && oversizedGuardMethods > 0) {
            result.getWarnings().add(oversizedGuardMethods + " method(s) were left with plain control "
                    + "flow because they are already near the 65535-byte method limit and adding the "
                    + "guard would overflow them");
        }
        if (req.getReportFile() != null) {
            writeReport(req.getReportFile(), cfg, result);
        }
        return result;
    }

    /**
     * String encryption is disabled on the JavaScript port for now: the ParparVM JS backend's
     * minifier treats certain string literals as live references into the CN1 native bridge, and
     * encrypting one would break the bridge. Every other port is safe (the decoder is ordinary
     * translated/compiled code).
     */
    /** True when at least one transform will actually run for this config and platform. */
    static boolean willApplyAnyTransform(HardeningConfig cfg) {
        // A per-platform opt-out means nothing runs for this target -- so a non-entitled build with
        // harden.<platform>.enabled=false is skipped, not rejected as not-entitled.
        if (!cfg.isPlatformEnabled()) {
            return false;
        }
        // renameRequested (not renameEnabled): on Android the engine does not rename, but R8 does,
        // so a rename-only Android build is still a hardened build and must not be skipped.
        if (cfg.isRenameRequested()) {
            return true;
        }
        if (cfg.isAnyStringEncryption() && stringEncryptionSafeFor(cfg.getPlatform())) {
            return true;
        }
        return cfg.isControlFlow() && controlFlowSafeFor(cfg.getPlatform());
    }

    static boolean stringEncryptionSafeFor(String platform) {
        return !"javascript".equals(platform);
    }

    /**
     * Control-flow obfuscation runs only on the JVM-bytecode ports (Android, desktop). The
     * ParparVM native ports (ios/mac/watch/tv/win/linux) translate to C, where the opaque
     * predicate fights the optimizer/devirtualizer and the arithmetic reducer, and the JavaScript
     * port inflates the bundle and confuses the suspension analysis; those are left untouched.
     */
    static boolean controlFlowSafeFor(String platform) {
        return "and".equals(platform) || "android".equals(platform)
                || "javase".equals(platform) || "desktop".equals(platform);
    }

    /**
     * The ports whose classes are translated to C by ParparVM, where the class/package mangle
     * ({@code . / $} all collapse to {@code _}) can make two legal Java names share one C symbol.
     * The collision guard is meaningful only for these; Android (DEX) and JavaSE (JVM) keep the
     * names distinct, and JavaScript uses a different mangling entirely.
     */
    static boolean translatesThroughParparVMC(String platform) {
        return "ios".equals(platform) || "mac".equals(platform) || "watch".equals(platform)
                || "tv".equals(platform) || "win".equals(platform) || "linux".equals(platform);
    }

    /**
     * A classloader over the (renamed) application classes plus the library jars, for stack-map
     * frame computation. JDK library classes resolve through the parent (bootstrap) loader, so the
     * jmods are intentionally not added -- URLClassLoader can't read them and java.* resolves via
     * the parent anyway. Never initializes classes (FrameClassWriter uses initialize=false).
     */
    private static ClassLoader buildHierarchyLoader(File hierarchyJar, List<File> libraryJars) {
        List<java.net.URL> urls = new ArrayList<java.net.URL>();
        try {
            if (hierarchyJar != null && hierarchyJar.isFile()) {
                urls.add(hierarchyJar.toURI().toURL());
            }
            if (libraryJars != null) {
                for (File lib : libraryJars) {
                    if (lib != null && lib.isFile()) {
                        urls.add(lib.toURI().toURL());
                    }
                }
            }
        } catch (java.net.MalformedURLException e) {
            return HardeningEngine.class.getClassLoader();
        }
        return new java.net.URLClassLoader(urls.toArray(new java.net.URL[urls.size()]),
                HardeningEngine.class.getClassLoader());
    }

    /**
     * Keep rules for every class named by a {@code META-INF/services/*} descriptor -- the service
     * interface (the file name) and each provider class listed inside. The descriptors are carried
     * across verbatim, so renaming any of these would break {@code ServiceLoader}.
     */
    private static List<String> serviceDescriptorKeeps(JarDemuxer.NonClassEntries nonClass) {
        List<String> rules = new ArrayList<String>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        String prefix = "META-INF/services/";
        for (Map.Entry<String, byte[]> e : nonClass.asMap().entrySet()) {
            String name = e.getKey();
            if (!name.startsWith(prefix) || name.length() <= prefix.length()) {
                continue;
            }
            addServiceKeep(rules, seen, name.substring(prefix.length()));
            String body = new String(e.getValue(), java.nio.charset.Charset.forName("UTF-8"));
            for (String line : body.split("\\r?\\n")) {
                int hash = line.indexOf('#');
                if (hash >= 0) {
                    line = line.substring(0, hash);
                }
                addServiceKeep(rules, seen, line.trim());
            }
        }
        return rules;
    }

    private static void addServiceKeep(List<String> rules, java.util.Set<String> seen, String className) {
        String c = className.trim();
        if (c.length() == 0 || !isPlausibleClassName(c) || !seen.add(c)) {
            return;
        }
        rules.add("-keep class " + c + " { *; }");
    }

    private static boolean isPlausibleClassName(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '$') {
                return false;
            }
        }
        return true;
    }

    private static int deriveSeed(HardeningConfig cfg, String buildKey) {
        String basis = cfg.getSeed() != null ? cfg.getSeed()
                : (buildKey == null || buildKey.isEmpty() ? "cn1-hardening" : buildKey);
        int h = 0;
        for (int i = 0; i < basis.length(); i++) {
            h = h * 31 + basis.charAt(i);
        }
        return h;
    }

    private static int countRenamed(java.util.Set<String> before, java.util.Set<String> after) {
        int n = 0;
        for (String b : before) {
            if (!after.contains(b)) {
                n++;
            }
        }
        return n;
    }

    private static void writeReport(File reportFile, HardeningConfig cfg, HardeningResult r)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"engine\": \"").append(ENGINE_VERSION).append("\",\n");
        sb.append("  \"proguard\": \"").append(PROGUARD_VERSION).append("\",\n");
        sb.append("  \"platform\": \"").append(json(cfg.getPlatform())).append("\",\n");
        sb.append("  \"profile\": \"").append(cfg.getProfile().name().toLowerCase()).append("\",\n");
        sb.append("  \"outcome\": \"").append(r.getOutcome().name()).append("\",\n");
        sb.append("  \"classesIn\": ").append(r.getClassesIn()).append(",\n");
        sb.append("  \"classesOut\": ").append(r.getClassesOut()).append(",\n");
        sb.append("  \"renamedClasses\": ").append(r.getRenamedClasses()).append(",\n");
        sb.append("  \"encryptedStrings\": ").append(r.getEncryptedStrings()).append(",\n");
        sb.append("  \"mappingId\": \"").append(json(r.getMappingId())).append("\",\n");
        sb.append("  \"transforms\": [");
        List<String> t = r.getTransformsApplied();
        for (int i = 0; i < t.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(json(t.get(i))).append('"');
        }
        sb.append("],\n");
        // Serialize the warnings too: a warning records a known limitation (e.g. plaintext left in a
        // JDK 9+ concat recipe, or a transform skipped as unsafe for the platform). A consumer reading
        // the report -- not the forked-process log -- would otherwise see "transforms":["strings:all"]
        // with no hint that some plaintext was excluded, which the doc promises the report surfaces.
        sb.append("  \"warnings\": [");
        List<String> w = r.getWarnings();
        for (int i = 0; i < w.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"').append(json(w.get(i))).append('"');
        }
        sb.append("]\n");
        sb.append("}\n");
        writeText(reportFile, sb.toString());
    }

    private static String json(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void writeText(File f, String text) throws IOException {
        FileOutputStream fo = new FileOutputStream(f);
        try {
            Writer w = new OutputStreamWriter(fo, Charset.forName("UTF-8"));
            w.write(text);
            w.flush();
        } finally {
            fo.close();
        }
    }

    private static void require(boolean cond, String message) throws HardeningException {
        if (!cond) {
            throw new HardeningException(message);
        }
    }
}
