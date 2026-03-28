package com.codename1.tools.translator;

import org.junit.jupiter.params.ParameterizedTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JavascriptNativeAuditTest {

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("com.codename1.tools.translator.BytecodeInstructionIntegrationTest#provideCompilerConfigs")
    void allCompiledJavaApiNativesAreCategorizedForJavascript(CompilerHelper.CompilerConfig config) throws Exception {
        assertTrue(CompilerHelper.isJavaApiCompatible(config),
                "JDK " + config.jdkVersion + " must target matching bytecode level for JavaAPI");

        Path javaApiDir = Files.createTempDirectory("java-api-native-audit");
        CompilerHelper.compileJavaAPI(javaApiDir, config);

        final List<String> uncategorized = new ArrayList<String>();
        try (Stream<Path> paths = Files.walk(javaApiDir)) {
            paths.filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> inspectClass(path, uncategorized));
        }

        Collections.sort(uncategorized);
        assertTrue(uncategorized.isEmpty(),
                "Every compiled JavaAPI native must be categorized for javascript backend. Missing: " + uncategorized);
    }

    private static void inspectClass(Path classFile, final List<String> uncategorized) {
        try (InputStream input = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(input);
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                private String owner;

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    owner = name;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if ((access & Opcodes.ACC_NATIVE) != 0) {
                        String symbol = JavascriptNameUtil.methodIdentifier(owner, name, descriptor);
                        if (JavascriptNativeRegistry.categoryFor(symbol) == JavascriptNativeRegistry.NativeCategory.UNCATEGORIZED) {
                            uncategorized.add(symbol);
                        }
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect " + classFile, ex);
        }
    }
}
