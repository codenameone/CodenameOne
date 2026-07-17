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
import java.util.List;

/**
 * Locates the bundled {@code cn1-ble-helper} executable for the current
 * host, so the JavaSE simulator and the native Windows/Linux ports resolve
 * it identically. Resolution order: the {@link #HELPER_PATH_PROPERTY}
 * system property, the OS/arch-keyed classpath resource under
 * {@link #HELPER_RESOURCE_DIR} (extracted to a temp file), then a
 * {@code PATH} lookup. {@code attempted} collects a human-readable trace of
 * every location tried for error messages.
 */
public final class HelperBinaryResolver {

    private HelperBinaryResolver() {
    }

    /**
     * System property naming an explicit helper binary; checked before the
     * classpath resource and the {@code PATH} lookup.
     */
    public static final String HELPER_PATH_PROPERTY =
            "cn1.bluetooth.helperPath";

    /** Classpath directory of the OS-keyed bundled helper binaries. */
    public static final String HELPER_RESOURCE_DIR =
            "/com/codename1/impl/javase/bluetooth/native/";

    /** Base name of the helper executable. */
    public static final String HELPER_BASENAME = "cn1-ble-helper";

    /**
     * Resolution order: explicit system property, bundled classpath
     * resource for the OS (extracted to a temp file), {@code PATH} lookup.
     * Returns {@code null} when nothing was found; {@code attempted}
     * collects a human-readable trace for error messages.
     */
    public static File resolveHelperBinary(String propertyValue, String osName,
            String osArch, String pathEnv, List<String> attempted) {
        if (propertyValue != null && propertyValue.length() > 0) {
            File f = new File(propertyValue);
            if (f.isFile()) {
                attempted.add("system property " + HELPER_PATH_PROPERTY
                        + " => " + f.getAbsolutePath());
                return f;
            }
            attempted.add("system property " + HELPER_PATH_PROPERTY + "="
                    + propertyValue + " (no such file)");
        } else {
            attempted.add("system property " + HELPER_PATH_PROPERTY
                    + " (not set)");
        }
        String resourcePath = helperResourcePath(osName, osArch);
        if (resourcePath == null) {
            attempted.add("no bundled helper for os.name=" + osName
                    + " os.arch=" + osArch);
        } else {
            File extracted = extractResource(resourcePath, attempted);
            if (extracted != null) {
                return extracted;
            }
        }
        File onPath = resolveFromPathEnv(pathEnv,
                helperExecutableName(osName), attempted);
        if (onPath != null) {
            return onPath;
        }
        return null;
    }

    /**
     * Classpath location of the helper binary for the OS and CPU
     * architecture, or {@code null} when no binary is bundled for the
     * combination. The binaries ship from the cn1-binaries repository via
     * the maven/javase resource mapping, laid out as
     * {@code ble/macos/cn1-ble-helper} (a universal Mach-O binary covering
     * both architectures) and {@code ble/{linux,windows}/{x64,arm64}/}
     * (ELF and PE have no fat-binary format, so those are per-arch).
     */
    public static String helperResourcePath(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            // universal binary: one file serves x86_64 and arm64
            return HELPER_RESOURCE_DIR + "macos/" + HELPER_BASENAME;
        }
        String arch = normalizeArch(osArch);
        if (arch == null) {
            return null;
        }
        if (os.contains("linux")) {
            return HELPER_RESOURCE_DIR + "linux/" + arch + "/"
                    + HELPER_BASENAME;
        }
        if (os.contains("windows")) {
            return HELPER_RESOURCE_DIR + "windows/" + arch + "/"
                    + HELPER_BASENAME + ".exe";
        }
        return null;
    }

    /**
     * Maps {@code os.arch} onto the directory names used by the bundled
     * binaries, or {@code null} for architectures no binary is shipped for
     * (32-bit x86 and 32-bit ARM among them) -- resolution then falls
     * through to the {@code PATH} lookup, so a self-built helper still
     * works there.
     */
    public static String normalizeArch(String osArch) {
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

    /** The platform file name of the helper executable. */
    public static String helperExecutableName(String osName) {
        String os = osName == null ? "" : osName.toLowerCase();
        return os.contains("windows") ? HELPER_BASENAME + ".exe"
                : HELPER_BASENAME;
    }

    /** Extracts the bundled helper to a temp file, or null when absent. */
    private static File extractResource(String resourcePath,
            List<String> attempted) {
        InputStream src =
                HelperBinaryResolver.class.getResourceAsStream(resourcePath);
        if (src == null) {
            attempted.add("classpath resource " + resourcePath
                    + " (missing)");
            return null;
        }
        try {
            boolean exe = resourcePath.endsWith(".exe");
            File out = File.createTempFile(HELPER_BASENAME + "-",
                    exe ? ".exe" : "");
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
            out.setExecutable(true, true);
            attempted.add("classpath resource " + resourcePath
                    + " => " + out.getAbsolutePath());
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

    /** Scans the given PATH-style value for the helper executable. */
    public static File resolveFromPathEnv(String pathEnv, String executableName,
            List<String> attempted) {
        if (pathEnv != null) {
            String[] dirs = pathEnv.split(File.pathSeparator);
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].length() == 0) {
                    continue;
                }
                File candidate = new File(dirs[i], executableName);
                if (candidate.isFile()) {
                    attempted.add("PATH lookup => "
                            + candidate.getAbsolutePath());
                    return candidate;
                }
            }
        }
        attempted.add("PATH lookup for " + executableName + " (not found)");
        return null;
    }

    /** Joins the attempted-location trace into one description string. */
    public static String join(List<String> parts) {
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
