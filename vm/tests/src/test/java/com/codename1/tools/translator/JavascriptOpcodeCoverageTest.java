package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptOpcodeCoverageTest {

    @Test
    void translatesStackAndPrimitiveCoverageFixture() throws Exception {
        Parser.cleanup();

        Path classesDir = Files.createTempDirectory("js-opcode-classes");
        writeCoverageClass(classesDir.resolve("JsOpcodeCoverage.class"));

        Path outputDir = Files.createTempDirectory("js-opcode-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsOpcodeCoverage");

        Path distDir = outputDir.resolve("dist").resolve("JsOpcodeCoverage-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("stackFamily"), "Coverage fixture should translate stack-family methods");
        assertTrue(translatedApp.contains("primitiveComparisons"), "Coverage fixture should translate primitive compare methods");
    }

    @Test
    void translatesMonitorAndWaitCoverageFixture() throws Exception {
        Parser.cleanup();

        Path classesDir = Files.createTempDirectory("js-monitor-classes");
        writeMonitorCoverageClass(classesDir.resolve("JsMonitorCoverage.class"));

        Path outputDir = Files.createTempDirectory("js-monitor-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsMonitorCoverage");

        Path distDir = outputDir.resolve("dist").resolve("JsMonitorCoverage-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        String runtime = new String(Files.readAllBytes(distDir.resolve("parparvm_runtime.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("monitorBlock"), "Coverage fixture should translate monitorenter/monitorexit methods");
        assertTrue(translatedApp.contains("waitAndNotify"), "Coverage fixture should translate wait/notify methods");
        assertTrue(translatedApp.contains("sleepOnce"), "Coverage fixture should translate sleep methods");
        assertTrue(runtime.contains("waitOn(thread, obj, timeout)"), "Runtime should expose cooperative wait support");
        assertTrue(runtime.contains("cn1_java_lang_Object_wait_long_int") || runtime.contains("cn1_java_lang_Object_wait___long_int"),
                "Runtime should expose wait() native support");
        assertTrue(runtime.contains("cn1_java_lang_Object_notifyAll") || runtime.contains("cn1_java_lang_Object_notifyAll__"),
                "Runtime should expose notifyAll() native support");
        assertTrue(runtime.contains("cn1_java_lang_Thread_sleep_long") || runtime.contains("cn1_java_lang_Thread_sleep___long"),
                "Runtime should expose sleep() native support");
    }

    @Test
    void translatesObjectTypeAndDispatchCoverageFixture() throws Exception {
        Parser.cleanup();

        Path classesDir = Files.createTempDirectory("js-type-classes");
        writeInterfaceClass(classesDir.resolve("JsTypeIface.class"));
        writeBaseClass(classesDir.resolve("JsTypeBase.class"));
        writeImplClass(classesDir.resolve("JsTypeImpl.class"));
        writeTypeCoverageClass(classesDir.resolve("JsTypeCoverage.class"));

        Path outputDir = Files.createTempDirectory("js-type-output");
        JavascriptTargetIntegrationTest.runJavascriptTranslator(classesDir, outputDir, "JsTypeCoverage");

        Path distDir = outputDir.resolve("dist").resolve("JsTypeCoverage-js");
        String translatedApp = new String(Files.readAllBytes(distDir.resolve("translated_app.js")), StandardCharsets.UTF_8);
        String runtime = new String(Files.readAllBytes(distDir.resolve("parparvm_runtime.js")), StandardCharsets.UTF_8);

        assertTrue(translatedApp.contains("castsAndTypes"), "Coverage fixture should translate CHECKCAST/INSTANCEOF methods");
        assertTrue(translatedApp.contains("dispatch"), "Coverage fixture should translate virtual/interface dispatch methods");
        assertTrue(translatedApp.contains("jvm.getClassObject(\"JsTypeImpl\")"), "Coverage fixture should translate class literals");
        assertTrue(translatedApp.contains("assignableTo: {")
                        && translatedApp.contains("\"JsTypeImpl\": true")
                        && translatedApp.contains("\"JsTypeBase\": true")
                        && translatedApp.contains("\"JsTypeIface\": true"),
                "Class metadata should include static assignability information");
        assertTrue(translatedApp.contains("const __classDef = __target.__classDef;")
                        && translatedApp.contains("(__classDef && __classDef.methods) ? __classDef.methods["),
                "Virtual/interface dispatch should use an exact-class method-table fast path");
        assertTrue(translatedApp.contains("jvm.resolveVirtual(__target.__class"), "Dispatch should retain inheritance/interface fallback");
        assertTrue(translatedApp.contains("__class !== \"JsTypeImpl\"")
                        && translatedApp.contains("__classDef.assignableTo[\"JsTypeImpl\"]"),
                "CHECKCAST should inline the exact-class and assignability check");
        assertTrue(translatedApp.contains("__class === \"JsTypeImpl\"")
                        && translatedApp.contains(".__classDef.assignableTo[\"JsTypeImpl\"]"),
                "INSTANCEOF should inline the exact-class and assignability check");
        assertTrue(!translatedApp.contains("jvm.instanceOf("),
                "Translated object/type checks should avoid the generic runtime instanceof helper");
        assertTrue(runtime.contains("resolveVirtual(className, methodId)"), "Runtime should resolve virtual methods by class name");
        assertTrue(runtime.contains("resolvedVirtualCache: Object.create(null)")
                        && runtime.contains("remappedMethodIdCache: Object.create(null)"),
                "Runtime virtual dispatch should keep per-method caches for resolved and remapped ids");
        assertTrue(runtime.contains("const cacheKey = className + \"|\" + methodId;")
                        && runtime.contains("const remappedId = this.remappedMethodId(current, methodId, tail);"),
                "Runtime virtual dispatch should cache both resolved lookups and remapped owner-specific ids");
        assertTrue(runtime.contains("obj.__classDef.assignableTo[className]"), "Runtime instanceof should use emitted class assignability tables");
        assertTrue(runtime.contains("errorClass === entry.type || (errorClassDef && errorClassDef.assignableTo && errorClassDef.assignableTo[entry.type])"),
                "Runtime exception matching should use direct class and assignability checks");
        assertTrue(runtime.contains("arrayAssignableTo(componentClass, dimensions)") && runtime.contains("isPrimitiveComponent(componentClass)"),
                "Runtime should keep array assignability limited to CN1-relevant cases");
    }

    private static void writeCoverageClass(Path target) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "JsOpcodeCoverage", null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor main = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.visitCode();
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsOpcodeCoverage", "stackFamily", "()V", false);
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsOpcodeCoverage", "primitiveComparisons", "()V", false);
        main.visitInsn(Opcodes.RETURN);
        main.visitMaxs(0, 0);
        main.visitEnd();

        MethodVisitor stack = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "stackFamily", "()V", null, null);
        stack.visitCode();
        stack.visitInsn(Opcodes.ICONST_1);
        stack.visitInsn(Opcodes.ICONST_2);
        stack.visitInsn(Opcodes.DUP_X1);
        stack.visitInsn(Opcodes.POP);
        stack.visitInsn(Opcodes.POP2);

        stack.visitInsn(Opcodes.ICONST_1);
        stack.visitInsn(Opcodes.ICONST_2);
        stack.visitInsn(Opcodes.ICONST_3);
        stack.visitInsn(Opcodes.DUP_X2);
        stack.visitInsn(Opcodes.POP);
        stack.visitInsn(Opcodes.POP2);
        stack.visitInsn(Opcodes.POP);

        stack.visitInsn(Opcodes.ICONST_1);
        stack.visitInsn(Opcodes.ICONST_2);
        stack.visitInsn(Opcodes.DUP2);
        stack.visitInsn(Opcodes.POP2);
        stack.visitInsn(Opcodes.POP2);

        stack.visitInsn(Opcodes.ICONST_1);
        stack.visitInsn(Opcodes.ICONST_2);
        stack.visitInsn(Opcodes.ICONST_3);
        stack.visitInsn(Opcodes.DUP2_X1);
        stack.visitInsn(Opcodes.POP);
        stack.visitInsn(Opcodes.POP2);
        stack.visitInsn(Opcodes.POP2);

        stack.visitInsn(Opcodes.ICONST_1);
        stack.visitInsn(Opcodes.ICONST_2);
        stack.visitInsn(Opcodes.ICONST_3);
        stack.visitInsn(Opcodes.ICONST_4);
        stack.visitInsn(Opcodes.DUP2_X2);
        stack.visitInsn(Opcodes.POP2);
        stack.visitInsn(Opcodes.POP2);
        stack.visitInsn(Opcodes.POP2);

        stack.visitInsn(Opcodes.ICONST_1);
        stack.visitInsn(Opcodes.ICONST_2);
        stack.visitInsn(Opcodes.SWAP);
        stack.visitInsn(Opcodes.POP2);
        stack.visitInsn(Opcodes.RETURN);
        stack.visitMaxs(0, 0);
        stack.visitEnd();

        MethodVisitor primitive = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "primitiveComparisons", "()V", null, null);
        primitive.visitCode();
        primitive.visitInsn(Opcodes.FCONST_0);
        primitive.visitInsn(Opcodes.FCONST_1);
        primitive.visitInsn(Opcodes.FCMPL);
        primitive.visitInsn(Opcodes.POP);
        primitive.visitInsn(Opcodes.FCONST_2);
        primitive.visitInsn(Opcodes.FCONST_1);
        primitive.visitInsn(Opcodes.FCMPG);
        primitive.visitInsn(Opcodes.POP);
        primitive.visitInsn(Opcodes.DCONST_0);
        primitive.visitInsn(Opcodes.DCONST_1);
        primitive.visitInsn(Opcodes.DCMPL);
        primitive.visitInsn(Opcodes.POP);
        primitive.visitInsn(Opcodes.DCONST_1);
        primitive.visitInsn(Opcodes.DCONST_0);
        primitive.visitInsn(Opcodes.DCMPG);
        primitive.visitInsn(Opcodes.POP);
        primitive.visitInsn(Opcodes.LCONST_0);
        primitive.visitInsn(Opcodes.LCONST_1);
        primitive.visitInsn(Opcodes.LCMP);
        primitive.visitInsn(Opcodes.POP);
        primitive.visitInsn(Opcodes.RETURN);
        primitive.visitMaxs(0, 0);
        primitive.visitEnd();

        cw.visitEnd();
        Files.write(target, cw.toByteArray());
    }

    private static void writeMonitorCoverageClass(Path target) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "JsMonitorCoverage", null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor main = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, new String[]{"java/lang/Exception"});
        main.visitCode();
        main.visitTypeInsn(Opcodes.NEW, "java/lang/Object");
        main.visitInsn(Opcodes.DUP);
        main.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        main.visitVarInsn(Opcodes.ASTORE, 1);
        main.visitVarInsn(Opcodes.ALOAD, 1);
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsMonitorCoverage", "monitorBlock", "(Ljava/lang/Object;)V", false);
        main.visitVarInsn(Opcodes.ALOAD, 1);
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsMonitorCoverage", "waitAndNotify", "(Ljava/lang/Object;)V", false);
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsMonitorCoverage", "sleepOnce", "()V", false);
        main.visitInsn(Opcodes.RETURN);
        main.visitMaxs(0, 0);
        main.visitEnd();

        MethodVisitor monitor = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "monitorBlock", "(Ljava/lang/Object;)V", null, null);
        monitor.visitCode();
        monitor.visitVarInsn(Opcodes.ALOAD, 0);
        monitor.visitInsn(Opcodes.MONITORENTER);
        monitor.visitVarInsn(Opcodes.ALOAD, 0);
        monitor.visitInsn(Opcodes.MONITOREXIT);
        monitor.visitInsn(Opcodes.RETURN);
        monitor.visitMaxs(0, 0);
        monitor.visitEnd();

        MethodVisitor waitNotify = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "waitAndNotify", "(Ljava/lang/Object;)V", null, new String[]{"java/lang/InterruptedException"});
        waitNotify.visitCode();
        waitNotify.visitVarInsn(Opcodes.ALOAD, 0);
        waitNotify.visitInsn(Opcodes.MONITORENTER);
        waitNotify.visitVarInsn(Opcodes.ALOAD, 0);
        waitNotify.visitInsn(Opcodes.LCONST_0);
        waitNotify.visitInsn(Opcodes.ICONST_0);
        waitNotify.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "wait", "(JI)V", false);
        waitNotify.visitVarInsn(Opcodes.ALOAD, 0);
        waitNotify.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
        waitNotify.visitVarInsn(Opcodes.ALOAD, 0);
        waitNotify.visitInsn(Opcodes.MONITOREXIT);
        waitNotify.visitInsn(Opcodes.RETURN);
        waitNotify.visitMaxs(0, 0);
        waitNotify.visitEnd();

        MethodVisitor sleep = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "sleepOnce", "()V", null, new String[]{"java/lang/InterruptedException"});
        sleep.visitCode();
        sleep.visitInsn(Opcodes.LCONST_1);
        sleep.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
        sleep.visitInsn(Opcodes.RETURN);
        sleep.visitMaxs(0, 0);
        sleep.visitEnd();

        cw.visitEnd();
        Files.write(target, cw.toByteArray());
    }

    private static void writeInterfaceClass(Path target) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                "JsTypeIface", null, "java/lang/Object", null);

        MethodVisitor call = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "call", "()I", null, null);
        call.visitEnd();

        cw.visitEnd();
        Files.write(target, cw.toByteArray());
    }

    private static void writeBaseClass(Path target) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "JsTypeBase", null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor value = cw.visitMethod(Opcodes.ACC_PUBLIC, "value", "()I", null, null);
        value.visitCode();
        value.visitIntInsn(Opcodes.BIPUSH, 7);
        value.visitInsn(Opcodes.IRETURN);
        value.visitMaxs(0, 0);
        value.visitEnd();

        cw.visitEnd();
        Files.write(target, cw.toByteArray());
    }

    private static void writeImplClass(Path target) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "JsTypeImpl", null, "JsTypeBase", new String[]{"JsTypeIface"});

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "JsTypeBase", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor value = cw.visitMethod(Opcodes.ACC_PUBLIC, "value", "()I", null, null);
        value.visitCode();
        value.visitIntInsn(Opcodes.BIPUSH, 11);
        value.visitInsn(Opcodes.IRETURN);
        value.visitMaxs(0, 0);
        value.visitEnd();

        MethodVisitor call = cw.visitMethod(Opcodes.ACC_PUBLIC, "call", "()I", null, null);
        call.visitCode();
        call.visitIntInsn(Opcodes.BIPUSH, 13);
        call.visitInsn(Opcodes.IRETURN);
        call.visitMaxs(0, 0);
        call.visitEnd();

        cw.visitEnd();
        Files.write(target, cw.toByteArray());
    }

    private static void writeTypeCoverageClass(Path target) throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "JsTypeCoverage", null, "java/lang/Object", null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor main = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.visitCode();
        main.visitTypeInsn(Opcodes.NEW, "JsTypeImpl");
        main.visitInsn(Opcodes.DUP);
        main.visitMethodInsn(Opcodes.INVOKESPECIAL, "JsTypeImpl", "<init>", "()V", false);
        main.visitVarInsn(Opcodes.ASTORE, 1);
        main.visitVarInsn(Opcodes.ALOAD, 1);
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsTypeCoverage", "castsAndTypes", "(Ljava/lang/Object;)V", false);
        main.visitMethodInsn(Opcodes.INVOKESTATIC, "JsTypeCoverage", "dispatch", "()V", false);
        main.visitInsn(Opcodes.RETURN);
        main.visitMaxs(0, 0);
        main.visitEnd();

        MethodVisitor casts = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "castsAndTypes", "(Ljava/lang/Object;)V", null, null);
        casts.visitCode();
        casts.visitVarInsn(Opcodes.ALOAD, 0);
        casts.visitTypeInsn(Opcodes.CHECKCAST, "JsTypeImpl");
        casts.visitInsn(Opcodes.POP);
        casts.visitVarInsn(Opcodes.ALOAD, 0);
        casts.visitTypeInsn(Opcodes.INSTANCEOF, "JsTypeImpl");
        casts.visitInsn(Opcodes.POP);
        casts.visitLdcInsn(org.objectweb.asm.Type.getObjectType("JsTypeImpl"));
        casts.visitInsn(Opcodes.POP);
        casts.visitInsn(Opcodes.RETURN);
        casts.visitMaxs(0, 0);
        casts.visitEnd();

        MethodVisitor dispatch = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "dispatch", "()V", null, null);
        dispatch.visitCode();
        dispatch.visitTypeInsn(Opcodes.NEW, "JsTypeImpl");
        dispatch.visitInsn(Opcodes.DUP);
        dispatch.visitMethodInsn(Opcodes.INVOKESPECIAL, "JsTypeImpl", "<init>", "()V", false);
        dispatch.visitVarInsn(Opcodes.ASTORE, 0);
        dispatch.visitVarInsn(Opcodes.ALOAD, 0);
        dispatch.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "JsTypeBase", "value", "()I", false);
        dispatch.visitVarInsn(Opcodes.ALOAD, 0);
        dispatch.visitMethodInsn(Opcodes.INVOKEINTERFACE, "JsTypeIface", "call", "()I", true);
        dispatch.visitInsn(Opcodes.IADD);
        dispatch.visitInsn(Opcodes.POP);
        dispatch.visitInsn(Opcodes.RETURN);
        dispatch.visitMaxs(0, 0);
        dispatch.visitEnd();

        cw.visitEnd();
        Files.write(target, cw.toByteArray());
    }
}
