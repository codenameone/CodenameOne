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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/// Every compatibility layer the build knows, and which of them an
/// application has switched on.
///
/// A layer is switched on by its runtime jar being on the compile classpath:
/// the project templates add that dependency when the layer's source directory
/// exists, so nothing else has to be configured.
public final class CompatLayers {

    /// The Swing layer. `java.awt` and `javax.swing` belong to the JDK, so
    /// unlike the other layers its runtime cannot be authored under the names
    /// an application compiles against: the application compiles against the
    /// JDK's own classes, and the runtime jar is written directly under the
    /// names it ships with. It therefore has no bridge package to move, and
    /// relocating its jar changes no class name.
    public static final Relocation SWING = new Relocation("Swing", "codenameone-swing-compat",
            "com/codename1/desktopcompat/",
            new String[] {"java/awt/", "javax/swing/", "java/beans/", "javax/accessibility/", "org/jdesktop/"},
            Relocation.JDK_SHIMS, null, null);

    /// The JavaFX layer, authored under `javafx.*` and relocated like the
    /// Android one.
    public static final Relocation JAVAFX = new Relocation("JavaFX", "codenameone-javafx-compat",
            "com/codename1/fxcompat/", new String[] {"javafx/"}, Relocation.JDK_SHIMS,
            "com/codename1/fxcompat/runtime/", "com/codename1/fxcompat/rt/");

    /// Every layer, in the order their rules are tried.
    public static final List<Relocation> ALL = Collections.unmodifiableList(
            Arrays.asList(AndroidRemapper.RELOCATION, SWING, JAVAFX));

    /// A relocator applying every layer's rules. Their packages are disjoint,
    /// so it is correct for any class; what it cannot say is which layers an
    /// application actually ships -- [#active] does.
    public static final ClassRelocator EVERY = new ClassRelocator(ALL);

    private CompatLayers() {
    }

    /// The layers whose runtime jar is among `classpath`.
    public static List<Relocation> active(Iterable<File> classpath) {
        List<Relocation> out = new ArrayList<Relocation>();
        for (Relocation r : ALL) {
            if (runtimeJar(r, classpath) != null) {
                out.add(r);
            }
        }
        return out;
    }

    /// `layer`'s runtime jar among `classpath`, or null.
    public static File runtimeJar(Relocation layer, Iterable<File> classpath) {
        File found = null;
        for (File f : classpath) {
            if (f != null && layer.isRuntimeJar(f.getName())) {
                found = f;
            }
        }
        return found;
    }

    /// The layer whose API `internalName` belongs to, as an application is
    /// compiled against it (`javax/swing/JTable`), or null.
    public static Relocation owning(String internalName) {
        for (Relocation r : ALL) {
            if (r.owns(internalName)) {
                return r;
            }
        }
        return null;
    }

    /// What a build tells a developer whose application uses `layer`'s API
    /// without having switched the layer on, or null when the build has
    /// nothing to suggest. The only place this wording lives.
    public static String enableHint(Relocation layer) {
        if (layer == SWING) {
            return "Codename One runs AWT/Swing code through its Swing compatibility layer, which this project "
                    + "has not enabled. Move the Swing sources to src/main/desktop to enable it, or use "
                    + "com.codename1.ui components for UI logic.";
        }
        if (layer == JAVAFX) {
            return "Codename One runs JavaFX code through its JavaFX compatibility layer, which this project "
                    + "has not enabled. Move the JavaFX sources to src/main/desktop to enable it, or use "
                    + "com.codename1.ui components for UI logic.";
        }
        return null;
    }

    /// The jar of shared JDK classes among `classpath`, or null.
    public static File jdkJar(Iterable<File> classpath) {
        File found = null;
        for (File f : classpath) {
            if (f != null && f.getName().startsWith(Relocation.JDK_ARTIFACT + "-") && f.getName().endsWith(".jar")) {
                found = f;
            }
        }
        return found;
    }
}
