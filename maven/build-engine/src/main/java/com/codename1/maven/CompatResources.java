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
import com.codename1.compat.jdk.ResourceNames;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
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
/// `com.codename1.compat.jdk.ResourceNames.flatName` gives its path, and the
/// nested copy the build tool put there is removed. A file already at the
/// root keeps its name and is left alone.
///
/// The flat name is computed by that one method, which the runtime calls too
/// when it turns the name an application asks for into the name to read:
/// this module depends on the shared JDK classes' artifact for exactly that
/// reason. The alternatives were a second copy of the rule in this module --
/// two implementations that must agree character for character, with nothing
/// but a test to say so -- or a mapping table generated into the application,
/// which costs every shipped resource a second string for something a pure
/// function answers. The artifact depends only on the core, so nothing
/// becomes circular.
///
/// A flat name that some OTHER file already has is a build error, never an
/// overwrite: two different resources would answer to one name on the
/// device and nothing would say which.
///
/// #### The registry
///
/// `com.codename1.compat.jdk.CompatRegistry` is generated with the path of
/// every shipped resource, every `.properties` bundle among them and every
/// bundle that is a class, the last created with `new` in a `switch` since a
/// device creates nothing by name. The shared JDK classes carry an empty
/// class of that name so that they compile; `CompatBoot.cn1Init()` runs
/// whichever of the two ships. The registrations themselves sit in part
/// classes beside it, `CompatRegistry$Part0` onwards; see [#registry].
///
/// #### Running it again
///
/// The flat names written are listed in a file BESIDE the classes directory
/// (it is not something to ship). The next run reads it to remove the flat
/// copy of a resource that has since been deleted or renamed, and to tell
/// its own earlier output from somebody else's file of the same name.
final class CompatResources {

    static final String REGISTRY = Relocation.JDK_PACKAGE + "CompatRegistry";
    private static final String RESOURCES = Relocation.JDK_PACKAGE + "Resources";
    private static final String BUNDLE = Relocation.JDK_PACKAGE + "ResourceBundle";
    private static final String FACTORY = BUNDLE + "$Cn1Factory";
    private static final String[] BUNDLE_BASES = {
        BUNDLE, Relocation.JDK_PACKAGE + "ListResourceBundle", Relocation.JDK_PACKAGE + "PropertyResourceBundle",
    };
    /// What follows the registry's name in the name of one of its parts.
    private static final String PART = "$Part";
    /// How many registrations one part holds.
    private static final int CHUNK = 1000;
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

