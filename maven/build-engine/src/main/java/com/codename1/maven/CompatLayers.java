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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
///
/// The two desktop layers share one source directory (`src/main/desktop`), so
/// the templates add both of their jars and cannot know which of them the
/// sources need. For those the jar is necessary but not sufficient: the
/// application's compiled classes must also name the layer's API
/// ([#active(Iterable, Iterable)]). A Swing application therefore does not
/// ship the JavaFX runtime, and nothing has to be declared for that.
public final class CompatLayers {

    /// The Swing layer. `java.awt` and `javax.swing` belong to the JDK, so
    /// unlike the other layers its runtime cannot be authored under the names
    /// an application compiles against: the application compiles against the
    /// JDK's own classes, and the runtime jar is written directly under the
    /// names it ships with. It therefore has no bridge package to move, and
    /// relocating its jar changes no class name.
    public static final Relocation SWING = new Relocation("Swing", "codenameone-swing-compat",
            "com/codename1/desktopcompat/",
            new String[] {"java/awt/", "javax/swing/", "java/beans/", "javax/accessibility/", "javax/imageio/",
                "org/jdesktop/"},
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

    /// Whether `layer` is switched on by its jar alone. The Android layer is:
    /// its sources have a directory of their own, and the build compiles
    /// resources and generates classes for it before any class exists to be
    /// examined. The desktop layers also have to be used.
    public static boolean activeByPresence(Relocation layer) {
        return layer != SWING && layer != JAVAFX;
    }

    /// The layers an application ships: those whose runtime jar is among
    /// `classpath` and which, unless the jar alone decides
    /// ([#activeByPresence]), a class under one of `classDirs` refers to.
    ///
    /// A reference counts under the name the application was compiled against
    /// (`javax/swing/JTable`) and under the name it has once relocated, so a
    /// directory an earlier run already rewrote gives the same answer. The
    /// runtimes a previous run extracted into a directory are not application
    /// code and are not read.
    public static List<Relocation> active(Iterable<File> classpath, Iterable<File> classDirs) throws IOException {
        List<Relocation> present = active(classpath);
        List<Relocation> undecided = new ArrayList<Relocation>();
        for (Relocation r : present) {
            if (!activeByPresence(r)) {
                undecided.add(r);
            }
        }
        if (undecided.isEmpty()) {
            return present;
        }
        ReferenceScan scan = new ReferenceScan(undecided);
        if (classDirs != null) {
            for (File dir : classDirs) {
                if (dir != null && dir.isDirectory()) {
                    scan.directory(dir, "");
                }
            }
        }
        List<Relocation> out = new ArrayList<Relocation>();
        for (Relocation r : present) {
            if (activeByPresence(r) || scan.found.contains(r)) {
                out.add(r);
            }
        }
        return out;
    }

    /// Finds which of a set of layers some class refers to, stopping as soon
    /// as every one of them has been seen.
    private static final class ReferenceScan extends Remapper {
        private final List<Relocation> wanted;
        private final List<Relocation> found = new ArrayList<Relocation>();

        ReferenceScan(List<Relocation> wanted) {
            this.wanted = wanted;
        }

        @Override
        public String map(String internalName) {
            if (internalName != null) {
                for (Relocation r : wanted) {
                    if (!found.contains(r) && (r.owns(internalName) || r.original(internalName) != null)) {
                        found.add(r);
                    }
                }
            }
            return internalName;
        }

        private boolean done() {
            return found.size() == wanted.size();
        }

        /// `rel` is the directory's path inside the classes directory, with
        /// its trailing slash, or empty for the classes directory itself.
        void directory(File dir, String rel) throws IOException {
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            for (File f : files) {
                if (done()) {
                    return;
                }
                if (f.isDirectory()) {
                    String child = rel + f.getName() + "/";
                    if (!isExtractedRuntime(child)) {
                        directory(f, child);
                    }
                } else if (f.getName().endsWith(".class")) {
                    // Every name a class file holds goes through the remapper:
                    // supertypes, descriptors, signatures, annotations and
                    // the instructions' operands alike.
                    new ClassReader(Files.readAllBytes(f.toPath())).accept(
                            new ClassRemapper(new ClassWriter(0), this), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
    }

    /// Whether a class of `jar` refers to one of `layers`: what makes a
    /// dependency a library written against a desktop toolkit
    /// ([CompatLibraries]).
    static boolean jarRefersTo(File jar, List<Relocation> layers) throws IOException {
        ReferenceScan scan = new ReferenceScan(layers);
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar);
        try {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements() && scan.found.isEmpty()) {
                java.util.zip.ZipEntry e = entries.nextElement();
                if (e.isDirectory() || !e.getName().endsWith(".class") || e.getName().startsWith("META-INF/")) {
                    continue;
                }
                java.io.InputStream in = zip.getInputStream(e);
                try {
                    new ClassReader(in).accept(new ClassRemapper(new ClassWriter(0), scan),
                            ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                } catch (RuntimeException unreadable) {
                    // A class file newer than this build reads names nothing
                    // that can be relocated; the compliance check reports it
                    // if it ever becomes part of the application.
                    continue;
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }
        return !scan.found.isEmpty();
    }

    /// Whether `rel` (a directory inside a classes directory, with its
    /// trailing slash) is where some layer's runtime, or the shared JDK
    /// classes, are extracted.
    static boolean isExtractedRuntime(String rel) {
        if (rel.startsWith(Relocation.JDK_PACKAGE)) {
            return true;
        }
        for (Relocation r : ALL) {
            if (rel.startsWith(r.target())) {
                return true;
            }
        }
        return false;
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
