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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Rewrites compiled classes by the rules of one or more [Relocation]s.
///
/// Several layers can be active in one application -- Android sources beside
/// Swing ones -- and they are applied as ONE pass with the union of their
/// rules. Running a pass per layer would have each walk the other's relocated
/// runtime, and two layers redirect the same JDK classes.
public final class ClassRelocator {

    private final List<Relocation> relocations;
    /// Whether a desktop layer is among them, which is what switches on the
    /// member rewrites of [CompatRewrites].
    private final boolean desktop;
    /// Entries of a runtime jar that are not copied, because the build
    /// generates the class of that name itself.
    private final Set<String> generated = new LinkedHashSet<String>();
    private final Remapper remapper = new Remapper() {
        @Override
        public String map(String internalName) {
            return ClassRelocator.this.map(internalName);
        }
    };

    public ClassRelocator(List<Relocation> relocations) {
        this.relocations = Collections.unmodifiableList(new ArrayList<Relocation>(relocations));
        boolean anyDesktop = false;
        for (Relocation r : this.relocations) {
            anyDesktop = anyDesktop || r.isDesktop();
        }
        this.desktop = anyDesktop;
    }

    /// Whether one of the layers is a desktop one (Swing, JavaFX).
    public boolean hasDesktopLayer() {
        return desktop;
    }

    /// Declares that the build generates the class `internalName` itself, so
    /// [#extractRuntime] leaves the placeholder a runtime jar carries under
    /// that name out. Without this the extraction and the generator would
    /// each overwrite the other's file on every run.
    ClassRelocator generating(String internalName) {
        generated.add(internalName + ".class");
        return this;
    }

    public ClassRelocator(Relocation... relocations) {
        this(Arrays.asList(relocations));
    }

    public List<Relocation> relocations() {
        return relocations;
    }

    /// The relocated internal name, or `name` unchanged.
    public String map(String name) {
        if (name == null) {
            return null;
        }
        for (Relocation r : relocations) {
            String shim = r.shim(name);
            if (shim != null) {
                return shim;
            }
        }
        for (Relocation r : relocations) {
            String moved = r.relocate(name);
            if (moved != null) {
                return moved;
            }
        }
        return name;
    }

    /// The name the application was compiled against, or `relocated`
    /// unchanged when no layer produced it.
    public String original(String relocated) {
        for (Relocation r : relocations) {
            String o = r.original(relocated);
            if (o != null) {
                return o;
            }
        }
        return relocated;
    }

    /// The layer whose runtime `jar` is, or null.
    public Relocation layerOf(File jar) {
        for (Relocation r : relocations) {
            if (r.isRuntimeJar(jar.getName())) {
                return r;
            }
        }
        return null;
    }

    public Remapper remapper() {
        return remapper;
    }

    public byte[] remap(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(0);
        ClassVisitor chain = new ClassRemapper(new PostRemapFixes(cw), remapper);
        if (desktop) {
            // Ahead of the relocation: the rules name what an application
            // is compiled against.
            chain = CompatRewrites.visitor(new MainRenamer(chain));
        }
        // First of all, so that what it generates is rewritten and relocated
        // like the code javac wrote.
        cr.accept(new RecordDesugar(chain), 0);
        return cw.toByteArray();
    }

    /// The name a desktop application's `main(String[])` ships under.
    ///
    /// Every desktop application declares a `main`, and often several: one per
    /// launcher, one in a demo class of a library. ParparVM takes the class
    /// holding a static `main` with one array argument to be THE entry point
    /// of the program and refuses a second one outright
    /// (`Multiple main classes: ...Stub and ...`), so an imported application
    /// failed its translation for iOS, macOS, Windows and Linux before a line
    /// of it was translated; only the JavaScript target, which names its main
    /// class to the translator, built. Nothing on a device calls `main` by
    /// that name -- the generated lifecycle ([DesktopEntryPoints]) is what
    /// starts the application -- so the method is renamed here, together with
    /// every call of it, and the name ParparVM reserves stays the stub's.
    static final String DESKTOP_MAIN = "cn1DesktopMain";

    /// The descriptor of a `main(String[])`.
    static final String MAIN_DESCRIPTOR = "([Ljava/lang/String;)V";

    /// Whether `name` and `descriptor` are those of a `main(String[])`,
    /// under the name it was compiled with or the one it ships under.
    static boolean isMain(String name, String descriptor) {
        return MAIN_DESCRIPTOR.equals(descriptor) && ("main".equals(name) || DESKTOP_MAIN.equals(name));
    }

