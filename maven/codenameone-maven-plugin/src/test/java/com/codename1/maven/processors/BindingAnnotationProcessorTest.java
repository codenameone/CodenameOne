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

        File binderFile = new File(classes, "com/example/LoginModelCn1Binder.class");
        assertTrue("generated binder file should exist: " + binderFile, binderFile.exists());
        File bootstrapFile = new File(classes, "cn1app/BinderBootstrap.class");
        assertTrue("BinderBootstrap should exist", bootstrapFile.exists());

        Shape shape = readShape(binderFile);
        assertTrue("binder should implement com.codename1.binding.Binder",
                shape.interfaces.contains("com/codename1/binding/Binder"));
        assertTrue("binder should expose type()", shape.methodNames.contains("type"));
        assertTrue("binder should expose bind()", shape.methodNames.contains("bind"));
        assertTrue("binder should expose register() static hook",
                shape.methodNames.contains("register"));
    }

    @Test
    public void resolvesJavaBeansAccessorsOnPrivateField() throws Exception {
        File classes = compileFixture(
                "com.example.PrivateBean",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Bindable\n"
                        + "public class PrivateBean {\n"
                        + "    @Bind(name=\"u\") private String user;\n"
                        + "    public String getUser() { return user; }\n"
                        + "    public void setUser(String u) { this.user = u; }\n"
                        + "    public PrivateBean() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);
        assertTrue(new File(classes, "com/example/PrivateBeanCn1Binder.class").exists());
    }

    @Test
    public void rejectsBindOnPrivateFieldWithoutAccessor() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bad",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Bindable public class Bad {\n"
                                + "    @Bind(name=\"x\") private String x;\n"
                                + "    public Bad() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected validation error on private field without accessor", ctx.hasErrors());
    }

    @Test
    public void instrumentsSetterWithNotifyChanged() throws Exception {
        File classes = compileFixture(
                "com.example.NotifyBean",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Bindable\n"
                        + "public class NotifyBean {\n"
                        + "    @Bind(name=\"u\") private String user;\n"
                        + "    public String getUser() { return user; }\n"
                        + "    public void setUser(String u) { this.user = u; }\n"
                        + "    public NotifyBean() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        // The setter bytes should now contain an INVOKESTATIC of
        // Binders.notifyChanged before the void RETURN.
        File beanFile = new File(classes, "com/example/NotifyBean.class");
        assertTrue(beanFile.exists());
        byte[] bytes = java.nio.file.Files.readAllBytes(beanFile.toPath());
        final boolean[] found = new boolean[1];
        new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor,
                                                                String signature, String[] exceptions) {
                if (!"setUser".equals(name)) {
                    return null;
                }
                return new org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String mname,
                                                 String desc, boolean iface) {
                        if (opcode == org.objectweb.asm.Opcodes.INVOKESTATIC
                                && "com/codename1/binding/Binders".equals(owner)
                                && "notifyChanged".equals(mname)) {
                            found[0] = true;
                        }
                    }
                };
            }
        }, 0);
        assertTrue("setUser should be instrumented with Binders.notifyChanged", found[0]);
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
        // Mirror ProcessAnnotationsMojo's flush step: write emitted
        // bytecode back to disk so the modified class file overlays the
        // original on subsequent file reads.
        for (java.util.Map.Entry<String, byte[]> e : ctx.getEmittedClasses().entrySet()) {
            File target = new File(classesDir, e.getKey() + ".class");
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            java.nio.file.Files.write(target.toPath(), e.getValue());
        }
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
