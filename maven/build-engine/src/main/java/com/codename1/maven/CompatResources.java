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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;
import com.codename1.desktopcompat.shared.ResourceNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/// Ships what a desktop application loads by name -- the files it reads with
/// `Class.getResource`, and its resource bundles -- in the form the runtime
/// looks them up, and generates the class that tells the runtime what was
/// shipped.
///
/// #### Flattening
///
/// A device bundle has no directories. Every file of the desktop resource
/// directories that sits in a directory is written to the root of the
/// classes directory under the flat name
/// the desktop runtime's `ResourceNames.flatName` gives its path, and the
/// nested copy the build tool put there is removed. A file already at the
/// root keeps its name and is left alone.
///
/// The flat name is computed by that one method, which the runtime calls too
/// when it turns the name an application asks for into the name to read:
/// this module reads it from the desktop runtime for exactly that reason. The
/// alternatives were a second copy of the rule in this module -- two
/// implementations that must agree character for character, with nothing
/// but a test to say so -- or a mapping table generated into the application,
/// which costs every shipped resource a second string for something a pure
/// function answers.
///
/// A flat name that some OTHER file already has is a build error, never an
/// overwrite: two different resources would answer to one name on the
/// device and nothing would say which.
///
/// #### The registry
///
/// `com.codename1.generated.compat.CompatRegistry` is generated with the
/// path of every shipped resource and every bundle that is a class, created
/// with `new` since a device creates nothing by name; see
/// [ApplicationRegistry]. A `.properties` bundle needs
/// no entry of its own: the class library's `ResourceBundle` reads it as the
/// resource it is. The desktop layers' jars carry an empty class of that name
/// so that their lifecycles, which call its `cn1Install()`, compile; the
/// generated one is the one that ships.
///
/// #### Running it again
///
/// The flat names written are listed in a file BESIDE the classes directory
/// (it is not something to ship). The next run reads it to remove the flat
/// copy of a resource that has since been deleted or renamed, and to tell
/// its own earlier output from somebody else's file of the same name.
final class CompatResources {

    /// What a bundle class descends from: the JDK's names as the application
    /// was compiled against them, and the desktop runtime's shims they are
    /// relocated to, since the scan may see either side of the remap.
    private static final String[] BUNDLE_BASES = {
        "java/util/ResourceBundle", "java/util/ListResourceBundle", "java/util/PropertyResourceBundle",
        CompatLayers.JDK + "java/util/ResourceBundle", CompatLayers.JDK + "java/util/ListResourceBundle",
        CompatLayers.JDK + "java/util/PropertyResourceBundle",
    };
    private static final String PROPERTIES_SUFFIX = ".properties";

    private final File classesDir;
    private final List<File> resourceDirs;
    private final List<File> classDirs;
    private final Log log;

    /// `resourceDirs` are the desktop resource directories; `classDirs` the
    /// further class directories of the application, searched for bundle
    /// classes beside `classesDir`.
    CompatResources(File classesDir, List<File> resourceDirs, List<File> classDirs, Log log) {
        this.classesDir = classesDir;
        this.resourceDirs = resourceDirs;
        this.classDirs = classDirs;
        this.log = log;
    }

    /// The file listing the flat names an earlier run wrote into
    /// `classesDir`.
    static File manifest(File classesDir) {
        File abs = classesDir.getAbsoluteFile();
        return new File(abs.getParentFile(), abs.getName() + ".cn1-compat-resources");
    }

    /// Flattens the resources and writes the registry.
    void run() throws IOException, BuildException {
        TreeMap<String, File> resources = new TreeMap<String, File>();
        for (File dir : resourceDirs) {
            if (dir != null && dir.isDirectory()) {
                collect(dir, "", resources);
            }
        }
        flatten(resources);

        int propertyBundles = 0;
        for (String path : resources.keySet()) {
            if (path.endsWith(PROPERTIES_SUFFIX)) {
                propertyBundles++;
            }
        }
        Map<String, Seen> seen = scanClasses();
        List<String> bundleClasses = bundleClasses(seen);
        ApplicationRegistry.write(classesDir, ApplicationRegistry.generate(
                new ArrayList<String>(resources.keySet()), bundleClasses), log);
        log.debug("Shipped " + resources.size() + " desktop resources, " + propertyBundles
                + " properties bundles and " + bundleClasses.size() + " bundle classes");
    }

