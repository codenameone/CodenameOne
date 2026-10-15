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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.build.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.*;

public class OrmEnhancerTest {
    private static final String MANIFEST = "META-INF/cn1/orm-enhanced-dependencies.list";

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void enhancesAndCleansNestedDependencyWithoutChangingJar() throws Exception {
        Path output = temporary.newFolder("classes").toPath();
        ProcessorContext context = context(output, "overlay/Reader.class");
        Path jar = new File(context.getCompileClasspath().get(0)).toPath();
        byte[] originalJar = Files.readAllBytes(jar);

        OrmEnhancer.enhance(entities(), context);
        assertFalse(Arrays.equals(reader("overlay/Reader"), Files.readAllBytes(output.resolve("overlay/Reader.class"))));
        assertTrue(Files.isRegularFile(output.resolve(MANIFEST)));
        assertEquals(Collections.singleton("overlay.Reader"), OrmEnhancer.prepare(context));
        assertFalse(Files.exists(output.resolve("overlay/Reader.class")));
        assertFalse(Files.exists(output.resolve(MANIFEST)));
        assertArrayEquals(originalJar, Files.readAllBytes(jar));
    }

    @Test
    public void rejectsTraversalAndAbsoluteArchiveNames() throws Exception {
        String[] names = {"../outside/Reader.class", "/outside/Reader.class",
                "..\\outside\\Reader.class", "C:\\outside\\Reader.class", "C:/outside/Reader.class"};
        for (String name : names) {
            final ProcessorContext context = context(temporary.newFolder().toPath(), name);
            rejects(new IoAction() {
                public void run() throws IOException { OrmEnhancer.enhance(entities(), context); }
            });
        }
    }

    @Test
    public void rejectsOutputDirectorySymlinkToSiblingWithSamePrefix() throws Exception {
        Path output = temporary.newFolder("classes").toPath();
        Path outside = temporary.newFolder("classes-escape").toPath();
        Files.createSymbolicLink(output.resolve("overlay"), outside);
        final ProcessorContext context = context(output, "overlay/nested/Reader.class");

        rejects(new IoAction() {
            public void run() throws IOException { OrmEnhancer.enhance(entities(), context); }
        });
        assertFalse(Files.exists(outside.resolve("nested/Reader.class")));
    }

    @Test
    public void rejectsManifestDirectorySymlink() throws Exception {
        Path output = temporary.newFolder("classes").toPath();
        Path outside = temporary.newFolder("outside").toPath();
        Files.createSymbolicLink(output.resolve("META-INF"), outside);
        final ProcessorContext context = context(output, "overlay/Reader.class");

        rejects(new IoAction() {
            public void run() throws IOException { OrmEnhancer.enhance(entities(), context); }
        });
        assertFalse(Files.exists(outside.resolve("cn1/orm-enhanced-dependencies.list")));
    }

    @Test
    public void cleanupDoesNotDeleteThroughOutputSymlink() throws Exception {
        Path output = temporary.newFolder("classes").toPath();
        Path outside = temporary.newFolder("outside").toPath();
        final ProcessorContext context = context(output, "overlay/Reader.class");
        OrmEnhancer.enhance(entities(), context);
        Path externalClass = outside.resolve("Reader.class");
        Files.move(output.resolve("overlay/Reader.class"), externalClass);
        byte[] original = Files.readAllBytes(externalClass);
        Files.delete(output.resolve("overlay"));
        Files.createSymbolicLink(output.resolve("overlay"), outside);

        rejects(new IoAction() {
            public void run() throws IOException { OrmEnhancer.prepare(context); }
        });
        assertArrayEquals(original, Files.readAllBytes(externalClass));
    }

    @Test
    public void cleanupDoesNotReadOrDeleteExternalManifest() throws Exception {
        Path output = temporary.newFolder("classes").toPath();
        Path outside = temporary.newFolder("outside").toPath();
        final ProcessorContext context = context(output, "overlay/Reader.class");
        OrmEnhancer.enhance(entities(), context);
        Files.move(output.resolve("META-INF"), outside.resolve("metadata"));
        Path externalManifest = outside.resolve("metadata/cn1/orm-enhanced-dependencies.list");
        byte[] original = Files.readAllBytes(externalManifest);
        Files.createSymbolicLink(output.resolve("META-INF"), outside.resolve("metadata"));

        rejects(new IoAction() {
            public void run() throws IOException { OrmEnhancer.prepare(context); }
        });
        assertArrayEquals(original, Files.readAllBytes(externalManifest));
        assertTrue(Files.exists(output.resolve("overlay/Reader.class")));
    }

    @Test
    public void routesGeneratedAccessToAPrivateInheritedFieldOfADependencyThroughAWovenAccessor() throws Exception {
        // model.Base declares a private id; model.Thing extends it; both live in a
        // dependency jar. The generated model, compiled against public stand-ins,
        // reads the field through Thing -- the static type javac names.
        Path output = temporary.newFolder("classes").toPath();
        File jar = temporary.newFile("entities.jar");
        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            stream.putNextEntry(new JarEntry("model/Base.class"));
            stream.write(entityClass("model/Base", "java/lang/Object", true));
            stream.closeEntry();
            stream.putNextEntry(new JarEntry("model/Thing.class"));
            stream.write(entityClass("model/Thing", "model/Base", false));
            stream.closeEntry();
        }
        Files.createDirectories(output.resolve("model"));
        Files.write(output.resolve("model/ThingCn1BackendModel.class"), generatedModel());
        ProcessorContext context = new ProcessorContext(output.toFile(), temporary.newFolder(), null,
                new SystemStreamLog(), null, null, null, Collections.<String>emptyList(), "UTF-8",
                Collections.singletonList(jar.getAbsolutePath()));
        OrmAnnotationProcessor.EntityClass thing = new OrmAnnotationProcessor.EntityClass();
        thing.binaryName = "model.Thing";
        thing.daoBinaryName = "model.ThingCn1BackendDao";
        thing.hiddenFields.add(new String[] {"model/Base", "id", "J"});

