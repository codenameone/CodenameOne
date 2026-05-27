/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Verifies the `@Entity` processor produces a structurally sound dao for a
/// simple POJO entity, and that the negative cases (missing @Id, relationship
/// fields) surface validation errors instead of silently emitting bad SQL.
public class OrmAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void generatesDaoWithExpectedShape() throws Exception {
        File classes = compileFixture(
                "com.example.User",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Entity(table=\"users\")\n"
                        + "public class User {\n"
                        + "    @Id(autoIncrement=true) public long id;\n"
                        + "    @Column(name=\"full_name\", nullable=false) public String name;\n"
                        + "    public int age;\n"
                        + "    @DbTransient public String tempCache;\n"
                        + "    public User() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        File daoFile = new File(classes, "com/codename1/orm/generated/UserDao.class");
        assertTrue("generated dao file should exist: " + daoFile, daoFile.exists());
        File indexFile = new File(classes, "com/codename1/orm/generated/DaosIndex.class");
        assertTrue("DaosIndex should exist", indexFile.exists());

        Shape shape = readShape(daoFile);
        assertTrue("dao should implement com.codename1.orm.Dao",
                shape.interfaces.contains("com/codename1/orm/Dao"));
        assertTrue(shape.methodNames.contains("createTable"));
        assertTrue(shape.methodNames.contains("insert"));
        assertTrue(shape.methodNames.contains("update"));
        assertTrue(shape.methodNames.contains("delete"));
        assertTrue(shape.methodNames.contains("findById"));
        assertTrue(shape.methodNames.contains("findAll"));
        assertTrue(shape.methodNames.contains("find"));
        assertTrue(shape.methodNames.contains("dropTable"));
        assertTrue(shape.methodNames.contains("attach"));
    }

    @Test
    public void rejectsEntityMissingIdField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.NoId",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Entity public class NoId {\n"
                                + "    public String name;\n"
                                + "    public NoId() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error when @Id is missing", ctx.hasErrors());
    }

    @Test
    public void rejectsEntityWithRelationshipField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Order",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Entity public class Order {\n"
                                + "    @Id public long id;\n"
                                + "    public java.util.List<String> tags;\n"
                                + "    public Order() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error on relationship field", ctx.hasErrors());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private File compileFixture(String fqn, String src) throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, src),
                classes,
                Arrays.asList(testClassesDir()));
        return classes;
    }

    private void runProcessorOrFail(File classesDir) throws Exception {
        ProcessorContext ctx = runProcessor(classesDir);
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("processor reported errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) sb.append(' ').append(e).append('\n');
            fail(sb.toString());
        }
    }

    private ProcessorContext runProcessor(File classesDir) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classesDir);
        OrmAnnotationProcessor proc = new OrmAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classesDir, tmp.newFolder(),
                index, new SystemStreamLog());
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            if (!cls.getClassAnnotations().isEmpty()) proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        return ctx;
    }

    private static File testClassesDir() throws Exception {
        URL url = OrmAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }

    private static Shape readShape(File classFile) throws Exception {
        final Shape shape = new Shape();
        byte[] bytes = Files.readAllBytes(classFile.toPath());
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature,
                              String superName, String[] interfaces) {
                if (interfaces != null) {
                    for (String i : interfaces) shape.interfaces.add(i);
                }
            }

            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name,
                                                                String descriptor, String signature,
                                                                String[] exceptions) {
                shape.methodNames.add(name);
                return null;
            }
        }, ClassReader.SKIP_CODE);
        return shape;
    }

    private static final class Shape {
        final Set<String> interfaces = new LinkedHashSet<String>();
        final Set<String> methodNames = new LinkedHashSet<String>();
    }
}
