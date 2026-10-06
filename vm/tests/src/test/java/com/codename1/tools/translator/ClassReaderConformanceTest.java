/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.tools.translator;

import com.codename1.tools.translator.classfile.tree.JsrInliner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The translator reads class files with its own {@code classfile} package; ASM
 * survives only here, as the oracle. Both readers drive a tracer that writes every
 * event the translator consumes as a line of text, and the two traces must be
 * identical: the same members, the same instructions in the same canonical forms,
 * the same labels at the same positions (labels are named by order of appearance,
 * so identity relationships are compared, not identities), the same line numbers
 * and local variables.
 *
 * <p>The JSR half runs every method of the Codename One core -- which was compiled
 * for a target old enough to use subroutines for {@code finally} -- through both
 * subroutine inliners, because the translator's output for those methods is built
 * from the inlined layout.
 */
class ClassReaderConformanceTest {

    // ------------------------------------------------------------------ tracing

    /** Names labels L0, L1, ... in order of first appearance, per method. */
    private static final class LabelNames {
        private final Map<Object, String> names = new IdentityHashMap<Object, String>();

        String of(Object label) {
            String name = names.get(label);
            if (name == null) {
                name = "L" + names.size();
                names.put(label, name);
            }
            return name;
        }
    }

    private static String constant(Object value, LabelNames labels) {
        if (value instanceof org.objectweb.asm.Type) {
            return "Type:" + ((org.objectweb.asm.Type) value).getDescriptor();
        }
        if (value instanceof com.codename1.tools.translator.classfile.Type) {
            return "Type:" + ((com.codename1.tools.translator.classfile.Type) value).getDescriptor();
        }
        if (value instanceof org.objectweb.asm.Handle) {
            org.objectweb.asm.Handle h = (org.objectweb.asm.Handle) value;
            return "Handle:" + h.getTag() + ":" + h.getOwner() + "." + h.getName() + h.getDesc() + ":" + h.isInterface();
        }
        if (value instanceof com.codename1.tools.translator.classfile.Handle) {
            com.codename1.tools.translator.classfile.Handle h = (com.codename1.tools.translator.classfile.Handle) value;
            return "Handle:" + h.getTag() + ":" + h.getOwner() + "." + h.getName() + h.getDesc() + ":" + h.isInterface();
        }
        if (value instanceof org.objectweb.asm.ConstantDynamic) {
            org.objectweb.asm.ConstantDynamic d = (org.objectweb.asm.ConstantDynamic) value;
            StringBuilder b = new StringBuilder("Condy:" + d.getName() + d.getDescriptor() + constant(d.getBootstrapMethod(), labels));
            for (int i = 0; i < d.getBootstrapMethodArgumentCount(); i++) {
                b.append(',').append(constant(d.getBootstrapMethodArgument(i), labels));
            }
            return b.toString();
        }
        if (value instanceof com.codename1.tools.translator.classfile.ConstantDynamic) {
            com.codename1.tools.translator.classfile.ConstantDynamic d = (com.codename1.tools.translator.classfile.ConstantDynamic) value;
            StringBuilder b = new StringBuilder("Condy:" + d.getName() + d.getDescriptor() + constant(d.getBootstrapMethod(), labels));
            for (int i = 0; i < d.getBootstrapMethodArgumentCount(); i++) {
                b.append(',').append(constant(d.getBootstrapMethodArgument(i), labels));
            }
            return b.toString();
        }
        return value == null ? "null" : value.getClass().getSimpleName() + ":" + value;
    }

    private static String args(Object[] values, LabelNames labels) {
        StringBuilder b = new StringBuilder();
        for (Object v : values) {
            b.append(constant(v, labels)).append(';');
        }
        return b.toString();
    }

    /** Code events only; the shared part of both tracers. */
    private static final class AsmCodeTracer extends org.objectweb.asm.MethodVisitor {
        final List<String> out;
        final LabelNames labels = new LabelNames();

        AsmCodeTracer(List<String> out) {
            super(Opcodes.ASM9);
            this.out = out;
        }