        TreeMap<String, String> propertyBundles = new TreeMap<String, String>();
        for (String path : resources.keySet()) {
            if (path.endsWith(PROPERTIES_SUFFIX)) {
                String name = path.substring(0, path.length() - PROPERTIES_SUFFIX.length());
                propertyBundles.put(name.replace('/', '.'), "/" + path);
            }
        }
        List<String> bundleClasses = bundleClasses();
        writeRegistry(registry(new ArrayList<String>(resources.keySet()), propertyBundles, bundleClasses));
        log.debug("Shipped " + resources.size() + " desktop resources, " + propertyBundles.size()
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
            if (resources.containsKey(flat)) {
                throw collision(path, flat, resources.get(flat).toString());
            }
            String other = flatToPath.put(flat, path);
            if (other != null) {
                throw collision(path, flat, "the resource " + other);
            }
            File nested = new File(classesDir, path);
            // The build tool's copy, when there is one: it may have been
            // filtered, and it is the bytes the developer's build produced.
            byte[] data = Files.readAllBytes((nested.isFile() ? nested : e.getValue()).toPath());
            File dest = new File(classesDir, flat);
            if (dest.exists() && !previous.contains(flat)
                    && !(dest.isFile() && Arrays.equals(Files.readAllBytes(dest.toPath()), data))) {
                throw collision(path, flat, dest.toString());
            }
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
    private List<String> bundleClasses() throws IOException {
        final Map<String, Seen> seen = new TreeMap<String, Seen>();
        List<File> dirs = new ArrayList<File>();
        dirs.add(classesDir);
        dirs.addAll(classDirs);
        for (File dir : dirs) {
            scan(dir, "", seen);
        }
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Seen> e : seen.entrySet()) {
            if (e.getValue().creatable && isBundle(e.getValue().superName, seen)) {
                out.add(e.getKey().replace('/', '.'));
            }
        }
        return out;
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

    /// The registry: the class itself and its parts, by internal name.
    /// `resources` are the shipped paths, without their leading slash;
    /// `propertyBundles` maps a bundle's dotted name to the path of its file;
    /// `bundleClasses` are dotted class names.
    ///
    /// The registrations are dealt out to part classes, each with one
    /// static `install` method, because a method's code is limited to 64KB
    /// and an application has as many resources as it has. They are classes
    /// of their own rather than further methods of the registry because the
    /// compliance check knows the registry by the placeholder in the shared
    /// JDK classes' jar: a method the placeholder does not declare would be
    /// reported as one that does not exist.
    static Map<String, byte[]> registry(List<String> resources, Map<String, String> propertyBundles,
                                        List<String> bundleClasses) {
        Map<String, byte[]> out = new TreeMap<String, byte[]>();
        List<Map.Entry<String, String>> files = new ArrayList<Map.Entry<String, String>>(propertyBundles.entrySet());
        int total = resources.size() + files.size() + bundleClasses.size();
        List<String> parts = new ArrayList<String>();
        for (int from = 0; from < total; from += CHUNK) {
            String part = REGISTRY + PART + parts.size();
            parts.add(part);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cw.visit(Opcodes.V1_5, Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, part, null, "java/lang/Object", null);
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "install", "()V", null, null);
            mv.visitCode();
            boolean hasFactory = false;
            for (int i = from; i < total && i < from + CHUNK; i++) {
                if (i < resources.size()) {
                    mv.visitLdcInsn(resources.get(i));
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, RESOURCES, "cn1AddResource", "(Ljava/lang/String;)V",
                            false);
                } else if (i < resources.size() + files.size()) {
                    Map.Entry<String, String> file = files.get(i - resources.size());
                    mv.visitLdcInsn(file.getKey());
                    mv.visitLdcInsn(file.getValue());
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, BUNDLE, "cn1RegisterProperties",
                            "(Ljava/lang/String;Ljava/lang/String;)V", false);
                } else {
                    if (!hasFactory) {
                        mv.visitTypeInsn(Opcodes.NEW, REGISTRY);
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, REGISTRY, "<init>", "()V", false);
                        mv.visitVarInsn(Opcodes.ASTORE, 0);
                        hasFactory = true;
                    }
                    int id = i - resources.size() - files.size();
                    mv.visitLdcInsn(bundleClasses.get(id));
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitLdcInsn(Integer.valueOf(id));
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, BUNDLE, "cn1RegisterBundleClass",
                            "(Ljava/lang/String;L" + FACTORY + ";I)V", false);
                }
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
            out.put(part, cw.toByteArray());
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, REGISTRY, null,
                "java/lang/Object", new String[] {FACTORY});

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor install = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "cn1Install", "()V", null,
                null);
        install.visitCode();
        install.visitMethodInsn(Opcodes.INVOKESTATIC, RESOURCES, "cn1BeginIndex", "()V", false);
        for (String part : parts) {
            install.visitMethodInsn(Opcodes.INVOKESTATIC, part, "install", "()V", false);
        }
        install.visitMethodInsn(Opcodes.INVOKESTATIC, BUNDLE, "cn1Seal", "()V", false);
        install.visitInsn(Opcodes.RETURN);
        install.visitMaxs(0, 0);
        install.visitEnd();

        MethodVisitor create = cw.visitMethod(Opcodes.ACC_PUBLIC, "cn1Create", "(I)L" + BUNDLE + ";", null, null);
        create.visitCode();
        Label none = new Label();
        if (!bundleClasses.isEmpty()) {
            Label[] cases = new Label[bundleClasses.size()];
            for (int i = 0; i < cases.length; i++) {
                cases[i] = new Label();
            }
            create.visitVarInsn(Opcodes.ILOAD, 1);
            create.visitTableSwitchInsn(0, cases.length - 1, none, cases);
            for (int i = 0; i < cases.length; i++) {
                String type = bundleClasses.get(i).replace('.', '/');
                create.visitLabel(cases[i]);
                create.visitTypeInsn(Opcodes.NEW, type);
                create.visitInsn(Opcodes.DUP);
                create.visitMethodInsn(Opcodes.INVOKESPECIAL, type, "<init>", "()V", false);
                create.visitInsn(Opcodes.ARETURN);
            }
        }
        create.visitLabel(none);
        create.visitInsn(Opcodes.ACONST_NULL);
        create.visitInsn(Opcodes.ARETURN);
        create.visitMaxs(0, 0);
        create.visitEnd();

        cw.visitEnd();
        out.put(REGISTRY, cw.toByteArray());
        return out;
    }

    /// Writes the registry's classes and removes the parts an earlier,
    /// larger registry left behind.
    private void writeRegistry(Map<String, byte[]> classes) throws IOException {
        File dir = new File(classesDir, REGISTRY + ".class").getParentFile();
        String prefix = REGISTRY.substring(REGISTRY.lastIndexOf('/') + 1) + PART;
        File[] existing = dir.listFiles();
        if (existing != null) {
            for (File f : existing) {
                String name = f.getName();
                if (name.startsWith(prefix) && name.endsWith(".class")
                        && !classes.containsKey(Relocation.JDK_PACKAGE + name.substring(0, name.length() - 6))
                        && !f.delete()) {
                    log.warn("Could not delete " + f);
                }
            }
        }
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            ClassRelocator.writeIfDifferent(new File(classesDir, e.getKey() + ".class"), e.getValue());
        }
    }
}
