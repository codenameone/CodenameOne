package com.codename1.tools.translator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class StressTest {

    private Path tempDir;
    private String originalOptimizer;

    @BeforeEach
    public void setup() throws Exception {
        Parser.cleanup();
        tempDir = Files.createTempDirectory("stress-test");
        originalOptimizer = System.getProperty("optimizer");
        System.setProperty("optimizer", "on");
        // Force re-initialization of optimizerOn flag in BytecodeMethod if needed,
        // but it's static final initialized at load time.
        // Actually BytecodeMethod.optimizerOn is static but not final, but it is initialized in static block.
        // We might need to reflectively set it if the class is already loaded.
        // However, tests run in separate forks or BytecodeMethod might be reloaded?
        // Let's assume we might need to toggle it via reflection if it's already loaded.

        try {
            java.lang.reflect.Field f = BytecodeMethod.class.getDeclaredField("optimizerOn");
            f.setAccessible(true);
            f.set(null, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AfterEach
    public void teardown() throws Exception {
        Parser.cleanup();
        deleteDir(tempDir.toFile());
        if (originalOptimizer != null) {
            System.setProperty("optimizer", originalOptimizer);
        } else {
            System.clearProperty("optimizer");
        }
    }

    private void deleteDir(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    @Test
    public void testChainOfMethods() throws Exception {
        // Creates a long chain of dependencies: Class0.main -> Class0.m0 -> Class0.m1 ... -> Class0.mN -> Class1.m0 ...
        int numClasses = 50;
        int methodsPerClass = 50;

        System.out.println("Generating " + numClasses + " classes with " + methodsPerClass + " methods each...");
        for (int i = 0; i < numClasses; i++) {
            createClass(i, numClasses, methodsPerClass);
        }

        System.out.println("Parsing classes...");
        File[] files = tempDir.toFile().listFiles((d, n) -> n.endsWith(".class"));
        for (File f : files) {
            Parser.parse(f);
        }

        System.out.println("Running optimizer...");
        long start = System.currentTimeMillis();
        File outputDir = Files.createTempDirectory("stress-output").toFile();
        try {
             // Mock ByteCodeTranslator.output to C or something valid
             ByteCodeTranslator.output = ByteCodeTranslator.OutputType.OUTPUT_TYPE_IOS;

             Parser.writeOutput(outputDir);
        } finally {
             deleteDir(outputDir);
        }
        long end = System.currentTimeMillis();
        System.out.println("Optimization and write took: " + (end - start) + "ms for " + (numClasses * methodsPerClass) + " methods.");
    }

    private void createClass(int index, int totalClasses, int methodsPerClass) throws Exception {
        String className = "Class" + index;
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);

        // Add constructor
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Add main method if index 0
        if (index == 0) {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            // Call method 0 of class 0
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "method0", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        // Add methods
        for (int m = 0; m < methodsPerClass; m++) {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "method" + m, "()V", null, null);
            mv.visitCode();

            // Call next method or method in next class to create dependency chain
            if (m < methodsPerClass - 1) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "method" + (m + 1), "()V", false);
            } else if (index < totalClasses - 1) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "Class" + (index + 1), "method0", "()V", false);
            }

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        FileOutputStream fos = new FileOutputStream(new File(tempDir.toFile(), className + ".class"));
        fos.write(cw.toByteArray());
        fos.close();
    }
}
