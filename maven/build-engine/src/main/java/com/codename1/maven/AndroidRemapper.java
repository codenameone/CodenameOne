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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Relocates an application's compiled Android code onto the compatibility
/// runtime, and ships the runtime with it.
///
/// The application is compiled against `android.*` -- that is what keeps its
/// sources unchanged -- but nothing may ship under those names: on an Android
/// build they would collide with the real framework. This step rewrites every
/// class in the output directory so `android/`, `androidx/`,
/// `com/google/android/material/` and `org/xmlpull/v1/` live under
/// [#TARGET], copies the runtime jar's classes in, relocated the same way,
/// and generates the `android:onClick` dispatcher from the methods the
/// compiled classes actually declare.
public final class AndroidRemapper {

    public static final String TARGET = "com/codename1/androidcompat/";
    static final String[] PREFIXES = {"android/", "androidx/", "com/google/android/material/", "org/xmlpull/v1/"};
    /// The runtime's own bridge package is relocated too, so the copy that
    /// ships never shares a name with the jar's unrelocated original, which
    /// stays on the compile (and simulator) classpath.
    public static final String RUNTIME_PACKAGE = "com/codename1/androidcompat/runtime/";
    public static final String RUNTIME_TARGET = "com/codename1/androidcompat/rt/";
    public static final String ON_CLICK_DISPATCH = RUNTIME_TARGET + "OnClickDispatch";
    static final String VIEW = TARGET + "android/view/View";
    public static final String FRAGMENT_FACTORY = RUNTIME_TARGET + "FragmentFactory";
    /// The generated replacement for the runtime's `JsInterfaceDispatch`
    /// placeholder, and the conversions it calls.
    public static final String JS_INTERFACE_DISPATCH = RUNTIME_TARGET + "JsInterfaceDispatch";
    static final String JS_BRIDGE = RUNTIME_TARGET + "JsBridge";
    /// `@JavascriptInterface`, as compiled and as relocated.
    static final String[] JS_INTERFACE_ANNOTATIONS = {
        "Landroid/webkit/JavascriptInterface;", "L" + TARGET + "android/webkit/JavascriptInterface;"};
    /// The fragment base classes, relocated: the platform's and AndroidX's.
    static final String[] FRAGMENT_BASES = {TARGET + "android/app/Fragment", TARGET + "androidx/fragment/app/Fragment"};
    /// The generated replacement for the runtime's `ViewModelFactory`
    /// placeholder, the view model base it looks for, and the constructor
    /// argument types it passes.
    public static final String VIEW_MODEL_FACTORY = RUNTIME_TARGET + "ViewModelFactory";
    static final String VIEW_MODEL = TARGET + "androidx/lifecycle/ViewModel";
    static final String APPLICATION = TARGET + "android/app/Application";
    static final String SAVED_STATE_HANDLE = TARGET + "androidx/lifecycle/SavedStateHandle";
    /// Constructors in the order AndroidX's `SavedStateViewModelFactory`
    /// prefers them.
    static final String[] VIEW_MODEL_CTORS = {
        "(L" + APPLICATION + ";L" + SAVED_STATE_HANDLE + ";)V", "(L" + SAVED_STATE_HANDLE + ";)V",
        "(L" + APPLICATION + ";)V", "()V"};

    private final File classesDir;
    private final File compatJar;
    private final File onClickNames;
    private final Log log;
    private boolean shipRuntime = true;
    private final List<File> handlerDirs = new ArrayList<File>();

    public AndroidRemapper(File classesDir, File compatJar, File onClickNames, Log log) {
        this.classesDir = classesDir;
        this.compatJar = compatJar;
        this.onClickNames = onClickNames;
        this.log = log;
    }

    /// Only relocates the classes, without copying in the runtime or writing
    /// the onClick dispatcher: for a second output directory of the same
    /// application (Gradle's Kotlin classes), which the main pass covers.
    public AndroidRemapper relocateOnly() {
        shipRuntime = false;
        return this;
    }

