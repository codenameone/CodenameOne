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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Generates, from the compiled classes of an application, what JavaFX's
/// `PropertyValueFactory` does by reflection on a desktop: calling the
/// `xxxProperty()`, `getXxx()` or `isXxx()` method of a row's item by the
/// name of the property.
///
/// #### Which classes
///
/// Bytecode does not say what a `TableView<S>` holds, so the bean classes
/// are found by what they look like. A class gets accessors when it is
/// public, is not an interface, and either
///
/// - has, itself or through a class of the application it extends, at
///   least one public `xxxProperty()` method that takes no argument and
///   returns an object; or
/// - has an accessor for a property name the application hands to a
///   `PropertyValueFactory` constructor as a string constant -- which is
///   what reaches a plain bean with getters only.
///
/// A class that is not public is left alone, as JavaFX itself refuses it.
///
/// #### What is generated
///
/// A subclass of the runtime's `PropertyAccess` under the fixed name the
/// runtime instantiates, in place of the layer's empty placeholder. Its
/// `call(bean, method)` finds the bean's class with `instanceof` -- the
/// most derived class first -- and hands over to one small class per bean,
/// `<registry>$P<n>`, whose method is a chain of string comparisons ending
/// in a direct call. A primitive result is returned in its wrapper. The
/// cast in a part follows the registry's `instanceof`: a failed cast does
/// not throw on every Codename One port, so nothing relies on one.
///
/// The names are parameters because the application sees the runtime
/// relocated and a test may not.
public final class PropertyAccessGenerator {

    /// The internal name of the runtime's accessor, before relocation.
    public static final String ACCESS = "com/codename1/fxcompat/runtime/PropertyAccess";
    /// The internal name of the runtime's placeholder, before relocation.
    public static final String REGISTRY = "com/codename1/fxcompat/runtime/PropertyRegistry";
    /// The internal name of the factory whose constructor names the
    /// properties, before relocation.
    public static final String FACTORY = "javafx/scene/control/cell/PropertyValueFactory";

    private static final String PART = "$P";
    private static final String CALL = "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;";
    private static final String SUFFIX = "Property";

    private static final class Scanned {
        String name;
        String superName;
        int access;
        /// Public instance methods without arguments that return a value:
        /// name to descriptor.
        final Map<String, String> accessors = new TreeMap<String, String>();
    }

    private static final class Bean {
        final Scanned type;
        final int depth;
        final Map<String, String> accessors;

        Bean(Scanned type, int depth, Map<String, String> accessors) {
            this.type = type;
            this.depth = depth;
            this.accessors = accessors;
        }
    }

    /// A subclass is asked before the class it extends, so the first
    /// `instanceof` that matches is the nearest class.
    private static final Comparator<Bean> MOST_DERIVED_FIRST = new Comparator<Bean>() {
        @Override
        public int compare(Bean a, Bean b) {
            return a.depth != b.depth ? b.depth - a.depth : a.type.name.compareTo(b.type.name);
        }
    };

    private final File classesDir;
    private final String registry;
    private final String access;
    private final String factory;
    private int beans;

    /// Creates a generator over a directory of compiled classes.
    ///
    /// #### Parameters
    ///
    /// - `classesDir`: the directory the classes are in and the registry
    ///   is written to
    ///
    /// - `registry`: the internal name of the class to generate
    ///
    /// - `access`: the internal name of the runtime's `PropertyAccess`
    ///
    /// - `factory`: the internal name of `PropertyValueFactory`, as the
    ///   classes in the directory name it
    public PropertyAccessGenerator(File classesDir, String registry, String access, String factory) {
        this.classesDir = classesDir;
        this.registry = registry;
        this.access = access;
        this.factory = factory;
    }

    /// How many bean classes the last run generated accessors for.
    public int beans() {
        return beans;
    }

