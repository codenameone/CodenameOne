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
    public void generatesValidatorForAnnotatedFields() throws Exception {
        File classes = compileFixture(
                "com.example.SignupModel",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Bindable\n"
                        + "public class SignupModel {\n"
                        + "    @Bind(name=\"emailField\") @Required @Email\n"
                        + "    public String email;\n"
                        + "    @Bind(name=\"ageField\") @Numeric(min = 13, max = 120)\n"
                        + "    public String age;\n"
                        + "    @Bind(name=\"phoneField\") @Regex(pattern=\"^[0-9]+$\", message=\"digits only\")\n"
                        + "    public String phone;\n"
                        + "    @Bind(name=\"siteField\") @Url\n"
                        + "    public String site;\n"
                        + "    @Bind(name=\"roleField\") @ExistIn({\"admin\", \"viewer\"})\n"
                        + "    public String role;\n"
                        + "    @Bind(name=\"lengthField\") @Length(min = 8)\n"
                        + "    public String secret;\n"
                        + "    public SignupModel() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);

        // The binder source is generated then compiled in-memory. Probe
        // the constant pool of the resulting .class for the type / method
        // references each constraint variant should emit.
        String pool = readClassConstantPool(classes,
                "com/example/SignupModelCn1Binder");
        assertTrue("validator type referenced",
                pool.contains("com/codename1/ui/validation/Validator"));
        assertTrue("addConstraint method referenced",
                pool.contains("addConstraint"));
        // getValidator() lives on the anonymous NotifiableBinding subclass
        // (`<binder>$1.class`), not on the outer binder. Scan the binder
        // class + every anonymous inner.
        String allPool = readBinderConstantPool(classes, "com/example/SignupModelCn1Binder");
        assertTrue("getValidator method emitted on Binding impl",
                allPool.contains("getValidator"));
        assertTrue("@Required / @Length both reference LengthConstraint",
                pool.contains("com/codename1/ui/validation/LengthConstraint"));
        assertTrue("@Email / @Regex reference RegexConstraint",
                pool.contains("com/codename1/ui/validation/RegexConstraint"));
        assertTrue("@Email reaches the validEmail factory",
                pool.contains("validEmail"));
        assertTrue("@Url reaches the validURL factory",
                pool.contains("validURL"));
        assertTrue("@Numeric references NumericConstraint",
                pool.contains("com/codename1/ui/validation/NumericConstraint"));
        assertTrue("@Regex pattern survives into constant pool",
                pool.contains("^[0-9]+$"));
        assertTrue("@ExistIn references ExistInConstraint",
                pool.contains("com/codename1/ui/validation/ExistInConstraint"));
        assertTrue("@ExistIn vocabulary survives into constant pool",
                pool.contains("admin") && pool.contains("viewer"));
    }

    @Test
    public void emptyValidatorWhenNoConstraintAnnotations() throws Exception {
        File classes = compileFixture(
                "com.example.NoValidation",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Bindable\n"
                        + "public class NoValidation {\n"
                        + "    @Bind(name=\"x\") public String x;\n"
                        + "    public NoValidation() {}\n"
                        + "}\n");
        runProcessorOrFail(classes);
        String pool = readClassConstantPool(classes,
                "com/example/NoValidationCn1Binder");
        // The validator must still be there so getValidator() never
        // returns null, but no constraint type should be referenced.
        assertTrue("validator type still referenced",
                pool.contains("com/codename1/ui/validation/Validator"));
        String allPool = readBinderConstantPool(classes, "com/example/NoValidationCn1Binder");
        assertTrue("getValidator method still emitted", allPool.contains("getValidator"));
        assertTrue("no LengthConstraint when no @Required/@Length present",
                !pool.contains("com/codename1/ui/validation/LengthConstraint"));
        assertTrue("no RegexConstraint when no @Regex/@Email/@Url present",
                !pool.contains("com/codename1/ui/validation/RegexConstraint"));
    }

    @Test
    public void rejectsEmptyRegexPattern() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.BadRegex",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Bindable public class BadRegex {\n"
                                + "    @Bind(name=\"x\") @Regex(pattern=\"\") public String x;\n"
                                + "    public BadRegex() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on empty @Regex pattern", ctx.hasErrors());
    }

    @Test
    public void rejectsEmptyExistInList() throws Exception {
        File classes = tmp.newFolder("classes");
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.BadExistIn",
                        "package com.example;\n"
                                + "import com.codename1.annotations.*;\n"
                                + "@Bindable public class BadExistIn {\n"
                                + "    @Bind(name=\"x\") @ExistIn({}) public String x;\n"
                                + "    public BadExistIn() {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir()));
        ProcessorContext ctx = runProcessor(classes);
        assertTrue("expected error on empty @ExistIn value list", ctx.hasErrors());
    }

    @Test
    public void customValidateAnnotationEmitsNewExpression() throws Exception {
        // Two top-level fixtures in the same package so the generated binder
        // can reference the custom constraint by binary name.
        File classes = tmp.newFolder("classes");
        java.util.Map<String, String> sources = new java.util.LinkedHashMap<String, String>();
        sources.put("com.example.PhoneConstraint",
                "package com.example;\n"
                        + "import com.codename1.ui.validation.Constraint;\n"
                        + "public class PhoneConstraint implements Constraint {\n"
                        + "    public boolean isValid(Object v) { return true; }\n"
                        + "    public String getDefaultFailMessage() { return \"\"; }\n"
                        + "}\n");
        sources.put("com.example.PhoneHolder",
                "package com.example;\n"
                        + "import com.codename1.annotations.*;\n"
                        + "@Bindable public class PhoneHolder {\n"
                        + "    @Bind(name=\"phone\") @Validate(PhoneConstraint.class)\n"
                        + "    public String phone;\n"
                        + "    public PhoneHolder() {}\n"
                        + "}\n");
        JavaSourceCompiler.compile(sources, classes, Arrays.asList(testClassesDir()));
        runProcessorOrFail(classes);
        String pool = readClassConstantPool(classes,
                "com/example/PhoneHolderCn1Binder");
        // The Validate annotation's class literal is captured as a Type ref
        // and emitted as a direct `new` expression on that class.
        assertTrue("custom constraint type referenced",
                pool.contains("com/example/PhoneConstraint"));
    }

    /// Returns the concatenation of every UTF-8 entry in the class file's
    /// constant pool. The .class binary embeds type / method / String
    /// references as UTF-8 records, so substring matching against this
    /// blob is sufficient for "does the generated binder reference
    /// `com/codename1/ui/validation/LengthConstraint`?" style checks.
    private static String readClassConstantPool(File classesRoot, String internalName) throws Exception {
        File classFile = new File(classesRoot, internalName + ".class");
        if (!classFile.exists()) {
            throw new IllegalStateException("Generated binder class missing: " + classFile);
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(classFile.toPath());
        // ISO-8859-1 round-trips the raw bytes 1:1, so UTF-8 entries inside
        // the constant pool show up as ASCII substrings without needing the
        // full constant-pool walker -- enough for the assertions here.
        return new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    /// Returns the union of every UTF-8 constant pool entry across the
    /// binder class and any inner classes that share its prefix. The
    /// generated binder splits methods across the outer class and one or
    /// more anonymous inner classes (the NotifiableBinding subclass, the
    /// listeners, the disposers); methods like `getValidator` live on the
    /// inner classes so a substring check against just the outer class
    /// would miss them.
    private static String readBinderConstantPool(File classesRoot, String binderInternalName) throws Exception {
        File parent = new File(classesRoot, binderInternalName).getParentFile();
        String prefix = binderInternalName.substring(binderInternalName.lastIndexOf('/') + 1);
        StringBuilder out = new StringBuilder();
        File[] files = parent.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().endsWith(".class")
                        && (f.getName().equals(prefix + ".class")
                                || f.getName().startsWith(prefix + "$"))) {
                    out.append(new String(
                            java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.ISO_8859_1));
                    out.append('\n');
                }
            }
        }
        return out.toString();
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
