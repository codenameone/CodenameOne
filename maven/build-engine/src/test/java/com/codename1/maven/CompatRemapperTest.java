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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The remap entry point for any set of compatibility layers: which layers a
/// classpath switches on, that they are applied as one pass, and that a
/// second run finds nothing left to do.
public class CompatRemapperTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    static final com.codename1.build.Log LOG = new com.codename1.build.Log() {
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

    static final String SWING = "com/codename1/desktopcompat/";
    static final String FX = "com/codename1/fxcompat/";

    /// A class with no members, as a runtime jar would hold it.
    static byte[] emptyClass(String name, String superName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, superName, null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    static File jar(File file, Object... nameThenBytes) throws IOException {
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(file));
        try {
            for (int i = 0; i < nameThenBytes.length; i += 2) {
                z.putNextEntry(new ZipEntry((String) nameThenBytes[i]));
                z.write((byte[]) nameThenBytes[i + 1]);
                z.closeEntry();
            }
        } finally {
            z.close();
        }
        return file;
    }

    /// An application class that touches every layer: it extends a Swing
    /// frame, holds a JavaFX stage, has an `android:onClick` handler and
    /// opens a `java.io.File`.
    private static byte[] appClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/x/Main", null, "javax/swing/JFrame", null);
        FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE, "stage", "Ljavafx/stage/Stage;", null, null);
        fv.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "tapped", "(Landroid/view/View;)V", null, null);
        mv.visitCode();
        mv.visitTypeInsn(Opcodes.NEW, "java/io/File");
        mv.visitInsn(Opcodes.POP);
        mv.visitTypeInsn(Opcodes.NEW, "java/awt/Color");
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /// Every class name, descriptor and type instruction of `cls`.
    private static List<String> refs(byte[] cls) {
        final List<String> out = new ArrayList<String>();
        new ClassReader(cls).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int v, int a, String name, String sig, String sup, String[] itf) {
                out.add(name);
                out.add(sup);
            }

            @Override
            public FieldVisitor visitField(int a, String n, String d, String s, Object value) {
                out.add(d);
                return null;
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

    private File swingJar() throws IOException {
        // Authored under the names it ships with; one class uses a JDK class
        // the device lacks, which is redirected as it is extracted.
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, SWING + "javax/swing/JFileChooser", null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PRIVATE, "selected", "Ljava/io/File;", null, null).visitEnd();
        cw.visitEnd();
        return jar(new File(tmp.getRoot(), "codenameone-swing-compat-1.jar"),
                SWING + "javax/swing/JFrame.class", emptyClass(SWING + "javax/swing/JFrame", "java/lang/Object"),
                SWING + "java/awt/Color.class", emptyClass(SWING + "java/awt/Color", "java/lang/Object"),
                SWING + "javax/swing/JFileChooser.class", cw.toByteArray(),
                SWING + "rt/SwingApp.class", emptyClass(SWING + "rt/SwingApp", "java/lang/Object"),
                "cn1_swing_defaults.bin", new byte[] {4, 5, 6},
                "META-INF/MANIFEST.MF", new byte[] {1});
    }

    private File javafxJar() throws IOException {
        return jar(new File(tmp.getRoot(), "codenameone-javafx-compat-1.jar"),
                "javafx/stage/Stage.class", emptyClass("javafx/stage/Stage", "java/lang/Object"),
                FX + "runtime/FxApp.class", emptyClass(FX + "runtime/FxApp", "javafx/stage/Stage"));
    }

    private File androidJar() throws IOException {
        return jar(new File(tmp.getRoot(), "codenameone-android-compat-1.jar"),
                "android/view/View.class", emptyClass("android/view/View", "java/lang/Object"),
                "com/codename1/androidcompat/runtime/AndroidApp.class",
                emptyClass("com/codename1/androidcompat/runtime/AndroidApp", "java/lang/Object"));
    }

    private File jdkJar() throws IOException {
        return jar(new File(tmp.getRoot(), "codenameone-compat-jdk-1.jar"),
                "com/codename1/compat/jdk/File.class", emptyClass("com/codename1/compat/jdk/File", "java/lang/Object"));
    }

    private File classesWithApp(String folder) throws IOException {
        File classes = tmp.newFolder(folder);
        assertTrue(new File(classes, "com/x").mkdirs());
        Files.write(new File(classes, "com/x/Main.class").toPath(), appClass());
        return classes;
    }

    /// Every file under `dir`, by relative path, with its bytes.
    private static Map<String, byte[]> snapshot(File dir) throws IOException {
        Map<String, byte[]> out = new TreeMap<String, byte[]>();
        snapshot(dir, dir, out);
        return out;
    }

    private static void snapshot(File root, File dir, Map<String, byte[]> out) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                snapshot(root, f, out);
            } else {
                out.put(root.toURI().relativize(f.toURI()).getPath(), Files.readAllBytes(f.toPath()));
            }
        }
    }

    private static void assertSameTree(Map<String, byte[]> expected, Map<String, byte[]> actual) {
        assertEquals(expected.keySet(), actual.keySet());
        for (Map.Entry<String, byte[]> e : expected.entrySet()) {
            assertArrayEquals(e.getKey(), e.getValue(), actual.get(e.getKey()));
        }
    }

    @Test
    public void theLayersAreRegisteredWithTheirOwnPackages() {
        assertEquals(Arrays.asList(AndroidRemapper.RELOCATION, CompatLayers.SWING, CompatLayers.JAVAFX),
                CompatLayers.ALL);
        ClassRelocator every = CompatLayers.EVERY;
        assertEquals(SWING + "java/awt/Color", every.map("java/awt/Color"));
        assertEquals(SWING + "javax/swing/JTable", every.map("javax/swing/JTable"));
        assertEquals(SWING + "java/beans/PropertyChangeListener", every.map("java/beans/PropertyChangeListener"));
        assertEquals(SWING + "javax/accessibility/Accessible", every.map("javax/accessibility/Accessible"));
        assertEquals(SWING + "javax/imageio/ImageIO", every.map("javax/imageio/ImageIO"));
        assertEquals(SWING + "org/jdesktop/layout/GroupLayout", every.map("org/jdesktop/layout/GroupLayout"));
        assertEquals(FX + "javafx/stage/Stage", every.map("javafx/stage/Stage"));
        assertEquals(FX + "rt/FxApp", every.map(FX + "runtime/FxApp"));
        assertEquals("com/codename1/compat/jdk/File", every.map("java/io/File"));
        // The names the Swing runtime is authored under are final already.
        assertEquals(SWING + "javax/swing/JTable", every.map(SWING + "javax/swing/JTable"));
        assertEquals(SWING + "rt/SwingApp", every.map(SWING + "rt/SwingApp"));
        // And back, for build messages.
        assertEquals("javax/swing/JTable", every.original(SWING + "javax/swing/JTable"));
        assertEquals("javafx/stage/Stage", every.original(FX + "javafx/stage/Stage"));
        assertEquals(SWING + "rt/SwingApp", every.original(SWING + "rt/SwingApp"));
        assertSame(CompatLayers.SWING, CompatLayers.owning("javax/swing/JTable"));
        assertSame(CompatLayers.JAVAFX, CompatLayers.owning("javafx/stage/Stage"));
        assertEquals(null, CompatLayers.owning("java/util/List"));
        assertEquals(null, CompatLayers.owning(SWING + "javax/swing/JTable"));
    }

    @Test
    public void relocatesSwingReferencesOntoARuntimeAuthoredUnderItsFinalNames() throws Exception {
        File classes = classesWithApp("classes");
        List<File> classpath = Arrays.asList(swingJar(), jdkJar(), new File(tmp.getRoot(), "other-1.jar"));

        CompatRemapper remapper = new CompatRemapper(classes, classpath, null, LOG);
        assertEquals(Collections.singletonList(CompatLayers.SWING), remapper.activeLayers());
        assertTrue(remapper.run());

        List<String> app = refs(Files.readAllBytes(new File(classes, "com/x/Main.class").toPath()));
        assertTrue(app.toString(), app.contains(SWING + "javax/swing/JFrame"));
        assertTrue(app.toString(), app.contains(SWING + "java/awt/Color"));
        assertTrue(app.toString(), app.contains("com/codename1/compat/jdk/File"));
        // Layers that are not active leave their names alone.
        assertTrue(app.toString(), app.contains("Ljavafx/stage/Stage;"));
        assertTrue(app.toString(), app.contains("(Landroid/view/View;)V"));

        assertTrue(new File(classes, SWING + "javax/swing/JFrame.class").isFile());
        assertTrue(new File(classes, SWING + "rt/SwingApp.class").isFile());
        assertTrue(new File(classes, "cn1_swing_defaults.bin").isFile());
        assertTrue(new File(classes, "com/codename1/compat/jdk/File.class").isFile());
        assertFalse(new File(classes, "javax").exists());
        assertFalse(new File(classes, "META-INF").exists());
        // No Android step ran.
        assertFalse(new File(classes, AndroidRemapper.ON_CLICK_DISPATCH + ".class").exists());
        // The runtime's own use of a JDK class the device lacks is redirected.
        List<String> chooser = refs(Files.readAllBytes(
                new File(classes, SWING + "javax/swing/JFileChooser.class").toPath()));
        assertTrue(chooser.toString(), chooser.contains("Lcom/codename1/compat/jdk/File;"));
    }

    @Test
    public void appliesAndroidSwingAndJavaFxAsOnePass() throws Exception {
        File classes = classesWithApp("classes");
        File onClick = tmp.newFile("onclick.txt");
        Files.write(onClick.toPath(), "tapped\n".getBytes("UTF-8"));
        List<File> classpath = Arrays.asList(androidJar(), swingJar(), javafxJar(), jdkJar());

        CompatRemapper remapper = new CompatRemapper(classes, classpath, onClick, LOG);
        assertEquals(CompatLayers.ALL, remapper.activeLayers());
        assertEquals(3, remapper.relocator().relocations().size());
        assertTrue(remapper.run());

        List<String> app = refs(Files.readAllBytes(new File(classes, "com/x/Main.class").toPath()));
        assertTrue(app.toString(), app.contains(SWING + "javax/swing/JFrame"));
        assertTrue(app.toString(), app.contains("L" + FX + "javafx/stage/Stage;"));
        assertTrue(app.toString(), app.contains("(Lcom/codename1/androidcompat/android/view/View;)V"));
        assertTrue(app.toString(), app.contains("com/codename1/compat/jdk/File"));

        assertTrue(new File(classes, "com/codename1/androidcompat/android/view/View.class").isFile());
        assertTrue(new File(classes, "com/codename1/androidcompat/rt/AndroidApp.class").isFile());
        assertTrue(new File(classes, SWING + "javax/swing/JFrame.class").isFile());
        assertTrue(new File(classes, FX + "javafx/stage/Stage.class").isFile());
        assertTrue(new File(classes, FX + "rt/FxApp.class").isFile());
        assertFalse(new File(classes, FX + "runtime").exists());
        assertFalse(new File(classes, "javafx").exists());
        assertFalse(new File(classes, "android").exists());
        assertTrue(new File(classes, "com/codename1/compat/jdk/File.class").isFile());
        // The JavaFX runtime's reference to its own API moved with it.
        assertTrue(refs(Files.readAllBytes(new File(classes, FX + "rt/FxApp.class").toPath()))
                .contains(FX + "javafx/stage/Stage"));
        // The Android generators ran, over the composed relocation.
        byte[] dispatch = Files.readAllBytes(new File(classes, AndroidRemapper.ON_CLICK_DISPATCH + ".class").toPath());
        final boolean[] callsHandler = new boolean[1];
        new ClassReader(dispatch).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                        callsHandler[0] |= owner.equals("com/x/Main") && name.equals("tapped");
                    }
                };
            }
        }, 0);
        assertTrue(callsHandler[0]);
    }

    @Test
    public void aSecondRunChangesNothing() throws Exception {
        File swingOnly = classesWithApp("swing");
        List<File> swingClasspath = Arrays.asList(swingJar(), jdkJar());
        assertTrue(new CompatRemapper(swingOnly, swingClasspath, null, LOG).run());
        Map<String, byte[]> first = snapshot(swingOnly);
        assertTrue(new CompatRemapper(swingOnly, swingClasspath, null, LOG).run());
        assertSameTree(first, snapshot(swingOnly));

        File all = classesWithApp("all");
        File onClick = tmp.newFile("onclick.txt");
        Files.write(onClick.toPath(), "tapped\n".getBytes("UTF-8"));
        List<File> classpath = Arrays.asList(androidJar(), swingJar(), javafxJar(), jdkJar());
        assertTrue(new CompatRemapper(all, classpath, onClick, LOG).run());
        first = snapshot(all);
        assertTrue(new CompatRemapper(all, classpath, onClick, LOG).run());
        assertSameTree(first, snapshot(all));
        assertTrue(new CompatRemapper(all, classpath, onClick, LOG).relocateOnly().run());
        assertSameTree(first, snapshot(all));
    }

    @Test
    public void withoutALayerNothingIsTouched() throws Exception {
        File classes = classesWithApp("classes");
        Map<String, byte[]> before = snapshot(classes);

        CompatRemapper remapper = new CompatRemapper(classes,
                Arrays.asList(jdkJar(), new File(tmp.getRoot(), "codenameone-core-1.jar")), null, LOG);
        assertTrue(remapper.activeLayers().isEmpty());
        assertFalse(remapper.run());
        assertSameTree(before, snapshot(classes));

        assertFalse(new CompatRemapper(classes, null, null, LOG).run());
        assertSameTree(before, snapshot(classes));
    }

    /// An application that has not switched the Swing layer on keeps its
    /// `java/awt/` references, whatever other layer it uses: the compliance
    /// check is what tells its developer about them.
    @Test
    public void anInactiveLayersNamesAreLeftForTheComplianceCheck() throws Exception {
        File classes = classesWithApp("classes");
        assertTrue(new CompatRemapper(classes, Arrays.asList(androidJar(), jdkJar()), null, LOG).run());

        List<String> app = refs(Files.readAllBytes(new File(classes, "com/x/Main.class").toPath()));
        assertTrue(app.toString(), app.contains("javax/swing/JFrame"));
        assertTrue(app.toString(), app.contains("java/awt/Color"));
        assertTrue(app.toString(), app.contains("Ljavafx/stage/Stage;"));
        assertTrue(app.toString(), app.contains("(Lcom/codename1/androidcompat/android/view/View;)V"));
    }

    /// With only Android active the generic entry point is the Android step
    /// and nothing else: the same files with the same bytes.
    @Test
    public void androidAloneIsExactlyTheAndroidStep() throws Exception {
        File onClick = tmp.newFile("onclick.txt");
        Files.write(onClick.toPath(), "tapped\nmissing\n".getBytes("UTF-8"));
        File android = androidJar();
        File jdk = jdkJar();

        File direct = classesWithApp("direct");
        new AndroidRemapper(direct, android, onClick, LOG).withSupportJars(Collections.singletonList(jdk)).run();
        File generic = classesWithApp("generic");
        assertTrue(new CompatRemapper(generic, Arrays.asList(android, jdk), onClick, LOG).run());

        assertSameTree(snapshot(direct), snapshot(generic));
    }

    /// An application class that uses Swing and nothing else.
    private static byte[] swingOnlyClass(String name) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "javax/swing/JFrame", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    /// The templates put both desktop jars on the classpath of a project with
    /// desktop sources. A Swing application must not pay for the JavaFX
    /// runtime: a desktop layer ships only when the classes refer to it.
    @Test
    public void aDesktopLayerShipsOnlyWhenTheClassesUseIt() throws Exception {
        File classes = tmp.newFolder("classes");
        assertTrue(new File(classes, "com/x").mkdirs());
        Files.write(new File(classes, "com/x/Main.class").toPath(), swingOnlyClass("com/x/Main"));
        List<File> classpath = Arrays.asList(swingJar(), javafxJar(), jdkJar());

        CompatRemapper remapper = new CompatRemapper(classes, classpath, null, LOG);
        assertEquals(Collections.singletonList(CompatLayers.SWING), remapper.activeLayers());
        assertTrue(remapper.run());
        assertTrue(new File(classes, SWING + "javax/swing/JFrame.class").isFile());
        assertFalse(new File(classes, FX).exists());

        // Read back from the relocated classes, the answer is the same, and
        // the extracted Swing runtime is not mistaken for application code.
        Map<String, byte[]> first = snapshot(classes);
        CompatRemapper again = new CompatRemapper(classes, classpath, null, LOG);
        assertEquals(Collections.singletonList(CompatLayers.SWING), again.activeLayers());
        assertTrue(again.run());
        assertSameTree(first, snapshot(classes));
    }

    /// With both jars present and neither used, nothing is active and the
    /// classes are left alone.
    @Test
    public void unusedDesktopJarsSwitchNothingOn() throws Exception {
        File classes = tmp.newFolder("classes");
        assertTrue(new File(classes, "com/x").mkdirs());
        Files.write(new File(classes, "com/x/Plain.class").toPath(), emptyClass("com/x/Plain", "java/lang/Object"));
        Map<String, byte[]> before = snapshot(classes);

        CompatRemapper remapper = new CompatRemapper(classes, Arrays.asList(swingJar(), javafxJar(), jdkJar()),
                null, LOG);
        assertTrue(remapper.activeLayers().isEmpty());
        assertFalse(remapper.run());
        assertSameTree(before, snapshot(classes));
    }

    /// Kotlin's classes are a directory of their own, relocated separately;
    /// a layer only they use still has to ship with the main pass.
    @Test
    public void aLayerUsedOnlyByAHandlerDirectoryStillShips() throws Exception {
        File classes = tmp.newFolder("classes");
        assertTrue(new File(classes, "com/x").mkdirs());
        Files.write(new File(classes, "com/x/Main.class").toPath(), swingOnlyClass("com/x/Main"));
        File kotlin = tmp.newFolder("kotlin");
        assertTrue(new File(kotlin, "com/x").mkdirs());
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/x/View", null, "java/lang/Object", null);
        cw.visitField(Opcodes.ACC_PRIVATE, "stage", "Ljavafx/stage/Stage;", null, null).visitEnd();
        cw.visitEnd();
        Files.write(new File(kotlin, "com/x/View.class").toPath(), cw.toByteArray());
        List<File> classpath = Arrays.asList(swingJar(), javafxJar(), jdkJar());

        CompatRemapper remapper = new CompatRemapper(classes, classpath, null, LOG)
                .withHandlerDirectories(Collections.singletonList(kotlin));
        assertEquals(Arrays.asList(CompatLayers.SWING, CompatLayers.JAVAFX), remapper.activeLayers());
        assertTrue(remapper.run());
        assertTrue(new File(classes, FX + "javafx/stage/Stage.class").isFile());

        // The Kotlin directory, on its own, is relocated by what it uses.
        CompatRemapper kotlinPass = new CompatRemapper(kotlin, classpath, null, LOG).relocateOnly();
        assertEquals(Collections.singletonList(CompatLayers.JAVAFX), kotlinPass.activeLayers());
        assertTrue(kotlinPass.run());
        assertTrue(refs(Files.readAllBytes(new File(kotlin, "com/x/View.class").toPath()))
                .contains("L" + FX + "javafx/stage/Stage;"));
    }

    /// The Android layer keeps its rule: its jar alone switches it on.
    @Test
    public void theAndroidLayerIsActiveByItsJarAlone() throws Exception {
        File classes = tmp.newFolder("classes");
        assertTrue(new File(classes, "com/x").mkdirs());
        Files.write(new File(classes, "com/x/Plain.class").toPath(), emptyClass("com/x/Plain", "java/lang/Object"));
        assertTrue(CompatLayers.activeByPresence(AndroidRemapper.RELOCATION));
        assertFalse(CompatLayers.activeByPresence(CompatLayers.SWING));
        assertFalse(CompatLayers.activeByPresence(CompatLayers.JAVAFX));
        assertEquals(Collections.singletonList(AndroidRemapper.RELOCATION),
                CompatLayers.active(Arrays.asList(androidJar(), swingJar(), javafxJar(), jdkJar()),
                        Collections.singletonList(classes)));
    }

    @Test
    public void theDesktopEntryRecordReachesTheGenerators() throws Exception {
        File record = new File(tmp.getRoot(), DesktopSources.ENTRY_RECORD);
        CompatRemapper remapper = new CompatRemapper(tmp.newFolder("classes"), null, null, LOG)
                .withDesktopEntryRecord(record);
        assertEquals(record, remapper.desktopEntryRecord());
    }

    @Test
    public void relocateOnlyShipsNoRuntime() throws Exception {
        File classes = classesWithApp("classes");
        assertTrue(new CompatRemapper(classes, Arrays.asList(swingJar(), javafxJar(), jdkJar()), null, LOG)
                .relocateOnly().run());

        List<String> app = refs(Files.readAllBytes(new File(classes, "com/x/Main.class").toPath()));
        assertTrue(app.toString(), app.contains(SWING + "javax/swing/JFrame"));
        assertTrue(app.toString(), app.contains("L" + FX + "javafx/stage/Stage;"));
        assertEquals(Collections.singleton("com/x/Main.class"), snapshot(classes).keySet());
    }
}