        @Override public void visitCode() { out.add("code"); }
        @Override public void visitInsn(int op) { out.add("insn " + op); }
        @Override public void visitIntInsn(int op, int v) { out.add("int " + op + " " + v); }
        @Override public void visitVarInsn(int op, int v) { out.add("var " + op + " " + v); }
        @Override public void visitTypeInsn(int op, String t) { out.add("type " + op + " " + t); }
        @Override public void visitFieldInsn(int op, String o, String n, String d) { out.add("field " + op + " " + o + "." + n + d); }
        @Override public void visitMethodInsn(int op, String o, String n, String d, boolean itf) { out.add("method " + op + " " + o + "." + n + d + " " + itf); }
        @Override public void visitInvokeDynamicInsn(String n, String d, org.objectweb.asm.Handle bsm, Object... a) {
            out.add("indy " + n + d + " " + constant(bsm, labels) + " " + args(a, labels));
        }
        @Override public void visitJumpInsn(int op, org.objectweb.asm.Label l) { out.add("jump " + op + " " + labels.of(l)); }
        @Override public void visitLabel(org.objectweb.asm.Label l) { out.add("label " + labels.of(l)); }
        @Override public void visitLdcInsn(Object v) { out.add("ldc " + constant(v, labels)); }
        @Override public void visitIincInsn(int v, int inc) { out.add("iinc " + v + " " + inc); }
        @Override public void visitTableSwitchInsn(int min, int max, org.objectweb.asm.Label d, org.objectweb.asm.Label... ls) {
            StringBuilder b = new StringBuilder("table " + min + " " + max + " " + labels.of(d));
            for (org.objectweb.asm.Label l : ls) b.append(' ').append(labels.of(l));
            out.add(b.toString());
        }
        @Override public void visitLookupSwitchInsn(org.objectweb.asm.Label d, int[] keys, org.objectweb.asm.Label[] ls) {
            StringBuilder b = new StringBuilder("lookup " + labels.of(d));
            for (int i = 0; i < keys.length; i++) b.append(' ').append(keys[i]).append(':').append(labels.of(ls[i]));
            out.add(b.toString());
        }
        @Override public void visitMultiANewArrayInsn(String d, int dims) { out.add("multi " + d + " " + dims); }
        @Override public void visitTryCatchBlock(org.objectweb.asm.Label s, org.objectweb.asm.Label e, org.objectweb.asm.Label h, String t) {
            out.add("try " + labels.of(s) + " " + labels.of(e) + " " + labels.of(h) + " " + t);
        }
        @Override public void visitLocalVariable(String n, String d, String sig, org.objectweb.asm.Label s, org.objectweb.asm.Label e, int i) {
            out.add("local " + n + " " + d + " " + sig + " " + labels.of(s) + " " + labels.of(e) + " " + i);
        }
        @Override public void visitLineNumber(int line, org.objectweb.asm.Label s) { out.add("line " + line + " " + labels.of(s)); }
        @Override public void visitMaxs(int s, int l) { out.add("maxs " + s + " " + l); }
        @Override public void visitEnd() { out.add("end"); }
    }

    private static final class CodeTracer extends com.codename1.tools.translator.classfile.MethodVisitor {
        final List<String> out;
        final LabelNames labels = new LabelNames();

        CodeTracer(List<String> out) {
            this.out = out;
        }

