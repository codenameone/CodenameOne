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

    /// Where the desktop runtime's JDK classes live: `java/io/File` ships as
    /// `com/codename1/desktopcompat/shared/jdk/java/io/File`.
    static final String JDK = "com/codename1/desktopcompat/shared/jdk/";

    /// The JDK classes a device does not have and a desktop application uses,
    /// renamed onto the desktop runtime's implementations over Codename One:
    /// files and `java.nio.file` over `FileSystemStorage`, `URL` over the
    /// network manager and the flattened resources, bundles over the
    /// generated registry, logging over `Log`, preferences over
    /// `Preferences`. A nested class goes with its class.
    ///
    /// The Android layer shims the `java.io.File` family too, to its own
    /// classes, and a name can ship under one class only: when a desktop
    /// layer is active these rows win for every class in the application,
    /// the Android runtime's included ([ClassRelocator#map]), so a `File`
    /// handed from Android code to Swing code is the same class on both
    /// sides.
    static final String[][] DESKTOP_JDK_SHIMS = {
        {"java/io/File", JDK + "java/io/File"},
        {"java/io/FileFilter", JDK + "java/io/FileFilter"},
        {"java/io/FileInputStream", JDK + "java/io/FileInputStream"},
        {"java/io/FileOutputStream", JDK + "java/io/FileOutputStream"},
        {"java/io/FileReader", JDK + "java/io/FileReader"},
        {"java/io/FileWriter", JDK + "java/io/FileWriter"},
        {"java/io/FilenameFilter", JDK + "java/io/FilenameFilter"},

        {"java/net/URL", JDK + "java/net/URL"},

        {"java/nio/file/AccessDeniedException", JDK + "java/nio/file/AccessDeniedException"},
        {"java/nio/file/CopyOption", JDK + "java/nio/file/CopyOption"},
        {"java/nio/file/DirectoryIteratorException", JDK + "java/nio/file/DirectoryIteratorException"},
        {"java/nio/file/DirectoryNotEmptyException", JDK + "java/nio/file/DirectoryNotEmptyException"},
        {"java/nio/file/DirectoryStream", JDK + "java/nio/file/DirectoryStream"},
        {"java/nio/file/FileAlreadyExistsException", JDK + "java/nio/file/FileAlreadyExistsException"},
        {"java/nio/file/FileSystemException", JDK + "java/nio/file/FileSystemException"},
        {"java/nio/file/FileVisitOption", JDK + "java/nio/file/FileVisitOption"},
        {"java/nio/file/FileVisitResult", JDK + "java/nio/file/FileVisitResult"},
        {"java/nio/file/FileVisitor", JDK + "java/nio/file/FileVisitor"},
        {"java/nio/file/Files", JDK + "java/nio/file/Files"},
        {"java/nio/file/InvalidPathException", JDK + "java/nio/file/InvalidPathException"},
        {"java/nio/file/LinkOption", JDK + "java/nio/file/LinkOption"},
        {"java/nio/file/NoSuchFileException", JDK + "java/nio/file/NoSuchFileException"},
        {"java/nio/file/NotDirectoryException", JDK + "java/nio/file/NotDirectoryException"},
        {"java/nio/file/OpenOption", JDK + "java/nio/file/OpenOption"},
        {"java/nio/file/Path", JDK + "java/nio/file/Path"},
        {"java/nio/file/Paths", JDK + "java/nio/file/Paths"},
        {"java/nio/file/SimpleFileVisitor", JDK + "java/nio/file/SimpleFileVisitor"},
        {"java/nio/file/StandardCopyOption", JDK + "java/nio/file/StandardCopyOption"},
        {"java/nio/file/StandardOpenOption", JDK + "java/nio/file/StandardOpenOption"},
        {"java/nio/file/attribute/BasicFileAttributes", JDK + "java/nio/file/attribute/BasicFileAttributes"},
        {"java/nio/file/attribute/FileAttribute", JDK + "java/nio/file/attribute/FileAttribute"},
        {"java/nio/file/attribute/FileTime", JDK + "java/nio/file/attribute/FileTime"},

        {"java/util/ResourceBundle", JDK + "java/util/ResourceBundle"},
        {"java/util/ListResourceBundle", JDK + "java/util/ListResourceBundle"},
        {"java/util/PropertyResourceBundle", JDK + "java/util/PropertyResourceBundle"},

        {"java/util/logging/Level", JDK + "java/util/logging/Level"},
        {"java/util/logging/Logger", JDK + "java/util/logging/Logger"},

        {"java/util/prefs/BackingStoreException", JDK + "java/util/prefs/BackingStoreException"},
        {"java/util/prefs/Preferences", JDK + "java/util/prefs/Preferences"},
    };

    private static String[][] desktopShims() {
        String[][] out = new String[Relocation.JDK_SHIMS.length + DESKTOP_JDK_SHIMS.length][];
        System.arraycopy(DESKTOP_JDK_SHIMS, 0, out, 0, DESKTOP_JDK_SHIMS.length);
        System.arraycopy(Relocation.JDK_SHIMS, 0, out, DESKTOP_JDK_SHIMS.length, Relocation.JDK_SHIMS.length);
        return out;
    }

    /// The Swing layer. `java.awt` and `javax.swing` belong to the JDK, so
    /// unlike the other layers its runtime cannot be authored under the names
    /// an application compiles against: the application compiles against the
    /// JDK's own classes, and the runtime jar is written directly under the
    /// names it ships with. It therefore has no bridge package to move, and
    /// relocating its jar changes no class name.
    public static final Relocation SWING = new Relocation("Swing", "codenameone-swing-compat",
            "com/codename1/desktopcompat/",
            new String[] {"java/awt/", "javax/swing/", "java/beans/", "javax/accessibility/", "javax/imageio/",
                "org/jdesktop/", "com/formdev/flatlaf/", "org/fife/"},
            desktopShims(), null, null);

    /// The JavaFX layer, authored under `javafx.*` and relocated like the
    /// Android one.
    public static final Relocation JAVAFX = new Relocation("JavaFX", "codenameone-javafx-compat",
            "com/codename1/fxcompat/", new String[] {"javafx/"}, desktopShims(),
            "com/codename1/fxcompat/runtime/", "com/codename1/fxcompat/rt/");

    /// The artifact of the runtime both desktop layers share -- the
    /// resource lookups, the uncaught exception forwarder, `Runtime.exec` --
    /// which both layers' jars depend on. It is copied into every
    /// application that has a desktop layer, as the layers' own runtimes are.
    public static final String DESKTOP_RUNTIME = "codenameone-desktop-compat-runtime";

    /// The package of [#DESKTOP_RUNTIME], as an internal name prefix. It is
    /// under the Swing layer's target, so the directory walks that skip an
    /// extracted runtime skip it too.
    public static final String DESKTOP_RUNTIME_PACKAGE = "com/codename1/desktopcompat/shared/";

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

    /// Whether `rel` (a directory inside a classes directory, with its
    /// trailing slash) is where some layer's runtime is extracted.
    static boolean isExtractedRuntime(String rel) {
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
}
