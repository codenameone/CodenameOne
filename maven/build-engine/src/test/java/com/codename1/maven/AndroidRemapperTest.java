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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AndroidRemapperTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final com.codename1.build.Log LOG = new com.codename1.build.Log() {
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable e) { }
        public void debug(Throwable e) { }
        public void info(CharSequence c) { }
        public void info(CharSequence c, Throwable e) { }
        public void info(Throwable e) { }
        public void warn(CharSequence c) { }
        public void warn(CharSequence c, Throwable e) { }
        public void warn(Throwable e) { }
        public void error(CharSequence c) { }
        public void error(CharSequence c, Throwable e) { }
        public void error(Throwable e) { }
        public boolean isDebugEnabled() { return false; }
        public boolean isInfoEnabled() { return false; }
        public boolean isWarnEnabled() { return false; }
        public boolean isErrorEnabled() { return false; }
    };

    /// An activity-like class: extends android.app.Activity, has a handler
    /// `public void tapped(android.view.View)` and uses java.io.BufferedReader.
    private static byte[] appClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/x/Main", null, "android/app/Activity", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "tapped", "(Landroid/view/View;)V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/io/BufferedReader");
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] runtimeClass(String name, String superName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, superName, null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static List<String> refs(byte[] cls) {
        final List<String> out = new ArrayList<String>();
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int v, int a, String name, String sig, String sup, String[] itf) {
                out.add(name);
                out.add(sup);
            }

            @Override
            public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                out.add(d);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitTypeInsn(int op, String type) {
                        out.add(type);
                    }
                };
            }
        }, 0);
        return out;
    }

    @Test
    public void relocatesApplicationAndRuntimeAndWritesTheDispatcher() throws Exception {
        File classes = tmp.newFolder("classes");
        new File(classes, "com/x").mkdirs();
        Files.write(new File(classes, "com/x/Main.class").toPath(), appClass());
        File jar = tmp.newFile("codenameone-android-compat-8.jar");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        z.putNextEntry(new ZipEntry("android/app/Activity.class"));
        z.write(runtimeClass("android/app/Activity", "java/lang/Object"));
        z.closeEntry();
        z.putNextEntry(new ZipEntry("com/codename1/androidcompat/runtime/AndroidApp.class"));
        z.write(runtimeClass("com/codename1/androidcompat/runtime/AndroidApp", "java/lang/Object"));
        z.closeEntry();
        z.putNextEntry(new ZipEntry("cn1_android_framework.bin"));
        z.write(new byte[] {1, 2, 3});
        z.closeEntry();
        z.putNextEntry(new ZipEntry("META-INF/android-compat/framework.symbols"));
        z.write(new byte[] {1});
        z.closeEntry();
        z.close();
        File onClick = tmp.newFile("onclick.txt");
        Files.write(onClick.toPath(), "tapped\nmissing\n".getBytes("UTF-8"));

        new AndroidRemapper(classes, jar, onClick, LOG).run();

        List<String> app = refs(Files.readAllBytes(new File(classes, "com/x/Main.class").toPath()));
        assertTrue(app.toString(), app.contains("com/codename1/androidcompat/android/app/Activity"));
        assertTrue(app.toString(), app.contains("(Lcom/codename1/androidcompat/android/view/View;)V"));
        assertTrue(app.toString(), app.contains("com/codename1/androidcompat/jdk/BufferedReader"));
        for (String r : app) {
            assertFalse(r, r != null && (r.startsWith("android/") || r.contains("Landroid/")));
        }
        assertTrue(new File(classes, "com/codename1/androidcompat/android/app/Activity.class").isFile());
        assertTrue(new File(classes, "com/codename1/androidcompat/rt/AndroidApp.class").isFile());
        assertFalse(new File(classes, "android").exists());
        assertTrue(new File(classes, "cn1_android_framework.bin").isFile());
        assertFalse(new File(classes, "META-INF/android-compat").exists());
        byte[] dispatch = Files.readAllBytes(new File(classes, AndroidRemapper.ON_CLICK_DISPATCH + ".class").toPath());
        List<String> d = refs(dispatch);
        assertTrue(d.toString(), d.contains("com/x/Main"));

        // A second run over relocated output is a no-op.
        byte[] before = Files.readAllBytes(new File(classes, "com/x/Main.class").toPath());
        new AndroidRemapper(classes, jar, onClick, LOG).run();
        assertEquals(before.length, Files.readAllBytes(new File(classes, "com/x/Main.class").toPath()).length);
    }

    /// A class with a constructor that calls its superclass's, so the
    /// generated factory's `new` really runs.
    private static byte[] classWithCtor(String name, String superName, int access, int ctorAccess) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, access, name, null, superName, null);
        MethodVisitor mv = cw.visitMethod(ctorAccess, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void generatesTheFragmentFactoryFromCompiledClasses() throws Exception {
        File classes = tmp.newFolder("classes");
        new File(classes, "com/x").mkdirs();
        int pub = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        // Direct, through a runtime subclass, and through another app fragment.
        Files.write(new File(classes, "com/x/Plain.class").toPath(),
                classWithCtor("com/x/Plain", "android/app/Fragment", pub, Opcodes.ACC_PUBLIC));
        Files.write(new File(classes, "com/x/Listing.class").toPath(),
                classWithCtor("com/x/Listing", "android/app/ListFragment", pub, Opcodes.ACC_PUBLIC));
        Files.write(new File(classes, "com/x/Deeper.class").toPath(),
                classWithCtor("com/x/Deeper", "com/x/Plain", pub, Opcodes.ACC_PUBLIC));
        Files.write(new File(classes, "com/x/Outer$Inner.class").toPath(),
                classWithCtor("com/x/Outer$Inner", "android/app/Fragment", pub, Opcodes.ACC_PUBLIC));
        // Not instantiable by name: no public constructor, abstract, not a fragment.
        Files.write(new File(classes, "com/x/Hidden.class").toPath(),
                classWithCtor("com/x/Hidden", "android/app/Fragment", pub, Opcodes.ACC_PRIVATE));
        Files.write(new File(classes, "com/x/Base.class").toPath(),
                classWithCtor("com/x/Base", "android/app/Fragment", pub | Opcodes.ACC_ABSTRACT, Opcodes.ACC_PUBLIC));
        Files.write(new File(classes, "com/x/Other.class").toPath(),
                classWithCtor("com/x/Other", "java/lang/Object", pub, Opcodes.ACC_PUBLIC));
        File jar = tmp.newFile("codenameone-android-compat-8.jar");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        z.putNextEntry(new ZipEntry("android/app/Fragment.class"));
        z.write(classWithCtor("android/app/Fragment", "java/lang/Object", pub, Opcodes.ACC_PUBLIC));
        z.closeEntry();
        z.putNextEntry(new ZipEntry("android/app/ListFragment.class"));
        z.write(classWithCtor("android/app/ListFragment", "android/app/Fragment", pub, Opcodes.ACC_PUBLIC));
        z.closeEntry();
        z.close();

        new AndroidRemapper(classes, jar, null, LOG).run();

        java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {classes.toURI().toURL()},
                null);
        try {
            Class<?> factory = loader.loadClass(AndroidRemapper.FRAGMENT_FACTORY.replace('/', '.'));
            java.lang.reflect.Method m = factory.getMethod("instantiate", String.class);
            for (String name : new String[] {"com.x.Plain", "com.x.Listing", "com.x.Deeper", "com.x.Outer$Inner"}) {
                Object o = m.invoke(null, name);
                assertTrue(name, o != null && o.getClass().getName().equals(name));
            }
            for (String name : new String[] {"com.x.Hidden", "com.x.Base", "com.x.Other",
                "com.codename1.androidcompat.android.app.ListFragment", "nope"}) {
                assertEquals(name, null, m.invoke(null, name));
            }
        } finally {
            loader.close();
        }
    }

    /// `public <ret> name(<desc params>)` returning its first argument (or
    /// twice it, for ints), optionally annotated `@JavascriptInterface`.
    private static void jsMethod(ClassWriter cw, String name, String desc, boolean annotated) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, name, desc, null, null);
        if (annotated) {
            mv.visitAnnotation("Landroid/webkit/JavascriptInterface;", true).visitEnd();
        }
        mv.visitCode();
        org.objectweb.asm.Type ret = org.objectweb.asm.Type.getReturnType(desc);
        if (ret.getSort() == org.objectweb.asm.Type.VOID) {
            mv.visitInsn(Opcodes.RETURN);
        } else if (ret.getSort() == org.objectweb.asm.Type.INT) {
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitInsn(Opcodes.ICONST_2);
            mv.visitInsn(Opcodes.IMUL);
            mv.visitInsn(Opcodes.IRETURN);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ARETURN);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /// The runtime's JsBridge reduced to what the test's methods use.
    private static byte[] jsBridgeStub() {
        String name = "com/codename1/androidcompat/runtime/JsBridge";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        int st = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        MethodVisitor mv = cw.visitMethod(st, "toInt", "(Ljava/lang/String;)I", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(st, "literal", "(I)Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(st, "literal", "(Ljava/lang/Object;)Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf",
                "(Ljava/lang/Object;)Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(st, "undefined", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("undefined");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void generatesTheJavascriptInterfaceDispatcher() throws Exception {
        File classes = tmp.newFolder("classes");
        new File(classes, "com/x").mkdirs();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "com/x/Bridge", null, "java/lang/Object", null);
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();
        jsMethod(cw, "greet", "(Ljava/lang/String;)Ljava/lang/String;", true);
        jsMethod(cw, "twice", "(I)I", true);
        jsMethod(cw, "ping", "()V", true);
        jsMethod(cw, "hidden", "(Ljava/lang/String;)Ljava/lang/String;", false);
        jsMethod(cw, "unpassable", "(Ljava/util/List;)Ljava/util/List;", true);
        cw.visitEnd();
        Files.write(new File(classes, "com/x/Bridge.class").toPath(), cw.toByteArray());
        File jar = tmp.newFile("codenameone-android-compat-8.jar");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        z.putNextEntry(new ZipEntry("com/codename1/androidcompat/runtime/JsBridge.class"));
        z.write(jsBridgeStub());
        z.closeEntry();
        z.close();

        new AndroidRemapper(classes, jar, null, LOG).run();

        java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {classes.toURI().toURL()},
                null);
        try {
            Class<?> dispatch = loader.loadClass(AndroidRemapper.JS_INTERFACE_DISPATCH.replace('/', '.'));
            Object bridge = loader.loadClass("com.x.Bridge").newInstance();
            String names = (String) dispatch.getMethod("methods", Object.class).invoke(null, bridge);
            assertTrue(names, names.contains("greet,") && names.contains("twice,") && names.contains("ping,"));
            assertFalse(names, names.contains("hidden") || names.contains("unpassable"));
            assertEquals("", dispatch.getMethod("methods", Object.class).invoke(null, "not a bridge"));
            java.lang.reflect.Method invoke = dispatch.getMethod("invoke", Object.class, String.class,
                    String[].class);
            assertEquals("hi", invoke.invoke(null, bridge, "greet", new String[] {"hi"}));
            assertEquals("42", invoke.invoke(null, bridge, "twice", new String[] {"21"}));
            assertEquals("undefined", invoke.invoke(null, bridge, "ping", new String[0]));
            // Wrong arity, an unannotated method, or another object: not found.
            assertEquals(null, invoke.invoke(null, bridge, "greet", new String[] {"a", "b"}));
            assertEquals(null, invoke.invoke(null, bridge, "hidden", new String[] {"a"}));
            assertEquals(null, invoke.invoke(null, "not a bridge", "greet", new String[] {"a"}));
        } finally {
            loader.close();
        }
    }

    @Test
    public void importerReadsGradleBuilds() {
        AndroidProjectImporter.Result r = new AndroidProjectImporter.Result();
        AndroidProjectImporter.readGradle("android {\n namespace 'com.acme.app'\n defaultConfig {\n"
                + " applicationId \"com.acme.store\"\n versionName \"2.1\"\n }\n}\n dependencies {\n"
                + " implementation 'androidx.appcompat:appcompat:1.6.1'\n"
                + " implementation(\"com.squareup.retrofit2:retrofit:2.9.0\")\n"
                + " testImplementation 'junit:junit:4.13.2'\n}\n", r);
        assertEquals("com.acme.app", r.namespace);
        assertEquals("com.acme.store", r.applicationId);
        assertEquals("2.1", r.versionName);
        assertEquals(1, r.covered.size());
        assertEquals("com.squareup.retrofit2:retrofit", r.uncovered.get(0));
    }

    /// The java.io classes the Codename One runtime lacks go to the shims,
    /// Closeable becomes AutoCloseable, and a class that named both keeps one.
    @Test
    public void mapsJavaIoShimsAndKeepsInterfacesDistinct() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/x/Res", null, "java/lang/Object",
                new String[] {"java/io/Closeable", "java/lang/AutoCloseable"});
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "open", "(Ljava/io/File;)Ljava/io/InputStream;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/io/FileInputStream");
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        byte[] out = AndroidRemapper.remap(cw.toByteArray());
        final List<String[]> itf = new ArrayList<String[]>();
        new ClassReader(out).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int v, int a, String name, String sig, String sup, String[] interfaces) {
                itf.add(interfaces);
            }
        }, 0);
        assertEquals(1, itf.get(0).length);
        assertEquals("java/lang/AutoCloseable", itf.get(0)[0]);
        List<String> r = refs(out);
        assertTrue(r.toString(), r.contains("(Lcom/codename1/androidcompat/jdk/File;)Ljava/io/InputStream;"));
        assertTrue(r.toString(), r.contains("com/codename1/androidcompat/jdk/FileInputStream"));
    }

    /// A public class `name` extending `superName` with public constructors
    /// of the given descriptors, each calling the superclass's `()V`.
    private static byte[] classWithCtors(String name, String superName, String... descs) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, superName, null);
        for (String desc : descs) {
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    public void generatesTheViewModelFactoryFromCompiledClasses() throws Exception {
        File classes = tmp.newFolder("vmclasses");
        new File(classes, "com/x").mkdirs();
        String app = "Landroid/app/Application;";
        String handle = "Landroidx/lifecycle/SavedStateHandle;";
        String vm = "androidx/lifecycle/ViewModel";
        Files.write(new File(classes, "com/x/Plain.class").toPath(), classWithCtors("com/x/Plain", vm, "()V"));
        Files.write(new File(classes, "com/x/WithApp.class").toPath(),
                classWithCtors("com/x/WithApp", vm, "(" + app + ")V"));
        Files.write(new File(classes, "com/x/WithHandle.class").toPath(),
                classWithCtors("com/x/WithHandle", vm, "(" + handle + ")V"));
        Files.write(new File(classes, "com/x/Both.class").toPath(),
                classWithCtors("com/x/Both", vm, "()V", "(" + app + handle + ")V"));
        Files.write(new File(classes, "com/x/Deeper.class").toPath(),
                classWithCtors("com/x/Deeper", "com/x/Plain", "()V"));
        Files.write(new File(classes, "com/x/Hidden.class").toPath(),
                classWithCtor("com/x/Hidden", vm, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, Opcodes.ACC_PRIVATE));
        Files.write(new File(classes, "com/x/NotAModel.class").toPath(),
                classWithCtors("com/x/NotAModel", "java/lang/Object", "()V"));
        File jar = tmp.newFile("codenameone-android-compat-vm.jar");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        int pub = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        for (String[] c : new String[][] {{vm, "java/lang/Object"}, {"android/app/Application", "java/lang/Object"},
            {"androidx/lifecycle/SavedStateHandle", "java/lang/Object"}}) {
            z.putNextEntry(new ZipEntry(c[0] + ".class"));
            z.write(classWithCtor(c[0], c[1], pub, Opcodes.ACC_PUBLIC));
            z.closeEntry();
        }
        z.close();

        new AndroidRemapper(classes, jar, null, LOG).run();

        java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {classes.toURI().toURL()},
                null);
        try {
            Class<?> factory = loader.loadClass(AndroidRemapper.VIEW_MODEL_FACTORY.replace('/', '.'));
            java.lang.reflect.Method m = factory.getMethod("create", String.class, Object.class, Object.class);
            Object application = loader.loadClass("com.codename1.androidcompat.android.app.Application").newInstance();
            Object savedState = loader.loadClass("com.codename1.androidcompat.androidx.lifecycle.SavedStateHandle")
                    .newInstance();
            for (String name : new String[] {"com.x.Plain", "com.x.Deeper", "com.x.WithApp", "com.x.WithHandle",
                "com.x.Both"}) {
                Object o = m.invoke(null, name, application, savedState);
                assertTrue(name, o != null && o.getClass().getName().equals(name));
            }
            // Only what the calling factory supplies: no application, no handle.
            assertTrue(m.invoke(null, "com.x.Plain", null, null) != null);
            assertEquals(null, m.invoke(null, "com.x.WithApp", null, savedState));
            assertEquals(null, m.invoke(null, "com.x.WithHandle", application, null));
            // Both prefers (Application, SavedStateHandle), as AndroidX's default factory does.
            assertEquals(null, m.invoke(null, "com.x.Both", application, null));
            for (String name : new String[] {"com.x.Hidden", "com.x.NotAModel", "nope"}) {
                assertEquals(name, null, m.invoke(null, name, application, savedState));
            }
        } finally {
            loader.close();
        }
    }

    /// Kotlin keeps member descriptors as strings in @kotlin.Metadata's d2.
    @Test
    public void remapsKotlinMetadataDescriptors() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/x/KtActivity", null, "android/app/Activity", null);
        org.objectweb.asm.AnnotationVisitor meta = cw.visitAnnotation("Lkotlin/Metadata;", true);
        org.objectweb.asm.AnnotationVisitor d2 = meta.visitArray("d2");
        d2.visit(null, "Lcom/x/KtActivity;");
        d2.visit(null, "Landroid/app/Activity;");
        d2.visit(null, "onCreate");
        d2.visit(null, "(Landroid/os/Bundle;)V");
        d2.visitEnd();
        meta.visitEnd();
        cw.visitEnd();
        final List<String> d2Out = new ArrayList<String>();
        new ClassReader(AndroidRemapper.remap(cw.toByteArray())).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public org.objectweb.asm.AnnotationVisitor visitArray(String name) {
                        return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String n, Object value) {
                                d2Out.add(String.valueOf(value));
                            }
                        };
                    }
                };
            }
        }, 0);
        assertEquals("Lcom/x/KtActivity;", d2Out.get(0));
        assertEquals("Lcom/codename1/androidcompat/android/app/Activity;", d2Out.get(1));
        assertEquals("onCreate", d2Out.get(2));
        assertEquals("(Lcom/codename1/androidcompat/android/os/Bundle;)V", d2Out.get(3));
    }

    /// The runtime is copied out of its jar into the classes directory; an
    /// entry naming a path outside it ("Zip Slip") must stop the build, not
    /// write there.
    @Test
    public void anEntryOutsideTheClassesDirectoryIsRefused() throws Exception {
        File classes = tmp.newFolder("slip-classes");
        File jar = tmp.newFile("codenameone-android-compat-slip.jar");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        z.putNextEntry(new ZipEntry("../evil.txt"));
        z.write(new byte[] {1});
        z.closeEntry();
        z.close();
        File evil = new File(classes.getParentFile(), "evil.txt");
        try {
            new AndroidRemapper(classes, jar, tmp.newFile("slip-onclick.txt"), LOG).run();
            org.junit.Assert.fail("an entry outside the classes directory was extracted");
        } catch (com.codename1.builders.BuildException expected) {
            String why = expected.getMessage() + (expected.getCause() == null ? "" : " " + expected.getCause().getMessage());
            assertTrue(why, why.contains("resolves outside"));
        }
        assertFalse(evil.exists());
    }

    /// An incremental build after the runtime jar changed: a resource whose
    /// contents changed but whose length did not (a rebuilt resource table)
    /// must replace the copy the previous build extracted.
    @Test
    public void aChangedResourceOfTheSameLengthIsReplaced() throws Exception {
        File classes = tmp.newFolder("res-classes");
        File onClick = tmp.newFile("res-onclick.txt");
        File jar = tmp.newFile("codenameone-android-compat-res.jar");
        writeJar(jar, "cn1_android_framework.bin", new byte[] {1, 2, 3, 4});
        new AndroidRemapper(classes, jar, onClick, LOG).run();
        File table = new File(classes, "cn1_android_framework.bin");
        assertArrayEquals(new byte[] {1, 2, 3, 4}, Files.readAllBytes(table.toPath()));

        writeJar(jar, "cn1_android_framework.bin", new byte[] {9, 8, 7, 6});
        new AndroidRemapper(classes, jar, onClick, LOG).run();
        assertArrayEquals("the previous runtime's resource table survived the rebuild",
                new byte[] {9, 8, 7, 6}, Files.readAllBytes(table.toPath()));
    }

    private static void writeJar(File jar, String entry, byte[] data) throws IOException {
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(jar));
        try {
            z.putNextEntry(new ZipEntry(entry));
            z.write(data);
            z.closeEntry();
        } finally {
            z.close();
        }
    }
}
