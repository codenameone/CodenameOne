package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassClosureVerifierTest {

    @Test
    void reportsMissingClassInProjectPackage(@TempDir Path tempDir) throws Exception {
        byte[] caller = classCalling("app/Caller", "app/Gone");
        Path classesDir = tempDir.resolve("classes");
        writeClassFile(classesDir, "app/Caller", caller);
        File jar = writeJar(tempDir.resolve("staged.jar"), entry("app/Caller", caller));

        Map<String, Set<String>> missing = ClassClosureVerifier.findMissingProjectReferences(
                jar, Collections.singletonList(classesDir.toFile()), Collections.<File>emptyList());

        assertEquals(Collections.singleton("app/Gone"), missing.keySet());
        assertEquals(Collections.singleton("app/Caller"), missing.get("app/Gone"));
    }

    @Test
    void ignoresMissingClassOutsideProjectPackages(@TempDir Path tempDir) throws Exception {
        byte[] caller = classCalling("app/Caller", "lib/OptionalDep");
        Path classesDir = tempDir.resolve("classes");
        writeClassFile(classesDir, "app/Caller", caller);
        File jar = writeJar(tempDir.resolve("staged.jar"), entry("app/Caller", caller));

        Map<String, Set<String>> missing = ClassClosureVerifier.findMissingProjectReferences(
                jar, Collections.singletonList(classesDir.toFile()), Collections.<File>emptyList());

        assertTrue(missing.isEmpty(), "References outside the project package space must be tolerated");
    }

    @Test
    void acceptsClassPresentInStagedJar(@TempDir Path tempDir) throws Exception {
        byte[] caller = classCalling("app/Caller", "app/Target");
        byte[] target = emptyClass("app/Target");
        Path classesDir = tempDir.resolve("classes");
        writeClassFile(classesDir, "app/Caller", caller);
        writeClassFile(classesDir, "app/Target", target);
        File jar = writeJar(tempDir.resolve("staged.jar"), entry("app/Caller", caller), entry("app/Target", target));

        Map<String, Set<String>> missing = ClassClosureVerifier.findMissingProjectReferences(
                jar, Collections.singletonList(classesDir.toFile()), Collections.<File>emptyList());

        assertTrue(missing.isEmpty());
    }

    @Test
    void acceptsClassPresentInServerProvidedJar(@TempDir Path tempDir) throws Exception {
        byte[] caller = classCalling("app/Caller", "app/ServerSide");
        Path classesDir = tempDir.resolve("classes");
        writeClassFile(classesDir, "app/Caller", caller);
        File jar = writeJar(tempDir.resolve("staged.jar"), entry("app/Caller", caller));
        File providedJar = writeJar(tempDir.resolve("provided.jar"), entry("app/ServerSide", emptyClass("app/ServerSide")));

        Map<String, Set<String>> missing = ClassClosureVerifier.findMissingProjectReferences(
                jar, Collections.singletonList(classesDir.toFile()), Collections.singletonList(providedJar));

        assertTrue(missing.isEmpty());
    }

    @Test
    void derivesProjectPackagesFromModuleJarRoots(@TempDir Path tempDir) throws Exception {
        byte[] caller = classCalling("app/Caller", "app/Gone");
        // The app's own classes reach the platform module as a jar dependency
        // (the common module jar), not as a classes directory.
        File commonJar = writeJar(tempDir.resolve("common.jar"), entry("app/Caller", caller));
        File jar = writeJar(tempDir.resolve("staged.jar"), entry("app/Caller", caller));

        Map<String, Set<String>> missing = ClassClosureVerifier.findMissingProjectReferences(
                jar, Collections.singletonList(commonJar), Collections.<File>emptyList());

        assertEquals(Collections.singleton("app/Gone"), missing.keySet());
        assertEquals(Collections.singleton("app/Caller"), missing.get("app/Gone"));
    }

    @Test
    void reportsMissingFieldTypeAndSupertype(@TempDir Path tempDir) throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "app/Holder", null, "app/GoneParent", null);
        writer.visitField(Opcodes.ACC_PRIVATE, "field", "Lapp/GoneFieldType;", null, null).visitEnd();
        writer.visitEnd();
        byte[] holder = writer.toByteArray();
        Path classesDir = tempDir.resolve("classes");
        writeClassFile(classesDir, "app/Holder", holder);
        File jar = writeJar(tempDir.resolve("staged.jar"), entry("app/Holder", holder));

        Map<String, Set<String>> missing = ClassClosureVerifier.findMissingProjectReferences(
                jar, Collections.singletonList(classesDir.toFile()), Collections.<File>emptyList());

        assertEquals(new java.util.TreeSet<String>(Arrays.asList("app/GoneFieldType", "app/GoneParent")), missing.keySet());
    }

    private static byte[] classCalling(String className, String calleeInternalName) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.visitCode();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, calleeInternalName, "show", "()V", false);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] emptyClass(String className) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null);
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void writeClassFile(Path classesDir, String internalName, byte[] bytes) throws Exception {
        Path classFile = classesDir.resolve(internalName + ".class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, bytes);
    }

    private static Object[] entry(String internalName, byte[] bytes) {
        return new Object[] {internalName + ".class", bytes};
    }

    private static File writeJar(Path jarPath, Object[]... entries) throws Exception {
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            for (Object[] entry : entries) {
                jar.putNextEntry(new JarEntry((String) entry[0]));
                jar.write((byte[]) entry[1]);
                jar.closeEntry();
            }
        }
        return jarPath.toFile();
    }
}