    /// Renames every static `main(String[])` to [#DESKTOP_MAIN], and every
    /// static call or method reference that names one. Applied to each class
    /// of a build with a desktop layer, so a declaration and its callers move
    /// together; running it again over its own output changes nothing.
    private static final class MainRenamer extends ClassVisitor {

        MainRenamer(ClassVisitor next) {
            super(Opcodes.ASM9, next);
        }

        private static String rename(String name, String descriptor) {
            return "main".equals(name) && MAIN_DESCRIPTOR.equals(descriptor) ? DESKTOP_MAIN : name;
        }

        private static Object rename(Object argument) {
            if (argument instanceof Handle) {
                Handle h = (Handle) argument;
                if (h.getTag() == Opcodes.H_INVOKESTATIC) {
                    return new Handle(h.getTag(), h.getOwner(), rename(h.getName(), h.getDesc()), h.getDesc(),
                            h.isInterface());
                }
            }
            return argument;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            String shipped = (access & Opcodes.ACC_STATIC) != 0 ? rename(name, descriptor) : name;
            MethodVisitor mv = super.visitMethod(access, shipped, descriptor, signature, exceptions);
            if (mv == null) {
                return null;
            }
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String n, String d, boolean isInterface) {
                    super.visitMethodInsn(opcode, owner, opcode == Opcodes.INVOKESTATIC ? rename(n, d) : n, d,
                            isInterface);
                }