    /// Generates the registry for the given classes of the application --
    /// internal names, as they are in the directory. A second run over its
    /// own output changes no file.
    public void run(Collection<String> appClasses) throws IOException {
        Map<String, Scanned> all = new TreeMap<String, Scanned>();
        Set<String> named = new HashSet<String>();
        for (String name : appClasses) {
            File f = new File(classesDir, name + ".class");
            if (f.isFile() && !name.equals(registry) && !name.startsWith(registry + PART)) {
                Scanned s = scan(Files.readAllBytes(f.toPath()), named);
                all.put(s.name, s);
            }
        }
        // What a constant name may be asked as: the three accessor names.
        Set<String> wanted = new HashSet<String>();
        for (String property : named) {
            if (property.length() > 0) {
                String capital = Character.toUpperCase(property.charAt(0)) + property.substring(1);
                wanted.add(property + SUFFIX);
                wanted.add("get" + capital);
                wanted.add("is" + capital);
            }
        }
        List<Bean> found = new ArrayList<Bean>();
        for (Scanned s : all.values()) {
            if ((s.access & Opcodes.ACC_PUBLIC) == 0 || (s.access & Opcodes.ACC_INTERFACE) != 0) {
                continue;
            }
            Map<String, String> accessors = new TreeMap<String, String>();
            int depth = 0;
            // The nearest declaration wins; a chain that loops ends.
            for (Scanned at = s; at != null && depth < 1000; at = all.get(at.superName)) {
                for (Map.Entry<String, String> e : at.accessors.entrySet()) {
                    if (!accessors.containsKey(e.getKey())) {
                        accessors.put(e.getKey(), e.getValue());
                    }
                }
                depth++;
            }
            boolean bean = false;
            for (Map.Entry<String, String> e : accessors.entrySet()) {
                String n = e.getKey();
                boolean property = n.length() > SUFFIX.length() && n.endsWith(SUFFIX)
                        && e.getValue().startsWith("()L");
                if (property || wanted.contains(n)) {
                    bean = true;
                    break;
                }
            }
            if (bean) {
                found.add(new Bean(s, depth, accessors));
            }
        }
        Collections.sort(found, MOST_DERIVED_FIRST);
        for (int i = 0; i < found.size(); i++) {
            writeIfDifferent(new File(classesDir, registry + PART + i + ".class"),
                    part(registry + PART + i, found.get(i)));
        }
        deleteStaleParts(found.size());
        writeIfDifferent(new File(classesDir, registry + ".class"), registry(found));
        beans = found.size();
    }

    private static boolean isAccessor(String name) {
        return (name.length() > SUFFIX.length() && name.endsWith(SUFFIX))
                || (name.length() > 3 && name.startsWith("get")) || (name.length() > 2 && name.startsWith("is"));
    }

    /// Reads what the generator needs from one class, and adds to `named`
    /// every string constant the class hands to the factory's constructor.
    private Scanned scan(byte[] bytes, final Set<String> named) {
        final Scanned s = new Scanned();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                s.name = name;
                s.superName = superName;
                s.access = access;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                int skip = Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE;
                if ((access & Opcodes.ACC_PUBLIC) != 0 && (access & skip) == 0 && descriptor.startsWith("()")
                        && !descriptor.endsWith(")V") && isAccessor(name)) {
                    s.accessors.put(name, descriptor);
                }
                return new MethodVisitor(Opcodes.ASM9) {
                    private String constant;

                    @Override
                    public void visitLdcInsn(Object value) {
                        constant = value instanceof String ? (String) value : null;
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method, String desc,
                            boolean isInterface) {
                        if (constant != null && opcode == Opcodes.INVOKESPECIAL && "<init>".equals(method)
                                && owner.equals(factory)) {
                            named.add(constant);
                        }
                        constant = null;
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        constant = null;
                    }

                    @Override
                    public void visitVarInsn(int opcode, int var) {
                        constant = null;
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String field, String desc) {
                        constant = null;
                    }

                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        constant = null;
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return s;
    }

    private void deleteStaleParts(int count) throws IOException {
        File dir = new File(classesDir, registry + ".class").getParentFile();
        String prefix = registry.substring(registry.lastIndexOf('/') + 1) + PART;
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String n = f.getName();
            if (!n.startsWith(prefix) || !n.endsWith(".class")) {
                continue;
            }
            boolean stale;
            try {
                stale = Integer.parseInt(n.substring(prefix.length(), n.length() - 6)) >= count;
            } catch (NumberFormatException e) {
                stale = false;
            }
            if (stale && !f.delete()) {
                throw new IOException("Could not delete the stale " + f);
            }
        }
    }

