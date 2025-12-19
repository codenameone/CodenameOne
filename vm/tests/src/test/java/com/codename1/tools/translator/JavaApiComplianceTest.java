package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaApiComplianceTest {

    @Test
    void fileSignaturesMatchJdk() throws Exception {
        Path outputDir = Files.createTempDirectory("java-api-signatures");
        compileJavaAPI(outputDir);

        Set<String> javaApiSignatures = readPublicSignatures(outputDir, "java/io/File");
        javaApiSignatures.addAll(readConstructors(outputDir, "java/io/File"));

        Set<String> jdkSignatures = reflectPublicSignatures(java.io.File.class);
        jdkSignatures.addAll(reflectConstructors(java.io.File.class));

        assertTrue(jdkSignatures.containsAll(javaApiSignatures),
                "JavaAPI java.io.File should expose the same public signatures as the JDK");
    }

    @Test
    void pathSignaturesMatchJdk() throws Exception {
        Path outputDir = Files.createTempDirectory("java-api-path-signatures");
        compileJavaAPI(outputDir);

        Set<String> javaApiSignatures = readPublicSignatures(outputDir, "java/nio/file/Path");
        javaApiSignatures.addAll(readConstructors(outputDir, "java/nio/file/Path"));

        Set<String> jdkSignatures = reflectPublicSignatures(java.nio.file.Path.class);
        jdkSignatures.addAll(reflectConstructors(java.nio.file.Path.class));

        assertTrue(jdkSignatures.containsAll(javaApiSignatures),
                "JavaAPI java.nio.file.Path should expose the same public signatures as the JDK");
    }

    private void compileJavaAPI(Path outputDir) throws Exception {
        Files.createDirectories(outputDir);
        Path javaApiRoot = Paths.get("..", "JavaAPI", "src").normalize().toAbsolutePath();
        List<String> sources = new ArrayList<String>();
        Files.walk(javaApiRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> sources.add(p.toString()));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<String>();

        if (!System.getProperty("java.version").startsWith("1.")) {
            args.add("--patch-module");
            args.add("java.base=" + javaApiRoot.toString());
        } else {
            args.add("-source");
            args.add("1.5");
            args.add("-target");
            args.add("1.5");
        }

        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(sources);

        int result = compiler.run(null, null, null, args.toArray(new String[0]));
        if (result != 0) {
            throw new IllegalStateException("Failed to compile JavaAPI sources. Exit code " + result);
        }
    }

    private Set<String> readPublicSignatures(Path outputDir, String internalName) throws IOException {
        Path classFile = outputDir.resolve(internalName + ".class");
        byte[] data = Files.readAllBytes(classFile);
        ClassReader reader = new ClassReader(data);
        final Set<String> methods = new HashSet<String>();
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ((access & Opcodes.ACC_PUBLIC) != 0) {
                    methods.add(name + descriptor);
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return methods;
    }

    private Set<String> readConstructors(Path outputDir, String internalName) throws IOException {
        Path classFile = outputDir.resolve(internalName + ".class");
        byte[] data = Files.readAllBytes(classFile);
        ClassReader reader = new ClassReader(data);
        final Set<String> constructors = new HashSet<String>();
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if ("<init>".equals(name) && (access & Opcodes.ACC_PUBLIC) != 0) {
                    constructors.add(name + descriptor);
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        }, 0);
        return constructors;
    }

    private Set<String> reflectPublicSignatures(Class<?> type) {
        Set<String> signatures = new HashSet<String>();
        for (Method m : type.getMethods()) {
            signatures.add(m.getName() + Type.getMethodDescriptor(m));
        }
        return signatures;
    }

    private Set<String> reflectConstructors(Class<?> type) {
        Set<String> signatures = new HashSet<String>();
        for (Constructor<?> c : type.getConstructors()) {
            Type[] params = new Type[c.getParameterTypes().length];
            for (int i = 0; i < c.getParameterTypes().length; i++) {
                params[i] = Type.getType(c.getParameterTypes()[i]);
            }
            StringBuilder desc = new StringBuilder();
            desc.append("<init>(");
            for (int i = 0; i < params.length; i++) {
                desc.append(params[i].getDescriptor());
            }
            desc.append(")V");
            signatures.add(desc.toString());
        }
        return signatures;
    }
}