    /// Every file under `dir`, by its path from the resource root. Compiled
    /// classes are not resources, and neither is what an operating system
    /// leaves in a directory under a name starting with a dot.
    private void collect(File dir, String rel, Map<String, File> out) throws BuildException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(".")) {
                continue;
            }
            if (f.isDirectory()) {
                collect(f, rel + name + "/", out);
            } else if (!name.endsWith(".class")) {
                String path = rel + name;
                File first = out.get(path);
                if (first != null) {
                    throw new BuildException("The resource " + path + " exists twice, as " + first + " and as " + f
                            + ". Only one of them can ship; remove or rename the other.");
                }
                out.put(path, f);
            }
        }
    }

    private void flatten(Map<String, File> resources) throws IOException, BuildException {
        File manifest = manifest(classesDir);
        Set<String> previous = new LinkedHashSet<String>();
        if (manifest.isFile()) {
            for (String line : new String(Files.readAllBytes(manifest.toPath()), "UTF-8").split("\n")) {
                if (line.length() > 0) {
                    previous.add(line);
                }
            }
        }
        Map<String, String> flatToPath = new HashMap<String, String>();
        TreeSet<String> written = new TreeSet<String>();
        for (Map.Entry<String, File> e : resources.entrySet()) {
            String path = e.getKey();
            if (path.indexOf('/') < 0) {
                continue;
            }
            String flat = ResourceNames.flatName(path);
            String taken = null;
            if (resources.containsKey(flat)) {
                taken = resources.get(flat).toString();
            } else if (flatToPath.containsKey(flat)) {
                taken = "the resource " + flatToPath.get(flat);
            }
            File nested = new File(classesDir, path);
            // The build tool's copy, when there is one: it may have been
            // filtered, and it is the bytes the developer's build produced.
            byte[] data = taken != null ? null
                    : Files.readAllBytes((nested.isFile() ? nested : e.getValue()).toPath());
            File dest = new File(classesDir, flat);
            if (taken == null && dest.exists() && !previous.contains(flat)
                    && !(dest.isFile() && Arrays.equals(Files.readAllBytes(dest.toPath()), data))) {
                taken = dest.toString();
            }
            if (taken != null) {
                throw collision(path, flat, taken);
            }
            flatToPath.put(flat, path);
            ClassRelocator.writeIfDifferent(dest, data);
            written.add(flat);
            if (nested.isFile()) {
                removeNested(nested);
            }
        }
        for (String stale : previous) {
            if (!written.contains(stale) && !resources.containsKey(stale)) {
                File f = new File(classesDir, stale);
                if (f.isFile() && !f.delete()) {
                    log.warn("Could not delete " + f);
                }
            }
        }
        StringBuilder list = new StringBuilder();
        for (String flat : written) {
            list.append(flat).append('\n');
        }
        if (written.isEmpty()) {
            if (manifest.isFile() && !manifest.delete()) {
                log.warn("Could not delete " + manifest);
            }
        } else {
            ClassRelocator.writeIfDifferent(manifest, list.toString().getBytes("UTF-8"));
        }
    }

    private static BuildException collision(String path, String flat, String taken) {
        return new BuildException("The resource " + path + " ships as " + flat + " -- a device bundle has no "
                + "directories, so every nested resource is flattened to one name -- but that name is already "
                + "taken by " + taken + ". Rename one of the two.");
    }

    /// Deletes the nested copy and every directory that leaves empty.
    private void removeNested(File nested) {
        if (!nested.delete()) {
            log.warn("Could not delete " + nested);
            return;
        }
        File root = classesDir.getAbsoluteFile();
        File dir = nested.getAbsoluteFile().getParentFile();
        while (dir != null && !dir.equals(root)) {
            String[] left = dir.list();
            if (left == null || left.length > 0 || !dir.delete()) {
                return;
            }
            dir = dir.getParentFile();
        }
    }

    /// What the scan keeps of one application class.
    private static final class Seen {
        String superName;
        boolean creatable;
    }

    /// The application's bundle classes, as dotted names in order: public,
    /// concrete, with a public constructor taking nothing, and descending
    /// from `ResourceBundle`. Anything else `getBundle` could not have
    /// created on a desktop either.
    private static List<String> bundleClasses(Map<String, Seen> seen) {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Seen> e : seen.entrySet()) {
            if (e.getValue().creatable && isBundle(e.getValue().superName, seen)) {
                out.add(e.getKey().replace('/', '.'));
            }
        }
        return out;
    }

    /// Every class the application ships that is not a layer's runtime, by
    /// internal name.
    private Map<String, Seen> scanClasses() throws IOException {
        final Map<String, Seen> seen = new TreeMap<String, Seen>();
        List<File> dirs = new ArrayList<File>();
        dirs.add(classesDir);
        dirs.addAll(classDirs);
        for (File dir : dirs) {
            scan(dir, "", seen);
        }
        return seen;
    }

    private static boolean isBundle(String superName, Map<String, Seen> seen) {
        // Bounded, so that a malformed (circular) hierarchy ends.
        for (int depth = 0; superName != null && depth < 64; depth++) {
            for (String base : BUNDLE_BASES) {
                if (base.equals(superName)) {
                    return true;
                }
            }
            Seen parent = seen.get(superName);
            superName = parent == null ? null : parent.superName;
        }
        return false;
    }

    private static void scan(File dir, String rel, final Map<String, Seen> seen) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String child = rel + f.getName() + "/";
                if (!CompatLayers.isExtractedRuntime(child)) {
                    scan(f, child, seen);
                }
            } else if (f.getName().endsWith(".class")) {
                new ClassReader(Files.readAllBytes(f.toPath())).accept(new ClassVisitor(Opcodes.ASM9) {
                    private Seen current;

                    @Override
                    public void visit(int version, int access, String name, String signature, String superName,
                                      String[] interfaces) {
                        current = new Seen();
                        current.superName = superName;
                        // Creatable so far; the constructor decides the rest.
                        current.creatable = (access & Opcodes.ACC_PUBLIC) != 0
                                && (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) == 0;
                        seen.put(name, current);
                        hasConstructor = false;
                    }

                    private boolean hasConstructor;

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                                     String[] exceptions) {
                        if ("<init>".equals(name) && "()V".equals(descriptor) && (access & Opcodes.ACC_PUBLIC) != 0) {
                            hasConstructor = true;
                        }
                        return null;
                    }

                    @Override
                    public void visitEnd() {
                        current.creatable = current.creatable && hasConstructor;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        }
    }
}