    private static void writeIfDifferent(File f, byte[] bytes) throws IOException {
        if (f.isFile() && Arrays.equals(Files.readAllBytes(f.toPath()), bytes)) {
            return;
        }
        File dir = f.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Could not create " + dir);
        }
        Files.write(f.toPath(), bytes);
    }

    /// `static Object call(Object bean, String method)` for one bean.
    private byte[] part(String name, Bean bean) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "call", CALL, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitTypeInsn(Opcodes.CHECKCAST, bean.type.name);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        for (Map.Entry<String, String> e : bean.accessors.entrySet()) {
            Label next = new Label();
            mv.visitLdcInsn(e.getKey());
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, bean.type.name, e.getKey(), e.getValue(), false);
            box(mv, Type.getReturnType(e.getValue()));
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(next);
        }
        mv.visitFieldInsn(Opcodes.GETSTATIC, access, "NONE", "Ljava/lang/Object;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void box(MethodVisitor mv, Type type) {
        String wrapper;
        switch (type.getSort()) {
            case Type.BOOLEAN:
                wrapper = "java/lang/Boolean";
                break;
            case Type.BYTE:
                wrapper = "java/lang/Byte";
                break;
            case Type.CHAR:
                wrapper = "java/lang/Character";
                break;
            case Type.SHORT:
                wrapper = "java/lang/Short";
                break;
            case Type.INT:
                wrapper = "java/lang/Integer";
                break;
            case Type.LONG:
                wrapper = "java/lang/Long";
                break;
            case Type.FLOAT:
                wrapper = "java/lang/Float";
                break;
            case Type.DOUBLE:
                wrapper = "java/lang/Double";
                break;
            default:
                return;
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper, "valueOf", "(" + type.getDescriptor() + ")L" + wrapper
                + ";", false);
    }

    private byte[] registry(List<Bean> found) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, registry, null, access,
                null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, access, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor call = cw.visitMethod(Opcodes.ACC_PUBLIC, "call", CALL, null, null);
        call.visitCode();
        MethodVisitor knows = cw.visitMethod(Opcodes.ACC_PUBLIC, "knows", "(Ljava/lang/Object;)Z", null, null);
        knows.visitCode();
        for (int i = 0; i < found.size(); i++) {
            String type = found.get(i).type.name;
            Label next = new Label();
            call.visitVarInsn(Opcodes.ALOAD, 1);
            call.visitTypeInsn(Opcodes.INSTANCEOF, type);
            call.visitJumpInsn(Opcodes.IFEQ, next);
            call.visitVarInsn(Opcodes.ALOAD, 1);
            call.visitVarInsn(Opcodes.ALOAD, 2);
            call.visitMethodInsn(Opcodes.INVOKESTATIC, registry + PART + i, "call", CALL, false);
            call.visitInsn(Opcodes.ARETURN);
            call.visitLabel(next);

            Label other = new Label();
            knows.visitVarInsn(Opcodes.ALOAD, 1);
            knows.visitTypeInsn(Opcodes.INSTANCEOF, type);
            knows.visitJumpInsn(Opcodes.IFEQ, other);
            knows.visitInsn(Opcodes.ICONST_1);
            knows.visitInsn(Opcodes.IRETURN);
            knows.visitLabel(other);
        }
        call.visitFieldInsn(Opcodes.GETSTATIC, access, "NONE", "Ljava/lang/Object;");
        call.visitInsn(Opcodes.ARETURN);
        call.visitMaxs(0, 0);
        call.visitEnd();
        knows.visitInsn(Opcodes.ICONST_0);
        knows.visitInsn(Opcodes.IRETURN);
        knows.visitMaxs(0, 0);
        knows.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