    /// Further class directories whose methods `android:onClick` may name.
    public AndroidRemapper withHandlerDirectories(List<File> dirs) {
        if (dirs != null) {
            for (File d : dirs) {
                if (d != null && d.isDirectory() && !d.equals(classesDir)) {
                    handlerDirs.add(d);
                }
            }
        }
        return this;
    }

    /// JDK classes Android code uses that the Codename One runtime lacks,
    /// redirected to the compatibility runtime's implementations.
    static final String[][] JDK_SHIMS = {
        {"java/io/BufferedReader", "com/codename1/androidcompat/jdk/BufferedReader"},
        {"java/io/BufferedWriter", "com/codename1/androidcompat/jdk/BufferedWriter"},
        {"java/io/BufferedInputStream", "com/codename1/androidcompat/jdk/BufferedInputStream"},
        {"java/io/BufferedOutputStream", "com/codename1/androidcompat/jdk/BufferedOutputStream"},
        {"java/io/File", "com/codename1/androidcompat/jdk/File"},
        {"java/io/FileFilter", "com/codename1/androidcompat/jdk/FileFilter"},
        {"java/io/FilenameFilter", "com/codename1/androidcompat/jdk/FilenameFilter"},
        {"java/io/FileInputStream", "com/codename1/androidcompat/jdk/FileInputStream"},
        {"java/io/FileOutputStream", "com/codename1/androidcompat/jdk/FileOutputStream"},
        {"java/io/FileReader", "com/codename1/androidcompat/jdk/FileReader"},
        {"java/io/FileWriter", "com/codename1/androidcompat/jdk/FileWriter"},
        {"java/io/FilterInputStream", "com/codename1/androidcompat/jdk/FilterInputStream"},
        {"java/io/FilterOutputStream", "com/codename1/androidcompat/jdk/FilterOutputStream"},
        {"java/io/PrintWriter", "com/codename1/androidcompat/jdk/PrintWriter"},
        // Every Codename One stream, reader and writer is AutoCloseable, and
        // close() is the interface's only method, so code holding a Closeable
        // runs unchanged against it.
        {"java/io/Closeable", "java/lang/AutoCloseable"},
    };

    /// The relocated internal name, or `name` unchanged.
    public static String map(String name) {
        if (name == null) {
            return null;
        }
        for (String[] shim : JDK_SHIMS) {
            if (shim[0].equals(name)) {
                return shim[1];
            }
        }
        if (name.startsWith(RUNTIME_PACKAGE)) {
            return RUNTIME_TARGET + name.substring(RUNTIME_PACKAGE.length());
        }
        for (String p : PREFIXES) {
            if (name.startsWith(p)) {
                return TARGET + name;
            }
        }
        return name;
    }

    static final Remapper REMAPPER = new Remapper() {
        @Override
        public String map(String internalName) {
            return AndroidRemapper.map(internalName);
        }
    };

    static byte[] remap(byte[] in) {
        ClassReader cr = new ClassReader(in);
        ClassWriter cw = new ClassWriter(0);
        cr.accept(new ClassRemapper(new PostRemapFixes(cw), REMAPPER), 0);
        return cw.toByteArray();
    }

    /// What the remapper itself does not cover: two source interfaces can map
    /// to one target (`Closeable` and `AutoCloseable` both become
    /// `AutoCloseable`), and a class file that names an interface twice is
    /// rejected by the JVM; and Kotlin's metadata strings (below).
    private static final class PostRemapFixes extends ClassVisitor {
        PostRemapFixes(ClassVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            String[] distinct = interfaces;
            if (interfaces != null && interfaces.length > 1) {
                Set<String> seen = new LinkedHashSet<String>(Arrays.asList(interfaces));
                if (seen.size() != interfaces.length) {
                    distinct = seen.toArray(new String[seen.size()]);
                }
            }
            super.visit(version, access, name, signature, superName, distinct);
        }

