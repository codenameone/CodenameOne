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
package com.codename1.impl.javase.bluetooth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates and loads the bundled {@code libcn1ble} shared library for the
 * current host so the JavaSE simulator can drive real Bluetooth in-process
 * (the library exports the {@code JniBleBridge} JNI entry points). Resolution
 * order: the {@link #LIBRARY_PATH_PROPERTY} system property, then the
 * OS/arch-keyed classpath resource under {@link #LIBRARY_RESOURCE_DIR}
 * (extracted to a temp file and {@link System#load(String) loaded}).
 * {@code attempted} collects a human-readable trace of every location tried.
 */
final class BleLibraryResolver {

    private BleLibraryResolver() {
    }

    /** System property naming an explicit {@code libcn1ble} to load. */
    static final String LIBRARY_PATH_PROPERTY = "cn1.bluetooth.libraryPath";

    /** Classpath directory of the OS-keyed bundled libraries. */
    static final String LIBRARY_RESOURCE_DIR =
            "/com/codename1/impl/javase/bluetooth/native/";

    private static Boolean loaded;
    private static String resolution = "not attempted";

    /**
     * Loads {@code libcn1ble} once (idempotent). Returns {@code true} when the
     * library is loaded and its JNI symbols are available. Failures are
     * captured in {@link #describeResolution()} rather than thrown, so the
     * caller can fall back to the simulator backend.
     */
    static synchronized boolean load() {
        if (loaded != null) {
            return loaded.booleanValue();
        }
        List<String> attempted = new ArrayList<String>();
        boolean ok = tryLoad(System.getProperty(LIBRARY_PATH_PROPERTY),
                System.getProperty("os.name"), System.getProperty("os.arch"),
                attempted);
        resolution = join(attempted);
        loaded = Boolean.valueOf(ok);
        return ok;
    }

    /** Human-readable trace of the locations that were tried. */
    static String describeResolution() {
        return resolution;
    }

    private static boolean tryLoad(String propertyValue, String osName,
            String osArch, List<String> attempted) {
        if (propertyValue != null && propertyValue.length() > 0) {
            File f = new File(propertyValue);
            if (f.isFile()) {
                try {
                    System.load(f.getAbsolutePath());
                    attempted.add("system property " + LIBRARY_PATH_PROPERTY
                            + " => loaded " + f.getAbsolutePath());
                    return true;
                } catch (Throwable ex) {
                    attempted.add("system property " + LIBRARY_PATH_PROPERTY
                            + "=" + propertyValue + " (load failed: " + ex
                            + ")");
                }
            } else {
                attempted.add("system property " + LIBRARY_PATH_PROPERTY + "="
                        + propertyValue + " (no such file)");
            }
        } else {
            attempted.add("system property " + LIBRARY_PATH_PROPERTY
                    + " (not set)");
        }
        String resourcePath = libraryResourcePath(osName, osArch);
        if (resourcePath == null) {
            attempted.add("no bundled libcn1ble for os.name=" + osName
                    + " os.arch=" + osArch);
            return false;
        }
        File extracted = extractResource(resourcePath, attempted);
        if (extracted == null) {
            return false;
        }
        try {
            System.load(extracted.getAbsolutePath());
            attempted.add("classpath resource " + resourcePath + " => loaded "
                    + extracted.getAbsolutePath());
            return true;
        } catch (Throwable ex) {
            attempted.add("classpath resource " + resourcePath
                    + " (load failed: " + ex + ")");
            return false;
        }
    }

    /**
     * Classpath location of {@code libcn1ble} for the OS and CPU
     * architecture, or {@code null} when nothing is bundled. Ships from the
     * cn1-binaries repository via the maven/javase resource mapping, laid out
     * as {@code ble/macos/libcn1ble.dylib} (a universal Mach-O covering both
     * architectures) and {@code ble/{linux,windows}/{x64,arm64}/} (ELF and PE
     * have no fat-binary format, so those are per-arch).
     */
    static String libraryResourcePath(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return LIBRARY_RESOURCE_DIR + "macos/libcn1ble.dylib";
        }
        String arch = normalizeArch(osArch);
        if (arch == null) {
            return null;
        }
        if (os.contains("linux")) {
            return LIBRARY_RESOURCE_DIR + "linux/" + arch + "/libcn1ble.so";
        }
        if (os.contains("windows")) {
            return LIBRARY_RESOURCE_DIR + "windows/" + arch + "/cn1ble.dll";
        }
        return null;
    }

    /**
     * Maps {@code os.arch} onto the bundled directory names, or {@code null}
     * for architectures no library is shipped for (32-bit x86 and ARM among
     * them).
     */
    static String normalizeArch(String osArch) {
        String arch = osArch == null ? "" : osArch.toLowerCase();
        if (arch.equals("amd64") || arch.equals("x86_64")
                || arch.equals("x64")) {
            return "x64";
        }
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            return "arm64";
        }
        return null;
    }

    private static File extractResource(String resourcePath,
            List<String> attempted) {
        InputStream src =
                BleLibraryResolver.class.getResourceAsStream(resourcePath);
        if (src == null) {
            attempted.add("classpath resource " + resourcePath + " (missing)");
            return null;
        }
        try {
            String suffix = resourcePath.substring(
                    resourcePath.lastIndexOf('.'));
            File out = File.createTempFile("libcn1ble-", suffix);
            out.deleteOnExit();
            FileOutputStream sink = new FileOutputStream(out);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = src.read(buf)) >= 0) {
                    sink.write(buf, 0, n);
                }
            } finally {
                sink.close();
            }
            return out;
        } catch (IOException ex) {
            attempted.add("classpath resource " + resourcePath
                    + " (extraction failed: " + ex + ")");
            return null;
        } finally {
            try {
                src.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        int size = parts.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }
}
