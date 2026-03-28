package com.codename1.tools.translator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class JavascriptNativeAuditSupport {

    private JavascriptNativeAuditSupport() {
    }

    static void inspectClass(Path classFile, final List<String> uncategorized) {
        try (InputStream input = Files.newInputStream(classFile)) {
            inspectClass(input, uncategorized);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to inspect " + classFile, ex);
        }
    }

    static void inspectClass(InputStream input, final List<String> uncategorized) throws IOException {
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
    }
}
