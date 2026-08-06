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

    private HardeningEngine() {
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
        keepRules.addAll(cfg.getExtraKeepRules());

        Map<String, byte[]> renamed;
        int renamedCount = 0;
        File mappingFile = req.getMappingFile();

        if (cfg.isRenameEnabled()) {
            File dict = new File(workDir, "cn1-dict.txt");
            Cn1NameFactory.writeDictionary(dict, Cn1NameFactory.dictionarySizeFor(classesIn));
            File renamedJar = new File(workDir, "renamed.jar");
            ProGuardRunner.rename(classesJar, renamedJar, mappingFile,
                    req.getLibraryJars(), keepRules, dict, workDir);
            renamed = JarDemuxer.readClasses(renamedJar);
            renamedCount = countRenamed(inClasses.keySet(), renamed.keySet());
        } else {
            renamed = new LinkedHashMap<String, byte[]>(inClasses);
            if (mappingFile != null) {
                writeText(mappingFile, "");
            }
        }

        int seed = deriveSeed(cfg, req.getBuildKey());
        int encryptedStrings = 0;
        boolean stringsApplied = cfg.isAnyStringEncryption() && stringEncryptionSafeFor(cfg.getPlatform());
        if (stringsApplied) {
            for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                StringEncryptTransform t = new StringEncryptTransform(cfg.isEncryptAllStrings(), seed);
                byte[] out = t.transform(e.getValue());
                if (out != e.getValue()) {
                    e.setValue(out);
                }
                encryptedStrings += t.getEncryptedCount();
            }
        }

        int guardedMethods = 0;
        boolean controlFlowApplied = cfg.isControlFlow() && controlFlowSafeFor(cfg.getPlatform());
        if (controlFlowApplied) {
            for (Map.Entry<String, byte[]> e : renamed.entrySet()) {
                ControlFlowTransform t = new ControlFlowTransform();
                byte[] out = t.transform(e.getValue());
                if (out != e.getValue()) {
                    e.setValue(out);
                }
                guardedMethods += t.getGuardedMethods();
            }
        }

        MangleCollisionCheck.check(renamed.keySet());
        OutputVerifier.verify(renamed);

        // Idempotence marker: a nested builder delegation must not harden twice.
        nonClass.asMap().put("META-INF/CN1-HARDENED",
                (ENGINE_VERSION + " " + cfg.getProfile().name().toLowerCase())
                        .getBytes(java.nio.charset.Charset.forName("UTF-8")));

        JarDemuxer.rebuild(req.getOutputJar(), renamed, nonClass);

        String mappingId = "";
        if (mappingFile != null) {
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
        }
        if (stringsApplied && encryptedStrings > 0) {
            result.getTransformsApplied().add(cfg.isEncryptAllStrings() ? "strings:all" : "strings:constants");
        }
        if (controlFlowApplied && guardedMethods > 0) {
            result.getTransformsApplied().add("controlFlow");
        }
        if (cfg.isControlFlow() && !controlFlowApplied) {
            result.getWarnings().add("control-flow obfuscation is not applied on platform '"
                    + cfg.getPlatform() + "' (unsafe for the ParparVM optimizer); skipped");
        }
        if (cfg.isAnyStringEncryption() && !stringsApplied) {
            result.getWarnings().add("string encryption is not applied on platform '"
                    + cfg.getPlatform() + "' (would break the JavaScript native bridge); skipped");
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
