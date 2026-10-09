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
package com.codename1.maven;

import com.codename1.build.BuildArtifact;
import com.codename1.build.BuildFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The compliance check as the compiler of a compatibility layer: an API the
/// layer lacks is reported under the name the developer wrote and with the
/// line it is on, wherever in the class file the reference sits -- and a
/// project with no layer is checked exactly as before.
public class CompatLayerComplianceTest {

    private static final String SWING = CompatRemapperTest.SWING;
    private static final String NOT_SUPPORTED = " is not supported by the Codename One Swing compatibility layer";
    private static final Handle METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory",
            "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
            + "Ljava/lang/invoke/CallSite;", false);

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /// The code of one method, written against the names an application is
    /// compiled against.
    private interface Body {
        void emit(MethodVisitor mv);
    }

    private static void line(MethodVisitor mv, int line) {
        Label label = new Label();
        mv.visitLabel(label);
        mv.visitLineNumber(line, label);
    }

    private static void pushDefault(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                mv.visitInsn(Opcodes.ACONST_NULL);
                break;
            case Type.LONG:
                mv.visitInsn(Opcodes.LCONST_0);
                break;
            case Type.FLOAT:
                mv.visitInsn(Opcodes.FCONST_0);
                break;
            case Type.DOUBLE:
                mv.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                mv.visitInsn(Opcodes.ICONST_0);
        }
    }

    /// A call with its operands pushed and its result dropped, so the method
    /// stays well formed: the check analyses every method's stack. A
    /// constructor call expects the `NEW` before it.
    private static void call(MethodVisitor mv, int opcode, String owner, String name, String descriptor) {
        if (opcode != Opcodes.INVOKESTATIC && !"<init>".equals(name)) {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }
        for (Type argument : Type.getArgumentTypes(descriptor)) {
            pushDefault(mv, argument);
        }
        mv.visitMethodInsn(opcode, owner, name, descriptor, false);
        if (Type.getReturnType(descriptor).getSort() != Type.VOID) {
            mv.visitInsn(Opcodes.POP);
        }
    }

    private static void getStatic(MethodVisitor mv, String owner, String name, String descriptor) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, name, descriptor);
        mv.visitInsn(Opcodes.POP);
    }

    private static void ldc(MethodVisitor mv, Object constant) {
        mv.visitLdcInsn(constant);
        mv.visitInsn(Opcodes.POP);
    }

    private static void typeInsn(MethodVisitor mv, int opcode, String type) {
        mv.visitInsn(opcode == Opcodes.ANEWARRAY ? Opcodes.ICONST_0 : Opcodes.ACONST_NULL);
        mv.visitTypeInsn(opcode, type);
        mv.visitInsn(Opcodes.POP);
    }

    private static void multiArray(MethodVisitor mv, String descriptor) {
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitMultiANewArrayInsn(descriptor, 2);
        mv.visitInsn(Opcodes.POP);
    }

    private static void lambda(MethodVisitor mv, String name, String descriptor, Object... bootstrapArguments) {
        for (Type argument : Type.getArgumentTypes(descriptor)) {
            pushDefault(mv, argument);
        }
        mv.visitInvokeDynamicInsn(name, descriptor, METAFACTORY, bootstrapArguments);
        mv.visitInsn(Opcodes.POP);
    }

    /// A `try` around one instruction, catching `type` (null: a `finally`).
    private static void tryCatch(MethodVisitor mv, String type) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label after = new Label();
        mv.visitTryCatchBlock(start, end, handler, type);
        mv.visitLabel(start);
        mv.visitInsn(Opcodes.NOP);
        mv.visitLabel(end);
        mv.visitJumpInsn(Opcodes.GOTO, after);
        mv.visitLabel(handler);
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(after);
    }

    private static ClassWriter appClass(String superName, String... interfaces) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/x/Main", null, superName, interfaces);
        cw.visitSource("Main.java", null);
        return cw;
    }

    private static void method(ClassWriter cw, String name, String descriptor, String[] thrown, Body body) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, descriptor, null, thrown);
        mv.visitCode();
        body.emit(mv);
        if (Type.getReturnType(descriptor).getSort() == Type.VOID) {
            mv.visitInsn(Opcodes.RETURN);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
        }
        mv.visitMaxs(16, 16);
        mv.visitEnd();
    }

    private static byte[] appWith(Body body) {
        ClassWriter cw = appClass("java/lang/Object");
        method(cw, "run", "()V", null, body);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] apiClass(String name, boolean iface, String superName, String[] fields, String... methods) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | (iface ? Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT : 0), name,
                null, superName, null);
        for (int i = 0; fields != null && i < fields.length; i += 2) {
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, fields[i], fields[i + 1], null, null).visitEnd();
        }
        for (int i = 0; i < methods.length; i += 2) {
            cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, methods[i], methods[i + 1], null, null)
                    .visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /// A tiny Swing runtime, authored the way the real one is: under the names
    /// it ships with, and against the JDK classes its sources name.
    private File swingJar() throws Exception {
        String table = SWING + "javax/swing/JTable";
        String listener = SWING + "java/awt/event/ActionListener";
        String event = SWING + "java/awt/event/ActionEvent";
        String component = SWING + "javax/swing/JComponent";
        return CompatRemapperTest.jar(new File(tmp.getRoot(), "codenameone-swing-compat-1.jar"),
                table + ".class", apiClass(table, false, component, new String[] {"AUTO_RESIZE_OFF", "I"},
                        "<init>", "()V", "setRowHeight", "(I)V", "setFile", "(Ljava/io/File;)V"),
                component + ".class", apiClass(component, false, "java/lang/Object", null, "<init>", "()V",
                        "repaint", "()V"),
                listener + ".class", apiClass(listener, true, "java/lang/Object", null,
                        "actionPerformed", "(L" + event + ";)V"),
                event + ".class", apiClass(event, false, "java/lang/Object", null));
    }

    private File allowedDir() throws Exception {
        File dir = new File(tmp.getRoot(), "allowed");
        if (!dir.isDirectory()) {
            assertTrue(new File(dir, "java/lang").mkdirs());
            Files.write(new File(dir, "java/lang/Object.class").toPath(),
                    apiClass("java/lang/Object", false, null, null, "<init>", "()V"));
            Files.write(new File(dir, "java/lang/Runnable.class").toPath(),
                    apiClass("java/lang/Runnable", true, "java/lang/Object", null, "run", "()V"));
        }
        return dir;
    }

    /// Scans `app` as the Swing layer's build does: relocated, against the
    /// runtime jar, with the layer active.
    private List<?> scanWithSwing(byte[] app) throws Exception {
        return scan(new ClassRelocator(CompatLayers.SWING).remap(app), Collections.singletonList(CompatLayers.SWING),
                Collections.singletonList(swingJar()));
    }

    private List<?> scan(byte[] app, List<Relocation> layers, List<File> dependencies) throws Exception {
        File classes = tmp.newFolder();
        assertTrue(new File(classes, "com/x").mkdirs());
        Files.write(new File(classes, "com/x/Main.class").toPath(), app);
        BytecodeCompliance check = new BytecodeCompliance(TestProjectHost.empty()).activeLayers(layers);
        Map<String, ?> allowed = buildClassIndex(check, Collections.singletonList(allowedDir()));
        Map<String, ?> project = buildClassIndex(check, dependencies);
        Method scan = BytecodeCompliance.class.getDeclaredMethod("scanProjectClasses", File.class, Map.class,
                Map.class);
        scan.setAccessible(true);
        return (List<?>) scan.invoke(check, classes, allowed, project);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> buildClassIndex(BytecodeCompliance check, List<File> roots) throws Exception {
        Method method = BytecodeCompliance.class.getDeclaredMethod("buildClassIndex", List.class);
        method.setAccessible(true);
        return (Map<String, ?>) method.invoke(check, roots);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static String render(Object violation, String method) throws Exception {
        Method m = violation.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        return (String) m.invoke(violation);
    }

    /// Each violation as `reference @line`.
    private static List<String> summary(List<?> violations) throws Exception {
        List<String> out = new ArrayList<String>();
        for (Object v : violations) {
            out.add(field(v, "referencedMember") + " @" + field(v, "line"));
        }
        return out;
    }

    private static String unsupported(String api, int line) {
        return api + NOT_SUPPORTED + " @" + line;
    }

    @Test
    public void aSupportedReferencePassesAtEverySite() throws Exception {
        ClassWriter cw = appClass("javax/swing/JComponent", "java/awt/event/ActionListener");
        cw.visitField(Opcodes.ACC_PRIVATE, "table", "Ljavax/swing/JTable;", null, null).visitEnd();
        method(cw, "lambda$run$0", "(Ljava/awt/event/ActionEvent;)V", null, mv -> line(mv, 5));
        method(cw, "run", "([Ljavax/swing/JTable;)Ljavax/swing/JComponent;", null, mv -> {
            line(mv, 10);
            tryCatch(mv, "java/lang/Object");
            mv.visitTypeInsn(Opcodes.NEW, "javax/swing/JTable");
            call(mv, Opcodes.INVOKESPECIAL, "javax/swing/JTable", "<init>", "()V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "setRowHeight", "(I)V");
            // Inherited from the layer's own superclass.
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "repaint", "()V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "setFile", "(Ljava/io/File;)V");
            getStatic(mv, "javax/swing/JTable", "AUTO_RESIZE_OFF", "I");
            line(mv, 11);
            lambda(mv, "actionPerformed", "()Ljava/awt/event/ActionListener;",
                    Type.getMethodType("(Ljava/awt/event/ActionEvent;)V"),
                    new Handle(Opcodes.H_INVOKESTATIC, "com/x/Main", "lambda$run$0", "(Ljava/awt/event/ActionEvent;)V",
                            false),
                    Type.getMethodType("(Ljava/awt/event/ActionEvent;)V"));
            lambda(mv, "run", "(Ljavax/swing/JTable;)Ljava/lang/Runnable;",
                    Type.getMethodType("()V"),
                    new Handle(Opcodes.H_INVOKEVIRTUAL, "javax/swing/JTable", "repaint", "()V", false),
                    Type.getMethodType("()V"));
            ldc(mv, Type.getObjectType("javax/swing/JTable"));
            typeInsn(mv, Opcodes.CHECKCAST, "[Ljavax/swing/JTable;");
            multiArray(mv, "[[Ljavax/swing/JComponent;");
        });
        cw.visitEnd();

        List<?> violations = scanWithSwing(cw.toByteArray());
        assertTrue(summary(violations).toString(), violations.isEmpty());
    }

    @Test
    public void aMissingMemberIsNamedAsTheDeveloperWroteItWithItsLine() throws Exception {
        List<?> violations = scanWithSwing(appWith(mv -> {
            line(mv, 12);
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "setAutoCreateRowSorter", "(Z)V");
            line(mv, 13);
            mv.visitTypeInsn(Opcodes.NEW, "javax/swing/JTable");
            call(mv, Opcodes.INVOKESPECIAL, "javax/swing/JTable", "<init>",
                    "(I[Ljava/lang/String;Ljava/io/File;)V");
            line(mv, 14);
            getStatic(mv, "javax/swing/JTable", "AUTO_RESIZE_ALL_COLUMNS", "I");
            line(mv, 15);
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "add",
                    "(Ljavax/swing/JComponent;)Ljavax/swing/JComponent;");
        }));

        assertEquals(Arrays.asList(
                unsupported("javax.swing.JTable.setAutoCreateRowSorter(boolean)", 12),
                unsupported("new javax.swing.JTable(int, java.lang.String[], java.io.File)", 13),
                unsupported("javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS", 14),
                unsupported("javax.swing.JTable.add(javax.swing.JComponent)", 15)), summary(violations));
        Object first = violations.get(0);
        assertEquals("Main.java", field(first, "sourceFile"));
        assertEquals("com/x/Main", field(first, "sourceClass"));
        assertEquals("run()V", field(first, "sourceMethod"));
        assertNull(field(first, "suggestion"));
        String inline = render(first, "renderInline");
        assertTrue(inline, inline.contains("javax.swing.JTable.setAutoCreateRowSorter(boolean)" + NOT_SUPPORTED));
        assertTrue(inline, inline.endsWith(" at Main.java:12"));
        assertTrue(render(first, "render"), render(first, "render").contains("Source location: Main.java:12\n"));
    }

    /// `new JTree()` is a type instruction and a constructor call, and every
    /// method called on it is another instruction; a class the layer lacks is
    /// one finding per line it is used on.
    @Test
    public void aMissingClassIsReportedOncePerLine() throws Exception {
        List<?> violations = scanWithSwing(appWith(mv -> {
            line(mv, 20);
            mv.visitTypeInsn(Opcodes.NEW, "javax/swing/JTree");
            call(mv, Opcodes.INVOKESPECIAL, "javax/swing/JTree", "<init>", "()V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTree", "expandRow", "(I)V");
            getStatic(mv, "javax/swing/JTree", "ROOT", "I");
            line(mv, 21);
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTree", "expandRow", "(I)V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "selectAll", "()V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "selectAll", "()V");
        }));

        assertEquals(Arrays.asList(unsupported("javax.swing.JTree", 20), unsupported("javax.swing.JTree", 21),
                unsupported("javax.swing.JTable.selectAll()", 21)), summary(violations));
    }

    @Test
    public void declarationsAreScanned() throws Exception {
        ClassWriter cw = appClass("javax/swing/JTree", "javax/swing/Missing1", "java/awt/event/ActionListener");
        cw.visitField(Opcodes.ACC_PRIVATE, "a", "[Ljavax/swing/Missing2;", null, null).visitEnd();
        cw.visitField(Opcodes.ACC_PRIVATE, "b", "Ljavax/swing/JTable;", null, null).visitEnd();
        method(cw, "run", "(I[[Ljavax/swing/Missing3;Ljavax/swing/JTable;)Ljavax/swing/Missing4;",
                new String[] {"javax/swing/Missing5", "java/lang/Missing"}, mv -> {
                    line(mv, 40);
                    tryCatch(mv, "javax/swing/Missing6");
                    tryCatch(mv, null);
                    line(mv, 41);
                    mv.visitInsn(Opcodes.NOP);
                });
        cw.visitEnd();

        assertEquals(Arrays.asList(
                unsupported("javax.swing.JTree", 0),
                unsupported("javax.swing.Missing1", 0),
                unsupported("javax.swing.Missing2", 0),
                unsupported("javax.swing.Missing3", 40),
                unsupported("javax.swing.Missing4", 40),
                unsupported("javax.swing.Missing5", 40),
                unsupported("javax.swing.Missing6", 40)), summary(scanWithSwing(cw.toByteArray())));
        Object superclass = scanWithSwing(cw.toByteArray()).get(0);
        assertEquals("(extends)", field(superclass, "sourceMethod"));
        assertTrue(render(superclass, "renderInline"), render(superclass, "renderInline").endsWith(" at Main.java"));
    }

    /// A class missing from a method's signature and used in its body is one
    /// problem; the report is the one that has a line.
    @Test
    public void aDeclarationIsNotReportedBesideTheInstructionThatUsesIt() throws Exception {
        ClassWriter cw = appClass("java/lang/Object");
        cw.visitField(Opcodes.ACC_PRIVATE, "tree", "Ljavax/swing/JTree;", null, null).visitEnd();
        method(cw, "run", "(Ljavax/swing/JTree;)V", null, mv -> {
            line(mv, 50);
            mv.visitInsn(Opcodes.NOP);
            line(mv, 51);
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTree", "expandRow", "(I)V");
        });
        cw.visitEnd();

        assertEquals(Collections.singletonList(unsupported("javax.swing.JTree", 51)),
                summary(scanWithSwing(cw.toByteArray())));
    }

    @Test
    public void typeInstructionsAndClassConstantsAreScanned() throws Exception {
        List<?> violations = scanWithSwing(appWith(mv -> {
            line(mv, 60);
            ldc(mv, Type.getObjectType("javax/swing/Missing1"));
            line(mv, 61);
            ldc(mv, Type.getType("[Ljavax/swing/Missing2;"));
            line(mv, 62);
            typeInsn(mv, Opcodes.INSTANCEOF, "[Ljavax/swing/Missing3;");
            line(mv, 63);
            typeInsn(mv, Opcodes.CHECKCAST, "javax/swing/Missing4");
            line(mv, 64);
            typeInsn(mv, Opcodes.ANEWARRAY, "javax/swing/Missing5");
            line(mv, 65);
            multiArray(mv, "[[Ljavax/swing/Missing6;");
            line(mv, 66);
            ldc(mv, "javax/swing/NotAReference");
            ldc(mv, Type.getType("[I"));
            multiArray(mv, "[[I");
        }));

        assertEquals(Arrays.asList(
                unsupported("javax.swing.Missing1", 60),
                unsupported("javax.swing.Missing2", 61),
                unsupported("javax.swing.Missing3", 62),
                unsupported("javax.swing.Missing4", 63),
                unsupported("javax.swing.Missing5", 64),
                unsupported("javax.swing.Missing6", 65)), summary(violations));
    }

    /// Every lambda and method reference is an `invokedynamic`: no method
    /// instruction names the listener interface or the referenced method.
    @Test
    public void lambdasAndMethodReferencesAreScanned() throws Exception {
        final Handle body = new Handle(Opcodes.H_INVOKESTATIC, "com/x/Main", "lambda$run$0", "()V", false);
        List<?> violations = scanWithSwing(appWith(mv -> {
            // A listener interface the layer lacks.
            line(mv, 70);
            lambda(mv, "mouseClicked", "()Ljava/awt/event/MouseListener;",
                    Type.getMethodType("()V"), body, Type.getMethodType("()V"));
            // An interface it has, implemented with an event class it lacks.
            line(mv, 71);
            lambda(mv, "actionPerformed", "()Ljava/awt/event/ActionListener;",
                    Type.getMethodType("(Ljava/awt/event/KeyEvent;)V"), body, Type.getMethodType("()V"));
            // An interface it has, without the method the lambda implements.
            line(mv, 72);
            lambda(mv, "actionCancelled", "()Ljava/awt/event/ActionListener;",
                    Type.getMethodType("(Ljava/awt/event/ActionEvent;)V"), body, Type.getMethodType("()V"));
            // table::setAutoCreateRowSorter
            line(mv, 73);
            lambda(mv, "run", "(Ljavax/swing/JTable;)Ljava/lang/Runnable;",
                    Type.getMethodType("()V"),
                    new Handle(Opcodes.H_INVOKEVIRTUAL, "javax/swing/JTable", "setAutoCreateRowSorter", "(Z)V", false),
                    Type.getMethodType("()V"));
            // JTree::new
            line(mv, 74);
            lambda(mv, "run", "()Ljava/lang/Runnable;", Type.getMethodType("()V"),
                    new Handle(Opcodes.H_NEWINVOKESPECIAL, "javax/swing/JTree", "<init>", "()V", false),
                    Type.getMethodType("()V"));
            // A field read through a handle, and a handle loaded as a constant.
            line(mv, 75);
            ldc(mv, new Handle(Opcodes.H_GETSTATIC, "javax/swing/JTable", "AUTO_RESIZE_LAST_COLUMN", "I",
                    false));
            line(mv, 76);
            ldc(mv, Type.getMethodType("(Ljavax/swing/JList;)V"));
        }));

        assertEquals(Arrays.asList(
                unsupported("java.awt.event.MouseListener", 70),
                unsupported("java.awt.event.KeyEvent", 71),
                unsupported("java.awt.event.ActionListener.actionCancelled(java.awt.event.ActionEvent)", 72),
                unsupported("javax.swing.JTable.setAutoCreateRowSorter(boolean)", 73),
                unsupported("javax.swing.JTree", 74),
                unsupported("javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN", 75),
                unsupported("javax.swing.JList", 76)), summary(violations));
    }

    /// The same application with no layer switched on: only method, field
    /// and type instructions are examined, each failing one is reported, in
    /// the form the check has always used -- and it says how to get the layer.
    @Test
    public void aProjectWithNoLayerIsCheckedAsBefore() throws Exception {
        ClassWriter cw = appClass("javax/swing/JTree", "javax/swing/Missing1");
        cw.visitField(Opcodes.ACC_PRIVATE, "a", "Ljavax/swing/Missing2;", null, null).visitEnd();
        method(cw, "run", "(Ljavax/swing/Missing3;)V", new String[] {"javax/swing/Missing4"}, mv -> {
            line(mv, 80);
            tryCatch(mv, "javax/swing/Missing5");
            mv.visitTypeInsn(Opcodes.NEW, "javax/swing/JTable");
            call(mv, Opcodes.INVOKESPECIAL, "javax/swing/JTable", "<init>", "()V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "setRowHeight", "(I)V");
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "setRowHeight", "(I)V");
            line(mv, 81);
            getStatic(mv, "java/awt/Color", "RED", "Ljava/awt/Color;");
            ldc(mv, Type.getObjectType("javax/swing/Missing6"));
            typeInsn(mv, Opcodes.CHECKCAST, "[Ljavax/swing/Missing7;");
            multiArray(mv, "[[Ljavax/swing/Missing8;");
            lambda(mv, "actionPerformed", "()Ljava/awt/event/ActionListener;",
                    Type.getMethodType("(Ljava/awt/event/ActionEvent;)V"),
                    new Handle(Opcodes.H_INVOKEVIRTUAL, "javax/swing/JTable", "repaint", "()V", false),
                    Type.getMethodType("(Ljava/awt/event/ActionEvent;)V"));
            call(mv, Opcodes.INVOKESTATIC, "javafx/application/Platform", "exit", "()V");
            call(mv, Opcodes.INVOKESTATIC, "com/vendor/Sdk", "start", "()V");
        });
        cw.visitEnd();

        List<?> violations = scan(cw.toByteArray(), Collections.<Relocation>emptyList(),
                Collections.<File>emptyList());
        assertEquals(Arrays.asList(
                "javax/swing/JTable (type) @80",
                "javax/swing/JTable#<init>()V @80",
                "javax/swing/JTable#setRowHeight(I)V @80",
                "javax/swing/JTable#setRowHeight(I)V @80",
                "java/awt/Color#RED:Ljava/awt/Color; @81",
                // What the lambda and the method reference name only in
                // their bootstrap arguments.
                "java/awt/event/ActionEvent (type) @81",
                "javax/swing/JTable#repaint()V @81",
                "java/awt/event/ActionEvent (type) @81",
                "javafx/application/Platform#exit()V @81",
                "com/vendor/Sdk#start()V @81"), summary(violations));
        for (int i = 0; i < 8; i++) {
            assertEquals(CompatLayers.enableHint(CompatLayers.SWING), field(violations.get(i), "suggestion"));
        }
        assertTrue(CompatLayers.enableHint(CompatLayers.SWING).contains("src/main/desktop"));
        assertEquals(CompatLayers.enableHint(CompatLayers.JAVAFX), field(violations.get(8), "suggestion"));
        assertNull(field(violations.get(9), "suggestion"));
        // A class compiled without debug information says nothing new.
        Object bare = newViolation("a/B", "m()V", "c/D#e()V", null, "a/B.class");
        assertEquals("a/B#m()V -> c/D#e()V (a/B.class)", render(bare, "renderInline"));
        assertEquals("Source class: a/B\nSource method: m()V\nSource bytecode file: a/B.class\n"
                + "Forbidden reference: c/D#e()V", render(bare, "render"));
    }

    private static Object newViolation(String sourceClass, String sourceMethod, String referencedMember,
                                       String suggestion, String sourcePath) throws Exception {
        java.lang.reflect.Constructor<?> ctor = Class.forName("com.codename1.maven.BytecodeCompliance$Violation")
                .getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(sourceClass, sourceMethod, referencedMember, suggestion, sourcePath);
    }

    /// With a layer active, a reference outside its packages is still checked
    /// only where it always was, and a raw `java/awt/` name -- a class the
    /// relocation never saw -- gets no advice to enable what is enabled.
    @Test
    public void anActiveLayerWidensTheScanOnlyForItsOwnPackages() throws Exception {
        ClassWriter cw = appClass("com/vendor/Base", "com/vendor/Listener");
        cw.visitField(Opcodes.ACC_PRIVATE, "a", "Lcom/vendor/Thing;", null, null).visitEnd();
        method(cw, "run", "(Lcom/vendor/Thing;)V", new String[] {"com/vendor/Failure"}, mv -> {
            line(mv, 90);
            ldc(mv, Type.getObjectType("com/vendor/Thing"));
            typeInsn(mv, Opcodes.CHECKCAST, "[Lcom/vendor/Thing;");
            multiArray(mv, "[[Lcom/vendor/Thing;");
            lambda(mv, "run", "()Lcom/vendor/Listener;", Type.getMethodType("()V"),
                    new Handle(Opcodes.H_INVOKESTATIC, "com/vendor/Sdk", "start", "()V", false),
                    Type.getMethodType("()V"));
            call(mv, Opcodes.INVOKESTATIC, "com/vendor/Sdk", "start", "()V");
            call(mv, Opcodes.INVOKESTATIC, "com/vendor/Sdk", "start", "()V");
        });
        cw.visitEnd();
        // Three: the two calls, and the method reference, which reaches the
        // same method through a handle and through no instruction.
        assertEquals(Arrays.asList("com/vendor/Sdk#start()V @90", "com/vendor/Sdk#start()V @90",
                        "com/vendor/Sdk#start()V @90"),
                summary(scanWithSwing(cw.toByteArray())));

        List<?> raw = scan(appWith(mv -> mv.visitTypeInsn(Opcodes.NEW, "java/awt/Color")),
                Collections.singletonList(CompatLayers.SWING), Collections.singletonList(swingJar()));
        assertEquals(Collections.singletonList("java/awt/Color (type) @0"), summary(raw));
        assertNull(field(raw.get(0), "suggestion"));
    }

    /// The Swing jar is authored under the names it ships with, so indexing it
    /// "under its relocated names too" must not produce a second entry -- and
    /// the one entry has to be the relocated reading, whose members name the
    /// JDK classes the device has.
    @Test
    public void aRuntimeAuthoredUnderItsFinalNamesIsIndexedOnce() throws Exception {
        BytecodeCompliance check = new BytecodeCompliance(TestProjectHost.empty());
        Map<String, ?> swing = buildClassIndex(check, Collections.singletonList(swingJar()));
        assertEquals(4, swing.size());
        Set<?> methods = (Set<?>) field(swing.get(SWING + "javax/swing/JTable"), "methods");
        assertTrue(methods.toString(), methods.contains("setFile(Lcom/codename1/compat/jdk/File;)V"));
        assertFalse(methods.toString(), methods.contains("setFile(Ljava/io/File;)V"));

        // A runtime authored under the API's own names is indexed under both.
        File fx = CompatRemapperTest.jar(new File(tmp.getRoot(), "codenameone-javafx-compat-1.jar"),
                "javafx/stage/Stage.class", apiClass("javafx/stage/Stage", false, "java/lang/Object", null),
                "com/codename1/fxcompat/runtime/FxApp.class",
                apiClass("com/codename1/fxcompat/runtime/FxApp", false, "java/lang/Object", null));
        Map<String, ?> javafx = buildClassIndex(check, Collections.singletonList(fx));
        assertEquals(javafx.keySet().toString(), 4, javafx.size());
        assertTrue(javafx.containsKey("javafx/stage/Stage"));
        assertTrue(javafx.containsKey("com/codename1/fxcompat/javafx/stage/Stage"));
        assertTrue(javafx.containsKey("com/codename1/fxcompat/rt/FxApp"));
    }

    /// The whole check, as a build runs it: the active layers come from the
    /// project's dependencies, and the failure names the API and the line.
    @Test
    public void theBuildFailsNamingTheUnsupportedApi() throws Exception {
        File classes = tmp.newFolder("classes");
        assertTrue(new File(classes, "com/x").mkdirs());
        byte[] app = appWith(mv -> {
            line(mv, 12);
            call(mv, Opcodes.INVOKEVIRTUAL, "javax/swing/JTable", "setAutoCreateRowSorter", "(Z)V");
        });
        Files.write(new File(classes, "com/x/Main.class").toPath(),
                new ClassRelocator(CompatLayers.SWING).remap(app));
        File runtime = CompatRemapperTest.jar(new File(tmp.getRoot(), "java-runtime-1.jar"),
                "java/lang/Object.class", apiClass("java/lang/Object", false, null, null, "<init>", "()V"));
        File core = CompatRemapperTest.jar(new File(tmp.getRoot(), "codenameone-core-1.jar"),
                "com/codename1/ui/CN.class", apiClass("com/codename1/ui/CN", false, "java/lang/Object", null));
        TestProjectHost host = TestProjectHost.empty();
        host.buildDir = tmp.newFolder("build");
        host.outputDir = classes;
        host.artifacts.add(new BuildArtifact("com.codenameone", "java-runtime", "1", null, "jar", "provided", runtime,
                null));
        host.artifacts.add(new BuildArtifact("com.codenameone", "codenameone-core", "1", null, "jar", "provided", core,
                null));
        host.artifacts.add(new BuildArtifact("com.codenameone", "codenameone-swing-compat", "1", null, "jar",
                "compile", swingJar(), null));

        try {
            new BytecodeCompliance(host).execute();
            fail("An unsupported Swing method must fail the build");
        } catch (BuildFailureException expected) {
            String message = expected.getMessage();
            assertTrue(message, message.contains("javax.swing.JTable.setAutoCreateRowSorter(boolean)" + NOT_SUPPORTED));
            assertTrue(message, message.contains("at Main.java:12"));
        }
    }
}