        @Override public void visitCode() { out.add("code"); }
        @Override public void visitInsn(int op) { out.add("insn " + op); }
        @Override public void visitIntInsn(int op, int v) { out.add("int " + op + " " + v); }
        @Override public void visitVarInsn(int op, int v) { out.add("var " + op + " " + v); }
        @Override public void visitTypeInsn(int op, String t) { out.add("type " + op + " " + t); }
        @Override public void visitFieldInsn(int op, String o, String n, String d) { out.add("field " + op + " " + o + "." + n + d); }
        @Override public void visitMethodInsn(int op, String o, String n, String d, boolean itf) { out.add("method " + op + " " + o + "." + n + d + " " + itf); }
        @Override public void visitInvokeDynamicInsn(String n, String d, com.codename1.tools.translator.classfile.Handle bsm, Object... a) {
            out.add("indy " + n + d + " " + constant(bsm, labels) + " " + args(a, labels));
        }
        @Override public void visitJumpInsn(int op, com.codename1.tools.translator.classfile.Label l) { out.add("jump " + op + " " + labels.of(l)); }
        @Override public void visitLabel(com.codename1.tools.translator.classfile.Label l) { out.add("label " + labels.of(l)); }
        @Override public void visitLdcInsn(Object v) { out.add("ldc " + constant(v, labels)); }
        @Override public void visitIincInsn(int v, int inc) { out.add("iinc " + v + " " + inc); }
        @Override public void visitTableSwitchInsn(int min, int max, com.codename1.tools.translator.classfile.Label d, com.codename1.tools.translator.classfile.Label... ls) {
            StringBuilder b = new StringBuilder("table " + min + " " + max + " " + labels.of(d));
            for (com.codename1.tools.translator.classfile.Label l : ls) b.append(' ').append(labels.of(l));
            out.add(b.toString());
        }
        @Override public void visitLookupSwitchInsn(com.codename1.tools.translator.classfile.Label d, int[] keys, com.codename1.tools.translator.classfile.Label[] ls) {
            StringBuilder b = new StringBuilder("lookup " + labels.of(d));
            for (int i = 0; i < keys.length; i++) b.append(' ').append(keys[i]).append(':').append(labels.of(ls[i]));
            out.add(b.toString());
        }
        @Override public void visitMultiANewArrayInsn(String d, int dims) { out.add("multi " + d + " " + dims); }
        @Override public void visitTryCatchBlock(com.codename1.tools.translator.classfile.Label s, com.codename1.tools.translator.classfile.Label e,
                com.codename1.tools.translator.classfile.Label h, String t) {
            out.add("try " + labels.of(s) + " " + labels.of(e) + " " + labels.of(h) + " " + t);
        }
        @Override public void visitLocalVariable(String n, String d, String sig, com.codename1.tools.translator.classfile.Label s,
                com.codename1.tools.translator.classfile.Label e, int i) {
            out.add("local " + n + " " + d + " " + sig + " " + labels.of(s) + " " + labels.of(e) + " " + i);
        }
        @Override public void visitLineNumber(int line, com.codename1.tools.translator.classfile.Label s) { out.add("line " + line + " " + labels.of(s)); }
        @Override public void visitMaxs(int s, int l) { out.add("maxs " + s + " " + l); }
        @Override public void visitEnd() { out.add("end"); }
    }

