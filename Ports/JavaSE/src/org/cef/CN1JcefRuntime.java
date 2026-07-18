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
package org.cef;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefBuildInfo;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.EnumPlatform;
import me.friwi.jcefmaven.UnsupportedPlatformException;

/**
 * Owns the JCEF Maven runtime used by the JavaSE port.
 *
 * <p>This class is deliberately in the {@code org.cef} package.  The
 * simulator keeps that package in its parent class loader so JCEF and its
 * native libraries are initialized exactly once while application classes
 * can still be reloaded.</p>
 */
public final class CN1JcefRuntime {
    public static final String INSTALL_DIR_PROPERTY = "cn1.jcef.installDir";
    public static final String MIRRORS_PROPERTY = "cn1.jcef.mirrors";

    private static CefApp instance;

    private CN1JcefRuntime() {
    }

    /**
     * Returns whether JCEF Maven supports the current operating system and
     * architecture.
     *
     * @return true if a native JCEF distribution is available
     */
    public static boolean isSupported() {
        try {
            EnumPlatform.getCurrentPlatform();
            CefBuildInfo.fromClasspath();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * Creates a builder configured with Codename One's shared, versioned JCEF
     * cache and optional download mirrors.
     *
     * @param args Chromium command-line arguments
     * @return the configured builder
     * @throws IOException if the JCEF build metadata cannot be read
     * @throws UnsupportedPlatformException if the current platform is unsupported
     */
    public static CefAppBuilder createBuilder(String[] args) throws IOException, UnsupportedPlatformException {
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(getInstallDir());
        if (args != null) {
            builder.addJcefArgs(args);
        }
        List<String> mirrors = getConfiguredMirrors();
        if (!mirrors.isEmpty()) {
            builder.setMirrors(mirrors);
        }
        return builder;
    }

    /**
     * Installs the native runtime, if necessary, and initializes JCEF.  A file
     * lock protects the shared cache when two simulator or desktop processes
     * start for the first time concurrently.
     *
     * @param builder a configured JCEF Maven builder
     * @return the process-wide JCEF application
     * @throws IOException if the runtime cannot be installed
     * @throws UnsupportedPlatformException if the current platform is unsupported
     * @throws InterruptedException if initialization is interrupted
     * @throws CefInitializationException if JCEF cannot be initialized
     */
    public static synchronized CefApp build(CefAppBuilder builder)
            throws IOException, UnsupportedPlatformException, InterruptedException,
            CefInitializationException {
        if (instance != null) {
            return instance;
        }

        File installDir = getInstallDir();
        File lockFile = new File(installDir.getParentFile(), installDir.getName() + ".lock");
        File parent = lockFile.getParentFile();
        if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create JCEF cache directory " + parent);
        }

        RandomAccessFile lockAccess = new RandomAccessFile(lockFile, "rw");
        try {
            FileChannel channel = lockAccess.getChannel();
            try {
                FileLock lock = channel.lock();
                try {
                    builder.install();
                } finally {
                    lock.release();
                }
            } finally {
                channel.close();
            }
        } finally {
            lockAccess.close();
        }

        instance = builder.build();
        return instance;
    }

    static File getInstallDir() throws IOException, UnsupportedPlatformException {
        String configured = System.getProperty(INSTALL_DIR_PROPERTY);
        if (configured != null && configured.trim().length() > 0) {
            File installDir = new File(configured.trim()).getAbsoluteFile();
            if (installDir.getParentFile() == null
                    || installDir.getCanonicalFile().equals(
                            new File(System.getProperty("user.home")).getCanonicalFile())) {
                throw new IOException(INSTALL_DIR_PROPERTY
                        + " must point to a dedicated JCEF runtime directory");
            }
            return installDir;
        }
        CefBuildInfo buildInfo = CefBuildInfo.fromClasspath();
        String platform = EnumPlatform.getCurrentPlatform().getIdentifier();
        return new File(new File(new File(System.getProperty("user.home"), ".codenameone"), "jcef"),
                buildInfo.getReleaseTag() + File.separator + platform).getAbsoluteFile();
    }

    static List<String> getConfiguredMirrors() {
        List<String> out = new ArrayList<String>();
        String configured = System.getProperty(MIRRORS_PROPERTY);
        if (configured == null) {
            return out;
        }
        String[] parts = configured.split("[,\\r\\n]+");
        for (String part : parts) {
            String mirror = part.trim();
            if (mirror.length() > 0) {
                out.add(mirror);
            }
        }
        return out;
    }
}