        /// Kotlin records the JVM descriptors of a class's members as plain
        /// strings in `@kotlin.Metadata`'s `d2` array. The remapper never sees
        /// those, so without this they would keep naming android/... types.
        @Override
        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            org.objectweb.asm.AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
            if (av == null || !"Lkotlin/Metadata;".equals(descriptor)) {
                return av;
            }
            return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9, av) {
                @Override
                public org.objectweb.asm.AnnotationVisitor visitArray(String name) {
                    org.objectweb.asm.AnnotationVisitor array = super.visitArray(name);
                    if (array == null || !"d2".equals(name)) {
                        return array;
                    }
                    return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9, array) {
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
    static String mapMetadataString(String s) {
        if (s.length() == 0) {
            return s;
        }
        try {
            if (s.charAt(0) == '(') {
                return REMAPPER.mapMethodDesc(s);
            }
            if ((s.charAt(0) == 'L' && s.endsWith(";")) || s.charAt(0) == '[') {
                return REMAPPER.mapDesc(s);
            }
        } catch (RuntimeException e) {
            // Not a descriptor after all (a name that happens to start with
            // L or a bracket): keep it as written.
            return s;
        }
        return s;
    }

    public void run() throws BuildException {
        try {
            List<String> appClasses = new ArrayList<String>();
            remapDirectory(classesDir, appClasses);
            if (!shipRuntime) {
                log.info("Relocated " + appClasses.size() + " application classes");
                return;
            }
            int runtime = extractRuntime();
            int handlers = writeOnClickDispatch(appClasses);
            int fragments = writeFragmentFactory();
            int viewModels = writeViewModelFactory();
            if (viewModels > 0) {
                log.info(viewModels + " view model class(es) creatable by ViewModelProvider");
            }
            int jsMethods = writeJsInterfaceDispatch(appClasses);
            if (jsMethods > 0) {
                log.info(jsMethods + " @JavascriptInterface method(s) callable from web views");
            }
            if (fragments > 0) {
                log.info(fragments + " fragment class(es) instantiable by name");
            }
            log.info("Relocated " + appClasses.size() + " application classes and " + runtime
                    + " Android runtime classes; " + handlers + " android:onClick handler(s)");
        } catch (IOException e) {
            throw new BuildException("Android remapping failed: " + e.getMessage(), e);
        }
    }

    private void remapDirectory(File dir, List<String> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                String rel = relative(f) + "/";
                // The relocated runtime is extracted, not application code.
                if (rel.equals(TARGET) || rel.startsWith(TARGET)) {
                    continue;
                }
                remapDirectory(f, out);
            } else if (f.getName().endsWith(".class")) {
                byte[] in = Files.readAllBytes(f.toPath());
                byte[] remapped = remap(in);
                String name = relative(f);
                name = name.substring(0, name.length() - ".class".length());
                String mapped = map(name);
                if (!mapped.equals(name)) {
                    // Application code inside android.* (a support shim
                    // the app carries itself) moves with the rest.
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

    private String relative(File f) {
        String base = classesDir.getAbsolutePath();
        String p = f.getAbsolutePath();
        return p.substring(base.length() + 1).replace(File.separatorChar, '/');
    }

    /// `dest` when it lies inside the classes directory. An entry name with
    /// `..` (or an absolute one) would write elsewhere; the runtime jar never
    /// has one, so meeting one means the artifact is not what it should be.
    private File inside(File dest, String entry) throws IOException {
        String root = classesDir.getCanonicalPath() + File.separator;
        if (!dest.getCanonicalPath().startsWith(root)) {
            throw new IOException(compatJar + " entry " + entry + " resolves outside " + classesDir);
        }
        return dest;
    }

    private int extractRuntime() throws IOException {
        int count = 0;
        ZipFile zip = new ZipFile(compatJar);
        try {
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory() || name.startsWith("META-INF/")) {
                    continue;
                }
                byte[] data = read(zip.getInputStream(e));
                if (name.endsWith(".class")) {
                    String cls = name.substring(0, name.length() - ".class".length());
                    File dest = inside(new File(classesDir, map(cls) + ".class"), name);
                    byte[] remapped = remap(data);
                    if (!dest.isFile() || dest.length() != remapped.length
                            || !Arrays.equals(Files.readAllBytes(dest.toPath()), remapped)) {
                        dest.getParentFile().mkdirs();
                        write(dest, remapped);
                    }
                    count++;
                } else {
                    File dest = inside(new File(classesDir, name), name);
                    if (!dest.isFile() || dest.length() != data.length) {
                        dest.getParentFile().mkdirs();
                        write(dest, data);
                    }
                }
            }
        } finally {
            zip.close();
        }
        return count;
    }

    /// Writes `OnClickDispatch.dispatch(Object, String, View)`: for each layout
    /// `android:onClick` name and each class declaring a public instance
    /// `name(View)` method, a type test and a direct call.
    private int writeOnClickDispatch(List<String> appClasses) throws IOException {
        Set<String> names = new LinkedHashSet<String>();
        if (onClickNames != null && onClickNames.isFile()) {
            for (String line : new String(Files.readAllBytes(onClickNames.toPath()), Charset.forName("UTF-8"))
                    .split("\n")) {
                if (line.trim().length() > 0) {
                    names.add(line.trim());
                }
            }
        }
        final List<String[]> handlers = new ArrayList<String[]>();
        if (!names.isEmpty()) {
            final String viewDesc = "(L" + VIEW + ";)V";
            List<File> classFiles = new ArrayList<File>();
            for (String cls : appClasses) {
                classFiles.add(new File(classesDir, cls + ".class"));
            }
            for (File dir : handlerDirs) {
                collectClassFiles(dir, classFiles);
            }
            for (final File f : classFiles) {
                final ClassReader cr = new ClassReader(Files.readAllBytes(f.toPath()));
                if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                    continue;
                }
                final String cls = map(cr.getClassName());
                final Set<String> wanted = names;
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        if ((access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_STATIC) == 0
                                && REMAPPER.mapMethodDesc(desc).equals(viewDesc) && wanted.contains(name)) {
                            // Mapped first: a handler directory's classes may not
                            // be relocated yet (Gradle relocates Kotlin's after javac).
                            handlers.add(new String[] {cls, name});
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE);
            }
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, ON_CLICK_DISPATCH, null,
                "java/lang/Object", null);
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "dispatch",
                "(Ljava/lang/Object;Ljava/lang/String;L" + VIEW + ";)Z", null, null);
        mv.visitCode();
        for (String[] h : handlers) {
            Label next = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(h[1]);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, h[0]);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.CHECKCAST, h[0]);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, h[0], h[1], "(L" + VIEW + ";)V", false);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(next);
        }
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        File out = new File(classesDir, ON_CLICK_DISPATCH + ".class");
        out.getParentFile().mkdirs();
        write(out, cw.toByteArray());
        for (String n : names) {
            boolean found = false;
            for (String[] h : handlers) {
                if (h[1].equals(n)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.warn("android:onClick=\"" + n + "\" names no public void " + n
                        + "(View) method in any compiled class; clicking will throw, as on Android");
            }
        }
        return handlers.size();
    }

    /// Writes `FragmentFactory.instantiate(String)`: for every public,
    /// concrete application class that extends a fragment base and has a
    /// public no-argument constructor, a name comparison and `new`. This is
    /// what `Fragment.instantiate` and `<fragment android:name>` resolve
    /// through, so fragments are created by name without reflection. The
    /// superclass chain is followed through every class in the output,
    /// including the runtime's, so an application fragment extending
    /// `ListFragment` or another application fragment is found too.
    private int writeFragmentFactory() throws IOException {
        final Map<String, String> supers = new HashMap<String, String>();
        List<File> all = new ArrayList<File>();
        collectClassFiles(classesDir, all);
        for (File dir : handlerDirs) {
            collectClassFiles(dir, all);
        }
        final Map<String, Boolean> candidates = new HashMap<String, Boolean>();
        for (File f : all) {
            ClassReader cr = new ClassReader(Files.readAllBytes(f.toPath()));
            final String cls = map(cr.getClassName());
            if (cr.getSuperName() != null) {
                supers.put(cls, map(cr.getSuperName()));
            }
            int access = cr.getAccess();
            if ((access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) != 0 || (access & Opcodes.ACC_PUBLIC) == 0
                    || cls.startsWith(TARGET)) {
                continue;
            }
            final boolean[] ctor = new boolean[1];
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                    if (name.equals("<init>") && desc.equals("()V") && (acc & Opcodes.ACC_PUBLIC) != 0) {
                        ctor[0] = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE);
            if (ctor[0]) {
                candidates.put(cls, Boolean.TRUE);
            }
        }
        List<String> fragments = new ArrayList<String>();
        for (String cls : candidates.keySet()) {
            if (extendsFragment(cls, supers)) {
                fragments.add(cls);
            }
        }
        java.util.Collections.sort(fragments);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, FRAGMENT_FACTORY, null,
                "java/lang/Object", null);
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "instantiate",
                "(Ljava/lang/String;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        for (String cls : fragments) {
            Label next = new Label();
            mv.visitLdcInsn(cls.replace('/', '.'));
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitTypeInsn(Opcodes.NEW, cls);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, cls, "<init>", "()V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(next);
        }
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        File out = new File(classesDir, FRAGMENT_FACTORY + ".class");
        out.getParentFile().mkdirs();
        write(out, cw.toByteArray());
        return fragments.size();
    }

    /// Writes `ViewModelFactory.create(String, Object, Object)`: for every
    /// public, concrete application class extending `ViewModel`, a name
    /// comparison and `new` through the constructor AndroidX's default
    /// factory would choose -- what `ViewModelProvider.get(Class)` does by
    /// reflection on Android. A constructor whose `Application` or
    /// `SavedStateHandle` the calling factory did not supply answers null,
    /// and the runtime reports it as AndroidX does.
    private int writeViewModelFactory() throws IOException {
        final Map<String, String> supers = new HashMap<String, String>();
        List<File> all = new ArrayList<File>();
        collectClassFiles(classesDir, all);
        for (File dir : handlerDirs) {
            collectClassFiles(dir, all);
        }
        final Map<String, String> ctorOf = new HashMap<String, String>();
        for (File f : all) {
            ClassReader cr = new ClassReader(Files.readAllBytes(f.toPath()));
            final String cls = map(cr.getClassName());
            if (cr.getSuperName() != null) {
                supers.put(cls, map(cr.getSuperName()));
            }
            int access = cr.getAccess();
            if ((access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) != 0 || (access & Opcodes.ACC_PUBLIC) == 0
                    || cls.startsWith(TARGET)) {
                continue;
            }
            final Set<String> ctors = new LinkedHashSet<String>();
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                    if (name.equals("<init>") && (acc & Opcodes.ACC_PUBLIC) != 0) {
                        ctors.add(REMAPPER.mapMethodDesc(desc));
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE);
            for (String preferred : VIEW_MODEL_CTORS) {
                if (ctors.contains(preferred)) {
                    ctorOf.put(cls, preferred);
                    break;
                }
            }
        }
        List<String> models = new ArrayList<String>();
        for (String cls : ctorOf.keySet()) {
            if (extendsBase(cls, supers, VIEW_MODEL)) {
                models.add(cls);
            }
        }
        java.util.Collections.sort(models);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, VIEW_MODEL_FACTORY, null,
                "java/lang/Object", null);
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "create",
                "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        Label none = new Label();
        for (String cls : models) {
            String desc = ctorOf.get(cls);
            boolean app = desc.contains(APPLICATION);
            boolean handle = desc.contains(SAVED_STATE_HANDLE);
            Label next = new Label();
            mv.visitLdcInsn(cls.replace('/', '.'));
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            if (app) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitJumpInsn(Opcodes.IFNULL, none);
            }
            if (handle) {
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitJumpInsn(Opcodes.IFNULL, none);
            }
            mv.visitTypeInsn(Opcodes.NEW, cls);
            mv.visitInsn(Opcodes.DUP);
            if (app) {
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, APPLICATION);
            }
            if (handle) {
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitTypeInsn(Opcodes.CHECKCAST, SAVED_STATE_HANDLE);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, cls, "<init>", desc, false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(next);
        }
        mv.visitLabel(none);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        File out = new File(classesDir, VIEW_MODEL_FACTORY + ".class");
        out.getParentFile().mkdirs();
        write(out, cw.toByteArray());
        return models.size();
    }

    private static boolean extendsBase(String cls, Map<String, String> supers, String base) {
        String c = supers.get(cls);
        for (int depth = 0; c != null && depth < 64; depth++) {
            if (base.equals(c)) {
                return true;
            }
            c = supers.get(c);
        }
        return false;
    }

    private static boolean extendsFragment(String cls, Map<String, String> supers) {
        String c = supers.get(cls);
        for (int depth = 0; c != null && depth < 64; depth++) {
            for (String base : FRAGMENT_BASES) {
                if (base.equals(c)) {
                    return true;
                }
            }
            c = supers.get(c);
        }
        return false;
    }

    /// Writes `JsInterfaceDispatch`: `methods(Object)` lists the
    /// `@JavascriptInterface` methods an object has, and
    /// `invoke(Object, String, String[])` calls one by name and argument
    /// count -- the lookup Android's bridge does by reflection -- converting
    /// each JavaScript argument to its parameter type through `JsBridge` and
    /// returning the result as JavaScript source. Methods whose parameters
    /// are not strings or primitives cannot be called from JavaScript on
    /// Android either, and are skipped with a warning.
    private int writeJsInterfaceDispatch(List<String> appClasses) throws IOException {
        List<File> classFiles = new ArrayList<File>();
        for (String cls : appClasses) {
            classFiles.add(new File(classesDir, cls + ".class"));
        }
        for (File dir : handlerDirs) {
            collectClassFiles(dir, classFiles);
        }
        final List<String[]> methods = new ArrayList<String[]>();
        for (File f : classFiles) {
            if (!f.isFile()) {
                continue;
            }
            ClassReader cr = new ClassReader(Files.readAllBytes(f.toPath()));
            if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                continue;
            }
            final String cls = map(cr.getClassName());
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(final int access, final String name, final String desc, String sig,
                                                 String[] ex) {
                    if ((access & Opcodes.ACC_PUBLIC) == 0 || (access & Opcodes.ACC_STATIC) != 0
                            || name.startsWith("<")) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM9) {
                        @Override
                        public org.objectweb.asm.AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                            for (String a : JS_INTERFACE_ANNOTATIONS) {
                                if (a.equals(annotation)) {
                                    String mapped = REMAPPER.mapMethodDesc(desc);
                                    if (jsCallable(mapped)) {
                                        methods.add(new String[] {cls, name, mapped});
                                    } else {
                                        log.warn("@JavascriptInterface " + cls.replace('/', '.') + "." + name
                                                + " takes a parameter JavaScript cannot pass (only strings and"
                                                + " primitives); it is not exposed");
                                    }
                                }
                            }
                            return null;
                        }
                    };
                }
            }, ClassReader.SKIP_CODE);
        }
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, JS_INTERFACE_DISPATCH,
                null, "java/lang/Object", null);
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        // methods(Object): a StringBuilder of "name," per class the object is.
        Map<String, StringBuilder> namesByClass = new java.util.LinkedHashMap<String, StringBuilder>();
        for (String[] m : methods) {
            StringBuilder sb = namesByClass.get(m[0]);
            if (sb == null) {
                sb = new StringBuilder();
                namesByClass.put(m[0], sb);
            }
            sb.append(m[1]).append(',');
        }
        MethodVisitor names = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "methods",
                "(Ljava/lang/Object;)Ljava/lang/String;", null, null);
        names.visitCode();
        names.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        names.visitInsn(Opcodes.DUP);
        names.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        names.visitVarInsn(Opcodes.ASTORE, 1);
        for (Map.Entry<String, StringBuilder> e : namesByClass.entrySet()) {
            Label next = new Label();
            names.visitVarInsn(Opcodes.ALOAD, 0);
            names.visitTypeInsn(Opcodes.INSTANCEOF, e.getKey());
            names.visitJumpInsn(Opcodes.IFEQ, next);
            names.visitVarInsn(Opcodes.ALOAD, 1);
            names.visitLdcInsn(e.getValue().toString());
            names.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            names.visitInsn(Opcodes.POP);
            names.visitLabel(next);
        }
        names.visitVarInsn(Opcodes.ALOAD, 1);
        names.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;",
                false);
        names.visitInsn(Opcodes.ARETURN);
        names.visitMaxs(0, 0);
        names.visitEnd();

        // invoke(Object, String, String[]): name, arity and type tests, then a direct call.
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "invoke",
                "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        for (String[] m : methods) {
            org.objectweb.asm.Type[] params = org.objectweb.asm.Type.getArgumentTypes(m[2]);
            org.objectweb.asm.Type ret = org.objectweb.asm.Type.getReturnType(m[2]);
            Label next = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(m[1]);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitInsn(Opcodes.ARRAYLENGTH);
            mv.visitLdcInsn(Integer.valueOf(params.length));
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, next);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, m[0]);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.CHECKCAST, m[0]);
            for (int i = 0; i < params.length; i++) {
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitLdcInsn(Integer.valueOf(i));
                mv.visitInsn(Opcodes.AALOAD);
                String conv = jsConversion(params[i]);
                if (conv != null) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_BRIDGE, conv,
                            "(Ljava/lang/String;)" + params[i].getDescriptor(), false);
                }
            }
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, m[0], m[1], m[2], false);
            if (ret.getSort() == org.objectweb.asm.Type.VOID) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_BRIDGE, "undefined", "()Ljava/lang/String;", false);
            } else {
                String arg;
                switch (ret.getSort()) {
                    case org.objectweb.asm.Type.BOOLEAN:
                        arg = "Z";
                        break;
                    case org.objectweb.asm.Type.CHAR:
                        arg = "C";
                        break;
                    case org.objectweb.asm.Type.BYTE:
                    case org.objectweb.asm.Type.SHORT:
                    case org.objectweb.asm.Type.INT:
                        arg = "I";
                        break;
                    case org.objectweb.asm.Type.LONG:
                        arg = "J";
                        break;
                    case org.objectweb.asm.Type.FLOAT:
                        arg = "F";
                        break;
                    case org.objectweb.asm.Type.DOUBLE:
                        arg = "D";
                        break;
                    default:
                        arg = "Ljava/lang/Object;";
                        break;
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, JS_BRIDGE, "literal", "(" + arg + ")Ljava/lang/String;",
                        false);
            }
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(next);
        }
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        File out = new File(classesDir, JS_INTERFACE_DISPATCH + ".class");
        out.getParentFile().mkdirs();
        write(out, cw.toByteArray());
        return methods.size();
    }

    /// Whether every parameter of `desc` is one JavaScript can pass.
    static boolean jsCallable(String desc) {
        for (org.objectweb.asm.Type t : org.objectweb.asm.Type.getArgumentTypes(desc)) {
            if (t.getSort() == org.objectweb.asm.Type.OBJECT) {
                String n = t.getInternalName();
                if (!n.equals("java/lang/String") && !n.equals("java/lang/Object")
                        && !n.equals("java/lang/CharSequence")) {
                    return false;
                }
            } else if (t.getSort() == org.objectweb.asm.Type.ARRAY || t.getSort() == org.objectweb.asm.Type.VOID) {
                return false;
            }
        }
        return true;
    }

    /// The `JsBridge` method converting a string to `t`, or null for a
    /// string-typed parameter that takes the string itself.
    private static String jsConversion(org.objectweb.asm.Type t) {
        switch (t.getSort()) {
            case org.objectweb.asm.Type.INT:
                return "toInt";
            case org.objectweb.asm.Type.LONG:
                return "toLong";
            case org.objectweb.asm.Type.SHORT:
                return "toShort";
            case org.objectweb.asm.Type.BYTE:
                return "toByte";
            case org.objectweb.asm.Type.CHAR:
                return "toChar";
            case org.objectweb.asm.Type.DOUBLE:
                return "toDouble";
            case org.objectweb.asm.Type.FLOAT:
                return "toFloat";
            case org.objectweb.asm.Type.BOOLEAN:
                return "toBoolean";
            default:
                return null;
        }
    }

    private static void collectClassFiles(File dir, List<File> out) {
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

    private static byte[] read(InputStream in) throws IOException {
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

    private static void write(File f, byte[] data) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }
}
