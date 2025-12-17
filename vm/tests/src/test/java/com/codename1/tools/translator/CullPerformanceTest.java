package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CullPerformanceTest {

    @Test
    void cullsLargeUnusedGraphQuickly() throws Exception {
        Parser.cleanup();

        Path classesDir = Files.createTempDirectory("cull-stress-classes");
        int totalNodes = 300;
        List<Path> generated = new ArrayList<Path>();
        generated.add(writeStub(classesDir, "java/lang/Object"));

        for (int i = 0; i < totalNodes; i++) {
            generated.add(writeNode(classesDir, i, totalNodes));
        }
        generated.add(writeEntryPoint(classesDir, totalNodes));

        for (Path classFile : generated) {
            Parser.parse(classFile.toFile());
        }

        List<ByteCodeClass> parsedClasses = getParsedClasses();
        for (ByteCodeClass bc : parsedClasses) {
            bc.updateAllDependencies();
        }
        ByteCodeClass.markDependencies(parsedClasses, null);
        List<ByteCodeClass> reachable = ByteCodeClass.clearUnmarked(parsedClasses);
        setParsedClasses(reachable);

        Method eliminateUnusedMethods = Parser.class.getDeclaredMethod("eliminateUnusedMethods");
        eliminateUnusedMethods.setAccessible(true);

        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> eliminateUnusedMethods.invoke(null));

        long remaining = reachable.stream().filter(bc -> !bc.isEliminated()).count();
        assertTrue(remaining < totalNodes / 4, "Most generated classes should be culled to keep optimization fast");
    }

    private Path writeStub(Path classesDir, String internalName) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        String superName = "java/lang/Object".equals(internalName) ? null : "java/lang/Object";
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, superName, null);
        addDefaultConstructor(cw, superName);
        cw.visitEnd();
        return writeClass(classesDir, internalName, cw.toByteArray());
    }

    private Path writeNode(Path classesDir, int index, int totalNodes) throws Exception {
        String name = "com/example/stress/Node" + index;
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);

        addDefaultConstructor(cw, "java/lang/Object");

        MethodVisitor value = cw.visitMethod(Opcodes.ACC_PUBLIC, "value", "(I)I", null, null);
        value.visitCode();
        value.visitVarInsn(Opcodes.ILOAD, 1);
        value.visitIntInsn(Opcodes.SIPUSH, index);
        value.visitInsn(Opcodes.IADD);

        if (index + 1 < totalNodes && index % 3 == 0) {
            String next = "com/example/stress/Node" + (index + 1);
            value.visitTypeInsn(Opcodes.NEW, next);
            value.visitInsn(Opcodes.DUP);
            value.visitMethodInsn(Opcodes.INVOKESPECIAL, next, "<init>", "()V", false);
            value.visitVarInsn(Opcodes.ILOAD, 1);
            value.visitMethodInsn(Opcodes.INVOKEVIRTUAL, next, "value", "(I)I", false);
            value.visitInsn(Opcodes.IADD);
        }

        value.visitInsn(Opcodes.IRETURN);
        value.visitMaxs(3, 2);
        value.visitEnd();

        cw.visitEnd();
        return writeClass(classesDir, name, cw.toByteArray());
    }

    private Path writeEntryPoint(Path classesDir, int totalNodes) throws Exception {
        String name = "com/example/stress/StressEntry";
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);

        addDefaultConstructor(cw, "java/lang/Object");

        MethodVisitor main = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.visitCode();
        main.visitTypeInsn(Opcodes.NEW, "com/example/stress/Node0");
        main.visitInsn(Opcodes.DUP);
        main.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/example/stress/Node0", "<init>", "()V", false);
        main.visitInsn(Opcodes.ICONST_0);
        main.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/example/stress/Node0", "value", "(I)I", false);
        main.visitInsn(Opcodes.POP);
        main.visitInsn(Opcodes.RETURN);
        main.visitMaxs(3, 1);
        main.visitEnd();

        cw.visitEnd();
        return writeClass(classesDir, name, cw.toByteArray());
    }

    private void addDefaultConstructor(ClassWriter cw, String superName) {
        MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        ctor.visitCode();
        if (superName != null) {
            ctor.visitVarInsn(Opcodes.ALOAD, 0);
            ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        }
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(superName != null ? 1 : 0, 1);
        ctor.visitEnd();
    }

    private Path writeClass(Path classesDir, String internalName, byte[] data) throws Exception {
        Path classFile = classesDir.resolve(internalName + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, data);
        return classFile;
    }

    @SuppressWarnings("unchecked")
    private List<ByteCodeClass> getParsedClasses() throws Exception {
        Field f = Parser.class.getDeclaredField("classes");
        f.setAccessible(true);
        return (List<ByteCodeClass>) f.get(null);
    }

    private void setParsedClasses(List<ByteCodeClass> updated) throws Exception {
        Field f = Parser.class.getDeclaredField("classes");
        f.setAccessible(true);
        f.set(null, updated);
    }
}
