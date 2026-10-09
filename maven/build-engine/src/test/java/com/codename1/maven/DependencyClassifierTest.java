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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// What a dependency jar is taken for, from its classes alone, and which of
/// its classes an application reaches.
public class DependencyClassifierTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final List<Relocation> LAYERS = Arrays.asList(CompatLayers.SWING, CompatLayers.JAVAFX);

    /// A class `name` that calls a static method of each of `uses` and holds
    /// each of `strings` as a constant.
    static byte[] cls(String name, String[] uses, String... strings) {
        return cls(Opcodes.V1_8, name, uses, strings);
    }

    static byte[] cls(int version, String name, String[] uses, String... strings) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(version, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V", null, null);
        mv.visitCode();
        for (String use : uses) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, use, "run", "()V", false);
        }
        for (String s : strings) {
            mv.visitLdcInsn(s);
            mv.visitInsn(Opcodes.POP);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    static String[] uses(String... names) {
        return names;
    }

    /// A jar of `count` classes `prefix0`..., the first `toolkit` of which
    /// name `javax/swing/JPanel`.
    private File jar(String name, String prefix, int count, int toolkit, Object... extra) throws Exception {
        List<Object> entries = new ArrayList<Object>();
        for (int i = 0; i < count; i++) {
            entries.add(prefix + i + ".class");
            entries.add(cls(prefix + i, i < toolkit ? uses("javax/swing/JPanel") : uses()));
        }
        entries.addAll(Arrays.asList(extra));
        return CompatRemapperTest.jar(new File(tmp.newFolder(), name), entries.toArray());
    }

    private DependencyClassifier.Library one(File jar) throws Exception {
        List<DependencyClassifier.Library> out = DependencyClassifier.classify(Collections.singletonList(jar), LAYERS);
        assertEquals(1, out.size());
        return out.get(0);
    }

    @Test
    public void theShareOfToolkitClassesDecidesWhatALibraryIs() throws Exception {
        DependencyClassifier.Library pure = one(jar("pure.jar", "org/pure/C", 10, 0));
        assertEquals(DependencyClassifier.Kind.PURE_JAVA, pure.kind());
        assertEquals(10, pure.classCount());
        assertEquals(0, pure.toolkitClassCount());
        assertEquals("org.pure", pure.mainPackage());

        DependencyClassifier.Library incidental = one(jar("incidental.jar", "org/inc/C", 10, 1));
        assertEquals(DependencyClassifier.Kind.INCIDENTAL_TOOLKIT, incidental.kind());
        assertEquals(1, incidental.toolkitClassCount());

        // The threshold itself is a toolkit library; one class under it is not.
        assertEquals(DependencyClassifier.Kind.UI_LIBRARY, one(jar("ui.jar", "org/ui/C", 10, 2)).kind());
        assertEquals(DependencyClassifier.Kind.INCIDENTAL_TOOLKIT, one(jar("almost.jar", "org/ui/C", 11, 2)).kind());
    }

    @Test
    public void aJarOfALayersOwnPackagesIsProvidedByTheLayer() throws Exception {
        DependencyClassifier.Library fx = one(jar("javafx-controls.jar", "javafx/scene/control/C", 4, 0));
        assertEquals(DependencyClassifier.Kind.LAYER_PROVIDED, fx.kind());
        assertEquals(CompatLayers.JAVAFX.name(), fx.layer());
        assertFalse("Nothing of it ever ships", fx.shipped().contains("javafx/scene/control/C0"));
        // A pure-Java library names no layer.
        assertNull(one(jar("pure.jar", "org/pure/C", 2, 0)).layer());
    }

    @Test
    public void whatTheBuildHandlesItselfIsThePlatforms() throws Exception {
        DependencyClassifier.Library kotlin = one(jar("kotlin-stdlib.jar", "kotlin/C", 3, 0));
        assertEquals(DependencyClassifier.Kind.PLATFORM, kotlin.kind());
        assertEquals("Kotlin runtime", kotlin.platform());
        assertFalse(kotlin.managed());

        DependencyClassifier.Library core = one(jar("codenameone-core.jar", "com/codename1/ui/C", 3, 0));
        assertEquals(DependencyClassifier.Kind.PLATFORM, core.kind());

        File cn1lib = CompatRemapperTest.jar(new File(tmp.newFolder(), "some-cn1lib.jar"), "org/lib/Maps.class",
                cls("org/lib/Maps", uses("com/codename1/ui/Display")));
        assertEquals("Codename One library", one(cn1lib).platform());

        File resources = CompatRemapperTest.jar(new File(tmp.newFolder(), "icons.jar"), "icons/a.png", new byte[] {1},
                "META-INF/versions/9/module-info.class", new byte[] {1});
        assertEquals(DependencyClassifier.Kind.NO_CLASSES, one(resources).kind());
        assertFalse(one(resources).managed());
    }

    @Test
    public void nativeCodeIsFoundThreeWays() throws Exception {
        assertEquals("ships native libraries", one(jar("a.jar", "org/a/C", 2, 0, "win32-x86-64/a.dll",
                new byte[] {1})).nativeCode());
        assertEquals("calls native code through JNA", one(CompatRemapperTest.jar(new File(tmp.newFolder(), "b.jar"),
                "org/b/C.class", cls("org/b/C", uses("com/sun/jna/Native")))).nativeCode());
        assertNull(one(jar("c.jar", "org/c/C", 2, 0)).nativeCode());

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "org/d/C", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitLdcInsn("d");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "loadLibrary", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        assertEquals("loads native code", one(CompatRemapperTest.jar(new File(tmp.newFolder(), "d.jar"),
                "org/d/C.class", cw.toByteArray())).nativeCode());
    }

    @Test
    public void onlyWhatIsReachedShipsOfAPureJavaLibrary() throws Exception {
        File pure = CompatRemapperTest.jar(new File(tmp.newFolder(), "pure.jar"),
                "org/pure/A.class", cls("org/pure/A", uses("org/pure/B")),
                "org/pure/B.class", cls("org/pure/B", uses(), "org.pure.ByName", "not a class name"),
                "org/pure/ByName.class", cls("org/pure/ByName", uses("org/other/X")),
                "org/pure/Unused.class", cls("org/pure/Unused", uses("org/pure/A")));
        File other = CompatRemapperTest.jar(new File(tmp.newFolder(), "other.jar"),
                "org/other/X.class", cls("org/other/X", uses()),
                "org/other/Y.class", cls("org/other/Y", uses()));
        File ui = jar("ui.jar", "org/ui/C", 5, 5);
        File never = jar("never.jar", "org/never/C", 3, 0);
        List<DependencyClassifier.Library> libs = DependencyClassifier.classify(Arrays.asList(pure, other, ui, never),
                LAYERS);

        // Four classes by reference and the one the toolkit library was entered through.
        assertEquals(5, DependencyClassifier.reach(Arrays.asList("org/pure/A", "org.ui.C3", "java/lang/String"),
                libs));
        // A is used, B through A, ByName through the string B loads it by, and
        // through ByName a class of another jar.
        assertEquals(new TreeSet<String>(Arrays.asList("org/pure/A", "org/pure/B", "org/pure/ByName")),
                libs.get(0).shipped());
        assertEquals(Collections.singleton("org/other/X"), libs.get(1).shipped());
        // A toolkit library that is reached at all ships whole.
        assertEquals(5, libs.get(2).shipped().size());
        assertTrue(libs.get(3).shipped().isEmpty());
        assertTrue(libs.get(3).managed());
    }

    @Test
    public void theFirstJarOnTheClassPathOwnsAClassTwoHold() throws Exception {
        File first = CompatRemapperTest.jar(new File(tmp.newFolder(), "first.jar"), "org/dup/A.class",
                cls("org/dup/A", uses()));
        File second = CompatRemapperTest.jar(new File(tmp.newFolder(), "second.jar"), "org/dup/A.class",
                cls("org/dup/A", uses()));
        List<DependencyClassifier.Library> libs = DependencyClassifier.classify(Arrays.asList(first, second, first,
                new File("no-such.jar")), LAYERS);
        assertEquals("A jar is read once, and a missing one not at all", 2, libs.size());
        DependencyClassifier.reach(Collections.singleton("org/dup/A"), libs);
        assertEquals(1, libs.get(0).shipped().size());
        assertTrue(libs.get(1).shipped().isEmpty());
    }

    @Test
    public void anApplicationsClassesAndSourcesBothSeedTheWalk() throws Exception {
        File pure = CompatRemapperTest.jar(new File(tmp.newFolder(), "pure.jar"),
                "org/pure/A.class", cls("org/pure/A", uses()),
                "org/pure/util/B.class", cls("org/pure/util/B", uses()),
                "org/pure/util/C.class", cls("org/pure/util/C", uses()),
                "org/pure/D.class", cls("org/pure/D", uses()));
        File classes = tmp.newFolder();
        File main = new File(classes, "com/acme/Main.class");
        assertTrue(main.getParentFile().mkdirs());
        Files.write(main.toPath(), cls("com/acme/Main", uses("org/pure/A")));
        File unpacked = new File(classes, "org/pure/D.class");
        assertTrue(unpacked.getParentFile().mkdirs());
        Files.write(unpacked.toPath(), cls("org/pure/D", uses("org/pure/util/B")));

        // What an earlier run unpacked is not the application speaking.
        java.util.Set<String> refs = DependencyClassifier.applicationReferences(classes,
                Collections.singleton("org/pure/D.class"), LAYERS);
        assertTrue(refs.toString(), refs.contains("org/pure/A"));
        assertFalse(refs.toString(), refs.contains("org/pure/util/B"));

        File sources = tmp.newFolder();
        Files.write(new File(sources, "Main.java").toPath(), ("package com.acme;\n"
                + "import org.pure.A;\n"
                + "import static org.pure.D.run;\n"
                + "import org.pure.util.*;\n"
                + "class Main { }\n").getBytes("UTF-8"));
        List<DependencyClassifier.Library> libs = DependencyClassifier.classify(Collections.singletonList(pure),
                LAYERS);
        DependencyClassifier.reach(DependencyClassifier.sourceReferences(sources, libs), libs);
        assertEquals(new TreeSet<String>(Arrays.asList("org/pure/A", "org/pure/D", "org/pure/util/B",
                "org/pure/util/C")), libs.get(0).shipped());
    }
}
