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
package com.codename1.maven;

import com.codename1.builders.BuildException;

import java.io.File;
import java.io.IOException;

/// The desktop sources of an application module: `src/main/desktop`, holding
/// a Swing or JavaFX application as its own build laid it out.
///
/// ```
/// src/main/desktop/java       Java sources
/// src/main/desktop/kotlin     Kotlin sources (optional)
/// src/main/desktop/resources  classpath resources: .fxml, .css, .properties, images
/// ```
///
/// Nothing in the directory says how the application starts. That is the
/// project's Codename One main class, an ordinary source file in
/// `src/main/java` that `cn1:import-desktop-project` writes
/// ([DesktopProjectImporter]): it extends the Swing or JavaFX lifecycle and
/// calls the desktop application from source. A project whose main class
/// does neither uses the desktop sources as a library.
///
/// The directory existing is what switches the desktop layers on: the project
/// templates add both runtime jars when it does, and the remap step ships
/// whichever of them the compiled sources use
/// ([CompatLayers#active(Iterable, Iterable)]). Both build plugins read the
/// layout from here, so they cannot disagree on it.
public final class DesktopSources {

    private DesktopSources() {
    }

    /// Whether the module has desktop sources, which is what enables the
    /// desktop layers.
    public static boolean isDesktopProject(File desktopDir) {
        return desktopDir != null && desktopDir.isDirectory();
    }

    public static File javaDir(File desktopDir) {
        return new File(desktopDir, "java");
    }

    public static File kotlinDir(File desktopDir) {
        return new File(desktopDir, "kotlin");
    }

    public static File resourcesDir(File desktopDir) {
        return new File(desktopDir, "resources");
    }

    /// The desktop layers whose API the sources under `desktopDir` name, as
    /// text: `""`, `"Swing"`, `"JavaFX"` or `"Swing,JavaFX"`.
    ///
    /// This is not what decides which runtime ships -- the compiled classes
    /// do ([CompatLayers#active(Iterable, Iterable)]). It is a cheap value
    /// that changes when that decision may have: Gradle makes it an input of
    /// the task that takes the decision, so that the task runs again.
    public static String toolkitsNamedIn(File desktopDir) {
        boolean[] named = new boolean[DESKTOP_LAYERS.length];
        try {
            scan(javaDir(desktopDir), named);
            scan(kotlinDir(desktopDir), named);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < named.length; i++) {
            if (named[i]) {
                out.append(out.length() == 0 ? "" : ",").append(DESKTOP_LAYERS[i].name());
            }
        }
        return out.toString();
    }

    private static final Relocation[] DESKTOP_LAYERS = {CompatLayers.SWING, CompatLayers.JAVAFX};

    private static void scan(File dir, boolean[] named) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                scan(f, named);
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".kt")) {
                String text = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
                for (int i = 0; i < named.length; i++) {
                    named[i] = named[i] || DESKTOP_LAYERS[i].namedInSource(text);
                }
            }
        }
    }

    /// Fails when the module has desktop sources but neither desktop runtime
    /// among its dependencies: the sources could compile (Swing code compiles
    /// against the JDK itself) and would then reach the compliance check as
    /// plain unsupported API. `artifactIds` are the module's resolved
    /// dependencies.
    public static void requireRuntime(File desktopDir, Iterable<String> artifactIds) throws BuildException {
        if (!isDesktopProject(desktopDir)) {
            return;
        }
        for (String id : artifactIds) {
            if (CompatLayers.SWING.artifactId().equals(id) || CompatLayers.JAVAFX.artifactId().equals(id)) {
                return;
            }
        }
        throw new BuildException("src/main/desktop exists but neither the " + CompatLayers.SWING.artifactId()
                + " nor the " + CompatLayers.JAVAFX.artifactId()
                + " dependency is present; add both to the common module (scope provided).");
    }
}
