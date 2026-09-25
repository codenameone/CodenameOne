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
import org.apache.maven.plugin.logging.SystemStreamLog;
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
