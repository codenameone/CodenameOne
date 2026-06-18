/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.builders.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Build-time decision: should this build upload its symbol bundle to
 * the crash protection service? Crash protection is opt-in by design --
 * we never ship a developer's symbols off the build server unless they
 * explicitly asked us to.
 *
 * <p>Two independent signals trigger an opt-in for a given build +
 * platform:
 *
 * <ol>
 *   <li><b>Build hint property</b>:
 *       {@code codename1.crashProtection.enabled=true} in
 *       {@code codenameone_settings.properties} (the app project's
 *       build configuration). Lets a developer flip crash protection
 *       on without having to commit to also calling the runtime API
 *       yet.</li>
 *   <li><b>Bytecode reference</b>: the user's compiled classes
 *       contain a reference to {@code com.codename1.crash.CrashProtection}.
 *       Detected via a simple bytestring scan of the user's class
 *       output (the constant pool of any class that imports the API
 *       carries the descriptor as a literal). Means the user wired the
 *       runtime API and we infer build-time intent.</li>
 * </ol>
 *
 * <p>Per-platform opt-OUT: even when the global signal is true the
 * developer can disable a specific platform with
 * {@code codename1.crashProtection.<platform>.enabled=false}. Defaults
 * to true for every platform when the global is true. Useful when a
 * platform's symbol upload is noisy or the developer doesn't ship
 * that target.
 *
 * <p>This class is intentionally dependency-free (no Spring, no ASM):
 * it runs inside the cloud build executor's classpath and must not
 * pull in heavy transitive deps.
 */
public final class CrashProtectionOptIn {

    /** Master opt-in property -- see class comment. */
    public static final String PROPERTY_GLOBAL = "codename1.crashProtection.enabled";

    /** Per-platform opt-OUT property template. */
    public static final String PROPERTY_PER_PLATFORM_TEMPLATE = "codename1.crashProtection.%s.enabled";

    /**
     * Class name (slashed JVM internal form) we search for in compiled
     * classes to detect API usage. The unslashed form would also work
     * but bytecode descriptors use the slashed variant, so this matches
     * any class that imports or references CrashProtection.
     */
    private static final String API_MARKER = "com/codename1/crash/CrashProtection";

    private CrashProtectionOptIn() {
    }

    /**
     * True if the given platform should upload symbols for this build.
     *
     * @param platform e.g. {@code "android"} / {@code "ios"} /
     *     {@code "mac"} / {@code "linux"} / {@code "win32"}.
     * @param projectProperties merged build properties for the project
     *     (i.e. parsed {@code codenameone_settings.properties}).
     *     Pass {@code null} when no project properties are available;
     *     the bytecode-scan signal can still fire.
     * @param compiledClasses directory or jar containing the user's
     *     compiled classes. Pass {@code null} to skip the bytecode
     *     scan (e.g. when the classes haven't been built yet).
     */
    public static boolean shouldUpload(String platform,
            Map<String, String> projectProperties, File compiledClasses) {
        if (isPerPlatformDisabled(platform, projectProperties)) {
            return false;
        }
        if (isGlobalEnabled(projectProperties)) {
            return true;
        }
        return referencesCrashProtectionApi(compiledClasses);
    }

    static boolean isGlobalEnabled(Map<String, String> props) {
        if (props == null) return false;
        String v = props.get(PROPERTY_GLOBAL);
        return v != null && Boolean.parseBoolean(v.trim());
    }

    static boolean isPerPlatformDisabled(String platform, Map<String, String> props) {
        if (props == null || platform == null || platform.isEmpty()) return false;
        String key = String.format(PROPERTY_PER_PLATFORM_TEMPLATE, platform.toLowerCase());
        String v = props.get(key);
        // A user who's never touched the per-platform key gets the
        // implicit default (not disabled). Only an explicit `false`
        // suppresses the platform.
        return v != null && !Boolean.parseBoolean(v.trim());
    }

    /**
     * Scan the user's compiled classes for a reference to
     * {@code com.codename1.crash.CrashProtection}. Accepts either a
     * directory of .class files or a .jar.
     */
    static boolean referencesCrashProtectionApi(File compiledClasses) {
        if (compiledClasses == null || !compiledClasses.exists()) return false;
        try {
            if (compiledClasses.isDirectory()) {
                return scanDirectory(compiledClasses);
            }
            if (compiledClasses.isFile() && compiledClasses.getName().endsWith(".jar")) {
                return scanJar(compiledClasses);
            }
        } catch (IOException ignored) {
            // A read error during scanning falls through to "not
            // detected" -- the property path is still available if the
            // user wants to be explicit.
        }
        return false;
    }

    private static boolean scanDirectory(File dir) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) return false;
        for (File f : children) {
            if (f.isDirectory()) {
                if (scanDirectory(f)) return true;
            } else if (f.getName().endsWith(".class")) {
                byte[] bytes = Files.readAllBytes(f.toPath());
                if (containsMarker(bytes)) return true;
            }
        }
        return false;
    }

    private static boolean scanJar(File jar) throws IOException {
        try (ZipFile zf = new ZipFile(jar)) {
            java.util.Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().endsWith(".class")) continue;
                try (InputStream in = zf.getInputStream(e)) {
                    byte[] bytes = drain(in);
                    if (containsMarker(bytes)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Stream-to-byte-array. {@code InputStream.readAllBytes()} would
     * be cleaner but only landed in Java 9; this module compiles for
     * Java 8 source to match the wider CN1 maven plugin baseline.
     */
    private static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * Boyer-Moore-free literal byte search. The marker is ASCII so a
     * naive scan over the raw class bytes is fine; constant-pool
     * UTF-8 entries store the marker verbatim.
     */
    private static boolean containsMarker(byte[] bytes) {
        // Explicit US_ASCII because the marker is a JVM-internal name
        // (only [A-Za-z0-9/_$]); avoids SpotBugs DM_DEFAULT_ENCODING
        // and matches the encoding actually used by the constant-pool
        // UTF-8 entries we are scanning.
        byte[] needle = API_MARKER.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (needle.length == 0 || bytes.length < needle.length) return false;
        outer:
        for (int i = 0; i <= bytes.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}