    /** Class-level events, plus each method's code, as ASM reads them. */
    private static List<String> asmTrace(byte[] bytes, final boolean inlineJsr) {
        final List<String> out = new ArrayList<String>();
        new org.objectweb.asm.ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
            @Override public void visit(int v, int a, String n, String sig, String sup, String[] itfs) {
                out.add("class " + v + " " + a + " " + n + " " + sig + " " + sup + " " + (itfs == null ? null : String.join(",", itfs)));
            }
            @Override public void visitSource(String s, String d) { out.add("source " + s + " " + d); }
            @Override public void visitOuterClass(String o, String n, String d) { out.add("outer " + o + " " + n + " " + d); }
            @Override public org.objectweb.asm.AnnotationVisitor visitAnnotation(String d, boolean vis) { out.add("annotation " + d + " " + vis); return null; }
            @Override public void visitInnerClass(String n, String o, String i, int a) { out.add("inner " + n + " " + o + " " + i + " " + a); }
            @Override public org.objectweb.asm.FieldVisitor visitField(int a, String n, String d, String sig, Object v) {
                out.add("field " + a + " " + n + d + " " + sig + " " + constant(v, null)); return null;
            }
            @Override public org.objectweb.asm.MethodVisitor visitMethod(int a, String n, String d, String sig, String[] ex) {
                out.add("method " + a + " " + n + d + " " + sig + " " + (ex == null ? null : String.join(",", ex)));
                AsmCodeTracer tracer = new AsmCodeTracer(out);
                return inlineJsr ? new JSRInlinerAdapter(tracer, a, n, d, sig, ex) : tracer;
            }
            @Override public void visitEnd() { out.add("classEnd"); }
        }, org.objectweb.asm.ClassReader.EXPAND_FRAMES);
        return out;
    }

    private static List<String> trace(byte[] bytes, final boolean inlineJsr) {
        final List<String> out = new ArrayList<String>();
        new com.codename1.tools.translator.classfile.ClassReader(bytes).accept(new com.codename1.tools.translator.classfile.ClassVisitor() {
            @Override public void visit(int v, int a, String n, String sig, String sup, String[] itfs) {
                out.add("class " + v + " " + a + " " + n + " " + sig + " " + sup + " " + (itfs == null ? null : String.join(",", itfs)));
            }
            @Override public void visitSource(String s, String d) { out.add("source " + s + " " + d); }
            @Override public void visitOuterClass(String o, String n, String d) { out.add("outer " + o + " " + n + " " + d); }
            @Override public com.codename1.tools.translator.classfile.AnnotationVisitor visitAnnotation(String d, boolean vis) {
                out.add("annotation " + d + " " + vis); return null;
            }
            @Override public void visitInnerClass(String n, String o, String i, int a) { out.add("inner " + n + " " + o + " " + i + " " + a); }
            @Override public com.codename1.tools.translator.classfile.FieldVisitor visitField(int a, String n, String d, String sig, Object v) {
                out.add("field " + a + " " + n + d + " " + sig + " " + constant(v, null)); return null;
            }
            @Override public com.codename1.tools.translator.classfile.MethodVisitor visitMethod(int a, String n, String d, String sig, String[] ex) {
                out.add("method " + a + " " + n + d + " " + sig + " " + (ex == null ? null : String.join(",", ex)));
                CodeTracer tracer = new CodeTracer(out);
                return inlineJsr ? new JsrInliner(tracer, a, n, d) : tracer;
            }
            @Override public void visitEnd() { out.add("classEnd"); }
        }, 0);
        return out;
    }

    // ------------------------------------------------------------------ corpora

    private static List<byte[]> classesUnder(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return new ArrayList<byte[]>();
        }
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> paths = files.filter(p -> p.toString().endsWith(".class")).sorted().collect(Collectors.toList());
            List<byte[]> out = new ArrayList<byte[]>();
            for (Path p : paths) {
                out.add(Files.readAllBytes(p));
            }
            return out;
        }
    }

    private static List<byte[]> classesInJarOf(Class<?> anchor) throws IOException {
        List<byte[]> out = new ArrayList<byte[]>();
        Path jar;
        try {
            jar = Paths.get(anchor.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return out;
        }
        if (!Files.isRegularFile(jar)) {
            return out;
        }
        try (JarInputStream in = new JarInputStream(Files.newInputStream(jar))) {
            JarEntry e;
            while ((e = in.getNextJarEntry()) != null) {
                if (e.getName().endsWith(".class") && !e.getName().endsWith("module-info.class")) {
                    out.add(readAll(in));
                }
            }
        }
        return out;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static void assertSameTraces(List<byte[]> corpus, boolean inlineJsr) {
        int compared = 0;
        for (byte[] bytes : corpus) {
            List<String> expected = asmTrace(bytes, inlineJsr);
            List<String> actual = trace(bytes, inlineJsr);
            if (!expected.equals(actual)) {
                int i = 0;
                while (i < expected.size() && i < actual.size() && expected.get(i).equals(actual.get(i))) {
                    i++;
                }
                fail(expected.get(0) + ": traces diverge at event " + i + ": expected <"
                        + (i < expected.size() ? expected.get(i) : "end") + "> but was <"
                        + (i < actual.size() ? actual.get(i) : "end") + ">");
            }
            compared++;
        }
        assertTrue(compared > 0, "nothing was compared");
    }

    @Test
    void readsJavaApiAndTheTranslatorExactlyAsAsmDoes() throws Exception {
        List<byte[]> corpus = new ArrayList<byte[]>();
        corpus.addAll(classesUnder(Paths.get("..", "JavaAPI", "target", "classes")));
        corpus.addAll(classesInJarOf(ByteCodeTranslator.class));
        corpus.addAll(classesInJarOf(org.objectweb.asm.ClassReader.class));
        Assumptions.assumeTrue(corpus.size() > 500, "JavaAPI and the translator must be built");
        assertSameTraces(corpus, false);
    }

    @Test
    void inlinesTheCoresSubroutinesExactlyAsAsmDoes() throws Exception {
        List<byte[]> corpus = classesInJarOf(com.codename1.ui.Display.class);
        Assumptions.assumeTrue(corpus.size() > 500, "codenameone-core must be on the test classpath");
        // The tree's own core build, when there is one, carries far more subroutines
        // than the published jar the test classpath pins.
        corpus.addAll(classesUnder(Paths.get("..", "..", "maven", "core", "target", "classes")));
        int withJsr = 0;
        for (byte[] bytes : corpus) {
            for (String event : trace(bytes, false)) {
                if (event.startsWith("jump " + Opcodes.JSR + " ")) {
                    withJsr++;
                    break;
                }
            }
        }
        assertTrue(withJsr >= 5, "the corpus must exercise subroutines; classes with JSR: " + withJsr);
        assertSameTraces(corpus, true);
    }
}
