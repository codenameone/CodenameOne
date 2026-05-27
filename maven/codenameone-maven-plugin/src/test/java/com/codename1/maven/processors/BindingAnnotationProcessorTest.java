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

/// Compiles a `@Bindable` POJO, runs the processor, and asserts the generated
/// `LoginModelBinder` class file is structurally sound (implements the
/// `Binder` interface, has `bind` and `type` methods). Listener-installation
/// and live binding behavior are exercised through the simulator at runtime
/// (out of scope for plugin unit tests).
public class BindingAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void generatesBinderWithExpectedShape() throws Exception {
        File classes = compileFixture(
                "com.example.LoginModel",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "import com.codename1.binding.BindAttr;\n"
                        + "@Bindable\n"
                        + "public class LoginModel {\n"
                        + "    @Bind(name=\"user\", attr=BindAttr.TEXT) public String user;\n"
                        + "    @Bind(name=\"remember\", attr=BindAttr.SELECTED) public boolean remember;\n"
                        + "    @Bind(name=\"banner\", attr=BindAttr.UIID, twoWay=false) public String bannerStyle;\n"
                        + "    public LoginModel() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        File binderFile = new File(classes, "com/codename1/binding/generated/LoginModelBinder.class");
        assertTrue("generated binder file should exist: " + binderFile, binderFile.exists());
        File indexFile = new File(classes, "com/codename1/binding/generated/BindersIndex.class");
        assertTrue("BindersIndex should exist", indexFile.exists());

        Shape shape = readShape(binderFile);
        assertTrue("binder should implement com.codename1.binding.Binder",
                shape.interfaces.contains("com/codename1/binding/Binder"));
        assertTrue("binder should expose type()", shape.methodNames.contains("type"));
        assertTrue("binder should expose bind()", shape.methodNames.contains("bind"));
    }

    @Test
    public void rejectsBindOnPrivateField() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "import com.codename1.binding.BindAttr;\n"
                                + "@Bindable public class Bad {\n"
                                + "    @Bind(name=\"x\") private String x;\n"
                                + "    public Bad() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error on private @Bind field", ctx.hasErrors());
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
        BindingAnnotationProcessor proc = new BindingAnnotationProcessor();
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
        URL url = BindingAnnotationProcessorTest.class.getProtectionDomain()
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