                @Override
                public void visitInvokeDynamicInsn(String n, String d, Handle bootstrap, Object... arguments) {
                    Object[] mapped = new Object[arguments.length];
                    for (int i = 0; i < arguments.length; i++) {
                        mapped[i] = rename(arguments[i]);
                    }
                    super.visitInvokeDynamicInsn(n, d, bootstrap, mapped);
                }
            };
        }
    }

    /// What the remapper itself does not cover: two source interfaces can map
    /// to one target (`Closeable` and `AutoCloseable` both become
    /// `AutoCloseable`), and a class file that names an interface twice is
    /// rejected by the JVM; and Kotlin's metadata strings (below).
    private final class PostRemapFixes extends ClassVisitor {
        private final Set<String> methods = new LinkedHashSet<String>();
        private String className;

        PostRemapFixes(ClassVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            className = name;
            String[] distinct = interfaces;
            if (interfaces != null && interfaces.length > 1) {
                Set<String> seen = new LinkedHashSet<String>(Arrays.asList(interfaces));
                if (seen.size() != interfaces.length) {
                    distinct = seen.toArray(new String[seen.size()]);
                }
            }
            super.visit(version, access, name, signature, superName, distinct);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (!methods.add(name + descriptor)) {
                // The wording predates the other layers and is what an
                // Android build has always printed; Closeable is the only
                // rule in any layer that maps two names onto one.
                throw new IllegalArgumentException("Android remapping collapses overloads in "
                        + className + ": " + name + descriptor
                        + ". Rename the overload or use one AutoCloseable signature.");
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        /// Kotlin records the JVM descriptors of a class's members as plain
        /// strings in `@kotlin.Metadata`'s `d2` array. The remapper never sees
        /// those, so without this they would keep naming android/... types.
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
            if (av == null || !"Lkotlin/Metadata;".equals(descriptor)) {
                return av;
            }
            return new AnnotationVisitor(Opcodes.ASM9, av) {
                @Override
                public AnnotationVisitor visitArray(String name) {
                    AnnotationVisitor array = super.visitArray(name);
                    if (array == null || !"d2".equals(name)) {
                        return array;
                    }
                    return new AnnotationVisitor(Opcodes.ASM9, array) {
                        @Override
                        public void visit(String n, Object value) {
                            super.visit(n, value instanceof String ? mapMetadataString((String) value) : value);
                        }
                    };
                }
            };
        }
    }

    /// A `kotlin.Metadata` string: a method or field descriptor, or a name
    /// that is left alone.
    String mapMetadataString(String s) {
        if (s.length() == 0) {
            return s;
        }
        try {
            if (s.charAt(0) == '(') {
                return remapper.mapMethodDesc(s);
            }
            if ((s.charAt(0) == 'L' && s.endsWith(";")) || s.charAt(0) == '[') {
                return remapper.mapDesc(s);
            }
        } catch (RuntimeException e) {
            // Not a descriptor after all (a name that happens to start with
            // L or a bracket): keep it as written.
            return s;
        }
        return s;
    }

    /// Relocates every class under `classesDir` in place, adding each one's
    /// final internal name to `out`. Directories holding a runtime this
    /// relocator already extracted are not application code and are skipped.
    public void remapDirectory(File classesDir, List<String> out, Log log) throws IOException {
        remapDirectory(classesDir, classesDir, out, log);
    }

    private void remapDirectory(File classesDir, File dir, List<String> out, Log log) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                if (isShipped(relative(classesDir, f) + "/")) {
                    continue;
                }
                remapDirectory(classesDir, f, out, log);
            } else if (f.getName().endsWith(".class")) {
                byte[] in = Files.readAllBytes(f.toPath());
                byte[] remapped = remap(in);
                String name = relative(classesDir, f);
                name = name.substring(0, name.length() - ".class".length());
                String mapped = map(name);
                if (!mapped.equals(name)) {
                    // Application code inside a relocated package (a support
                    // shim the app carries itself) moves with the rest.
                    File dest = new File(classesDir, mapped + ".class");
                    dest.getParentFile().mkdirs();
                    write(dest, remapped);
                    if (!f.delete()) {
                        log.warn("Could not delete " + f);
                    }
                    out.add(mapped);
                } else {
                    if (!Arrays.equals(in, remapped)) {
                        write(f, remapped);
                    }
                    out.add(name);
                }
            }
        }
    }

    /// Whether `rel` (a directory, with its trailing slash) is where some
    /// active layer's runtime, or the shared JDK classes, were extracted.
    private boolean isShipped(String rel) {
        if (rel.startsWith(Relocation.JDK_PACKAGE)) {
            return true;
        }
        for (Relocation r : relocations) {
            if (rel.startsWith(r.target())) {
                return true;
            }
        }
        return false;
    }

    private static String relative(File classesDir, File f) {
        String base = classesDir.getAbsolutePath();
        String p = f.getAbsolutePath();
        return p.substring(base.length() + 1).replace(File.separatorChar, '/');
    }

    /// `dest` when it lies inside the classes directory. An entry name with
    /// `..` (or an absolute one) would write elsewhere; a runtime jar never
    /// has one, so meeting one means the artifact is not what it should be.
    private static File inside(File classesDir, File jar, File dest, String entry) throws IOException {
        String root = classesDir.getCanonicalPath() + File.separator;
        if (!dest.getCanonicalPath().startsWith(root)) {
            throw new IOException(jar + " entry " + entry + " resolves outside " + classesDir);
        }
        return dest;
    }

    /// Copies a runtime jar's classes into `classesDir`, relocated, and its
    /// other entries as they are. Answers the number of classes.
    public int extractRuntime(File jar, File classesDir) throws IOException {
        int count = 0;
        ZipFile zip = new ZipFile(jar);
        try {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.startsWith("META-INF/") || generated.contains(name)) {
                    continue;
                }
                byte[] data = read(zip.getInputStream(e));
                if (name.endsWith(".class")) {
                    String cls = name.substring(0, name.length() - ".class".length());
                    File dest = inside(classesDir, jar, new File(classesDir, map(cls) + ".class"), name);
                    writeIfDifferent(dest, remap(data));
                    count++;
                } else {
                    // Resource tables and assets are compared byte for byte
                    // too: a rebuilt table routinely keeps its length while
                    // its contents change, and a length check alone kept the
                    // previous runtime's copy.
                    writeIfDifferent(inside(classesDir, jar, new File(classesDir, name), name), data);
                }
            }
        } finally {
            zip.close();
        }
        return count;
    }

    /// Writes `data` to `dest` unless the file already holds exactly those
    /// bytes, so an incremental build leaves unchanged outputs (and their
    /// timestamps) alone.
    static void writeIfDifferent(File dest, byte[] data) throws IOException {
        if (dest.isFile() && dest.length() == data.length && Arrays.equals(Files.readAllBytes(dest.toPath()), data)) {
            return;
        }
        dest.getParentFile().mkdirs();
        write(dest, data);
    }

    static void collectClassFiles(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectClassFiles(f, out);
            } else if (f.getName().endsWith(".class")) {
                out.add(f);
            }
        }
    }

    static byte[] read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    static void write(File f, byte[] data) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