        File stubs = OrmEnhancer.publicStubs(Collections.singletonMap(thing.binaryName, thing), context);
        try {
            assertTrue(Files.isRegularFile(stubs.toPath().resolve("model/Base.class")));
            final int[] access = {0};
            new org.objectweb.asm.ClassReader(Files.readAllBytes(stubs.toPath().resolve("model/Base.class")))
                    .accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
                        @Override public org.objectweb.asm.FieldVisitor visitField(int a, String n, String d, String sig, Object v) {
                            if (n.equals("id")) access[0] = a;
                            return null;
                        }
                    }, 0);
            assertEquals(Opcodes.ACC_PUBLIC, access[0] & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE));
        } finally {
            org.apache.commons.io.FileUtils.deleteDirectory(stubs);
        }

        OrmEnhancer.enhance(Collections.singletonMap(thing.binaryName, thing), context);
        // The declaring class is woven and written over the dependency's copy;
        // the subclass, which declares nothing hidden, is left alone.
        assertTrue(Files.isRegularFile(output.resolve("model/Base.class")));
        assertFalse(Files.exists(output.resolve("model/Thing.class")));
        java.net.URLClassLoader loader = new java.net.URLClassLoader(
                new java.net.URL[] {output.toUri().toURL(), jar.toURI().toURL()}, null);
        try {
            Class<?> type = loader.loadClass("model.Thing");
            Object instance = type.newInstance();
            Class<?> model = loader.loadClass("model.ThingCn1BackendModel");
            model.getMethod("write", type, long.class).invoke(null, instance, Long.valueOf(42L));
            assertEquals(Long.valueOf(42L), model.getMethod("read", type).invoke(null, instance));
            assertTrue(java.lang.reflect.Modifier.isPrivate(
                    loader.loadClass("model.Base").getDeclaredField("id").getModifiers()));
        } finally {
            loader.close();
        }
    }

    /// A class with a private long `id` when `declaresId`, and a public no-arg
    /// constructor.
    private static byte[] entityClass(String name, String superName, boolean declaresId) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, superName, null);
        if (declaresId) {
            writer.visitField(Opcodes.ACC_PRIVATE, "id", "J", null, null).visitEnd();
        }
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    /// What javac emits for `e.id` and `e.id = v` against the public stand-in.
    private static byte[] generatedModel() {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "model/ThingCn1BackendModel", null, "java/lang/Object", null);
        MethodVisitor read = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "read",
                "(Lmodel/Thing;)J", null, null);
        read.visitCode();
        read.visitVarInsn(Opcodes.ALOAD, 0);
        read.visitFieldInsn(Opcodes.GETFIELD, "model/Thing", "id", "J");
        read.visitInsn(Opcodes.LRETURN);
        read.visitMaxs(0, 0);
        read.visitEnd();
        MethodVisitor write = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "write",
                "(Lmodel/Thing;J)V", null, null);
        write.visitCode();
        write.visitVarInsn(Opcodes.ALOAD, 0);
        write.visitVarInsn(Opcodes.LLOAD, 1);
        write.visitFieldInsn(Opcodes.PUTFIELD, "model/Thing", "id", "J");
        write.visitInsn(Opcodes.RETURN);
        write.visitMaxs(0, 0);
        write.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private ProcessorContext context(Path output, String entry) throws IOException {
        File jar = temporary.newFile("dependency-" + System.nanoTime() + ".jar");
        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            stream.putNextEntry(new JarEntry(entry));
            stream.write(reader(entry.substring(0, entry.length() - 6)));
            stream.closeEntry();
        }
        return new ProcessorContext(output.toFile(), temporary.newFolder(), null, new SystemStreamLog(),
                null, null, null, Collections.<String>emptyList(), "UTF-8",
                Collections.singletonList(jar.getAbsolutePath()));
    }

    private static Map<String, OrmAnnotationProcessor.EntityClass> entities() {
        OrmAnnotationProcessor.EntityClass child = new OrmAnnotationProcessor.EntityClass();
        child.binaryName = "model.Child";
        OrmAnnotationProcessor.RelationField relation = new OrmAnnotationProcessor.RelationField();
        relation.field = "parent";
        relation.declaringType = "model.Child";
        relation.descriptor = "Lmodel/Parent;";
        child.relations.add(relation);
        return Collections.singletonMap(child.binaryName, child);
    }

    private static byte[] reader(String name) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "read",
                "(Lmodel/Child;)Lmodel/Parent;", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, "model/Child", "parent", "Lmodel/Parent;");
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private interface IoAction { void run() throws IOException; }

    private static void rejects(IoAction action) throws IOException {
        try {
            action.run();
            fail("Expected unsafe enhancement path to be rejected");
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("Invalid")
                    || expected.getMessage().contains("escapes"));
        }
    }
}
