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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/// The file-system half of {@link InterpBundleWriter}: reading class files,
/// sources and resources out of a project directory on the desktop.
///
/// Kept apart so the writer itself uses nothing ParparVM's class library
/// lacks. The device runtime app runs the writer on the phone, after the
/// on-device compiler, and the phone has no `java.io.File` or `java.nio`.
public final class InterpBundleFiles {
    private InterpBundleFiles() {
    }

    /** Adds a class from a {@code .class} file. */
    public static void addClassFile(InterpBundleWriter w, File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            w.addClass(bos.toByteArray());
        } finally {
            in.close();
        }
    }

    /**
     * Adds every non-source file under a directory, keyed by its path relative
     * to that directory. Source files ({@code .java}, {@code .kt}) are skipped
     * because {@link #addSourceTree} has already collected them into the
     * bundle's source section; storing them again as resources doubles the
     * payload of a Kotlin-heavy project and can push it past the service's
     * 64 MiB limit even though the executable half would fit.
     */
    public static void addResourceTree(InterpBundleWriter w, File dir) throws IOException {
        addResourceTree(w, dir, dir);
    }

    private static void addResourceTree(InterpBundleWriter w, File root, File dir) throws IOException {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File f : kids) {
            if (f.isDirectory()) {
                addResourceTree(w, root, f);
            } else if (!InterpBundleWriter.isSourceFile(f.getName())) {
                String rel = f.getAbsolutePath()
                        .substring(root.getAbsolutePath().length())
                        .replace(File.separatorChar, '/');
                w.addResource(rel, Files.readAllBytes(f.toPath()));
            }
        }
    }

    /**
     * Adds every source file under a directory, recursively.
     *
     * <p>Both {@code .java} and {@code .kt} are collected: the pushed bundle's
     * source lookup keys on the {@code SourceFile} attribute javac and kotlinc
     * emit, so a Kotlin class advertises {@code MyApp.kt} and the runtime
     * refuses the whole push as missing source if only {@code .java} is
     * gathered here. Any other language producing JVM classes would need its
     * extension added the same way.</p>
     *
     * <p>Keyed by package rather than by file name. A bare name collides the
     * moment a project has two {@code Util.java} in different packages, and the
     * loser is simply absent -- which the runtime reports as "bundle is missing
     * the source file Util.java" for a file that was right there. Since the
     * source requirement is what lets the app run pushed code at all, losing one
     * silently is not an option.</p>
     */
    public static void addSourceTree(InterpBundleWriter w, File dir) throws IOException {
        File[] kids = dir.listFiles();
        if (kids == null) {
            return;
        }
        for (File f : kids) {
            if (f.isDirectory()) {
                addSourceTree(w, f);
            } else if (InterpBundleWriter.isSourceFile(f.getName())) {
                String text = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                w.addSource(InterpBundleWriter.sourceKey(InterpBundleWriter.packageOf(text, f.getName()), f.getName()), text);
            }
        }
    }
}
