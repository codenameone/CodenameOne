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
package com.codename1.fxml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// Generates, from the compiled classes of an application, what an FXML
/// loader does by reflection on a desktop: finding the class that builds a
/// document, creating a controller, setting its `@FXML` fields and calling
/// its handler methods.
///
/// #### Why after javac, and why bytecode
///
/// A controller's `@FXML` members are usually private. Source generated
/// before compilation could not name them, and only the class files say for
/// certain which members exist, of which types, and what a controller
/// inherits. So this runs on the compiled classes and does two things:
///
/// - it widens what it needs to reach -- the controller class, its `@FXML`
///   fields and methods, its `initialize()` and its no-argument constructor
///   become public, in place. Nothing else about the class changes;
/// - it writes the registry class, whose methods are chains of string
///   comparisons ending in a direct field write or method call.
///
/// #### The registry
///
/// A subclass of the runtime's `FxmlDispatch` under the fixed name the
/// runtime instantiates (the names are parameters, because the application
/// sees the runtime relocated and the tests do not). It overrides
///
/// - `has(path)` and `load(path, context)` from the `CN1_PATH` constant of
///   each compiled document;
/// - `create(className)` for every `fx:controller` class that has a
///   no-argument constructor, by `new`;
/// - `inject`, `invoke` and `init`, which find the controller's class with
///   `instanceof` -- the most derived class first -- and hand over to one
///   small class per controller, `<registry>$C<n>`, so that the registry
///   itself declares nothing its placeholder does not.
///
/// A field is only written after an `instanceof` check of the value: a
/// failed cast does not throw on every Codename One port, so the generated
/// code never relies on one.
///
/// #### Which methods are handlers
///
/// Every method marked `@FXML`, and every public method whose name some
/// document uses as `#name`, provided it takes no argument or one object.
/// A handler with both forms is called with the event when the event is of
/// the parameter's type. Members a controller inherits from another class
/// of the application are included.
public final class FxmlDispatchGenerator {

    /// The names the generated code refers to, which differ between an
    /// application build -- where the runtime is relocated -- and a test.
    public static final class Names {
        final String registry;
        final String dispatch;
        final String context;
        final String annotation;

        /// Creates a set of names.
        ///
        /// #### Parameters
        ///
        /// - `registry`: the internal name of the class to generate
        ///
        /// - `dispatch`: the internal name of the runtime's `FxmlDispatch`
        ///
        /// - `context`: the internal name of the runtime's `FxmlContext`
        ///
        /// - `fxmlAnnotation`: the internal name of the `FXML` annotation
        public Names(String registry, String dispatch, String context, String fxmlAnnotation) {
            this.registry = registry;
            this.dispatch = dispatch;
            this.context = context;
            this.annotation = "L" + fxmlAnnotation + ";";
        }
    }

    /// The internal name of the runtime's dispatcher, before relocation.
    public static final String DISPATCH = "com/codename1/fxcompat/runtime/FxmlDispatch";
    /// The internal name of the runtime's registry placeholder, before
    /// relocation.
    public static final String REGISTRY = "com/codename1/fxcompat/runtime/FxmlRegistry";
    /// The internal name of the runtime's load context, before relocation.
    public static final String CONTEXT = "com/codename1/fxcompat/runtime/FxmlContext";
    /// The internal name of the `FXML` annotation, before relocation.
    public static final String ANNOTATION = "javafx/fxml/FXML";
    /// The internal name prefix of every compiled document.
    public static final String LOADER_PREFIX = "com/codename1/generated/fxml/Fxml_";

    private static final String PART = "$C";
    private static final int VISIBILITY = Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC;

    private static final class Member {
        final String owner;
        final String name;
        final String descriptor;
        final int access;
        final boolean annotated;

        Member(String owner, String name, String descriptor, int access, boolean annotated) {
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
            this.access = access;
            this.annotated = annotated;
        }

        String key() {
            return name + descriptor;
        }
    }

    private static final class Scanned {
        String name;
        String superName;
        int access;
        boolean annotated;
        final List<Member> fields = new ArrayList<Member>();
        final List<Member> methods = new ArrayList<Member>();
        String path;
        String controller;
        String handlers;
    }

    /// What one controller class needs, its own and inherited.
    private static final class Controller {
        final Scanned type;
        final int depth;
        final List<Member> fields = new ArrayList<Member>();
        final List<Member> handlers = new ArrayList<Member>();
        Member initialize;

        Controller(Scanned type, int depth) {
            this.type = type;
            this.depth = depth;
        }
    }

    private final File classesDir;
    private final Names names;
    private int documents;
    private int controllers;

    /// Creates a generator over a directory of compiled classes.
    public FxmlDispatchGenerator(File classesDir, Names names) {
        this.classesDir = classesDir;
        this.names = names;
    }

    /// How many compiled documents the last run registered.
    public int documents() {
        return documents;
    }

    /// How many controller classes the last run generated code for.
    public int controllers() {
        return controllers;
    }

    /// Generates the registry for the given classes of the application --
    /// internal names, as they are in the directory -- and widens the
    /// controllers among them. A second run over its own output changes no
    /// file.
    public void run(Collection<String> appClasses) throws IOException {
        Map<String, Scanned> all = new TreeMap<String, Scanned>();
        for (String name : appClasses) {
            File f = new File(classesDir, name + ".class");
            if (f.isFile() && !name.equals(names.registry) && !name.startsWith(names.registry + PART)) {
                Scanned s = scan(read(f));
                all.put(s.name, s);
            }
        }
        Map<String, Scanned> loaders = new TreeMap<String, Scanned>();
        Set<String> handlerNames = new HashSet<String>();
        Set<String> named = new TreeSet<String>();
        for (Scanned s : all.values()) {
            if (s.name.startsWith(LOADER_PREFIX) && s.path != null) {
                loaders.put(s.path, s);
                if (s.handlers != null && s.handlers.length() > 0) {
                    handlerNames.addAll(Arrays.asList(s.handlers.split(",")));
                }
                if (s.controller != null && s.controller.length() > 0) {
                    named.add(s.controller);
                }
            }
        }
        List<Controller> found = new ArrayList<Controller>();
        for (Scanned s : all.values()) {
            boolean isNamed = named.contains(s.name.replace('/', '.'));
            if ((s.access & Opcodes.ACC_INTERFACE) != 0 || !(isNamed || s.annotated)) {
                continue;
            }
            found.add(controller(s, all, handlerNames));
        }
        Collections.sort(found, new Comparator<Controller>() {
            @Override
            public int compare(Controller a, Controller b) {
                return a.depth != b.depth ? b.depth - a.depth : a.type.name.compareTo(b.type.name);
            }
        });

        // Widen first: the registry refers to the members as public ones.
        Map<String, Set<String>> widen = new LinkedHashMap<String, Set<String>>();
        List<String> creatable = new ArrayList<String>();
        for (Controller c : found) {
            need(widen, c.type.name, null);
            for (Member m : c.fields) {
                need(widen, m.owner, m.key());
            }
            for (Member m : c.handlers) {
                need(widen, m.owner, m.key());
            }
            if (c.initialize != null) {
                need(widen, c.initialize.owner, c.initialize.key());
            }
        }
        for (String binary : named) {
            Scanned s = all.get(binary.replace('.', '/'));
            if (s == null || (s.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0) {
                continue;
            }
            for (Member m : s.methods) {
                if ("<init>".equals(m.name) && "()V".equals(m.descriptor)) {
                    need(widen, s.name, m.key());
                    creatable.add(binary);
                }
            }
        }
        for (Map.Entry<String, Set<String>> e : widen.entrySet()) {
            File f = new File(classesDir, e.getKey() + ".class");
            writeIfDifferent(f, widen(read(f), widen.keySet(), e.getValue()));
        }

        for (int i = 0; i < found.size(); i++) {
            writeIfDifferent(new File(classesDir, names.registry + PART + i + ".class"),
                    part(names.registry + PART + i, found.get(i)));
        }
        deleteStaleParts(found.size());
        writeIfDifferent(new File(classesDir, names.registry + ".class"), registry(loaders, creatable, found));
        documents = loaders.size();
        controllers = found.size();
    }

    private static void need(Map<String, Set<String>> widen, String owner, String member) {
        Set<String> members = widen.get(owner);
        if (members == null) {
            members = new HashSet<String>();
            widen.put(owner, members);
        }
        if (member != null) {
            members.add(member);
        }
    }

    private void deleteStaleParts(int count) throws IOException {
        File registry = new File(classesDir, names.registry + ".class");
        File dir = registry.getParentFile();
        String prefix = names.registry.substring(names.registry.lastIndexOf('/') + 1) + PART;
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String n = f.getName();
            if (!n.startsWith(prefix) || !n.endsWith(".class")) {
                continue;
            }
            String index = n.substring(prefix.length(), n.length() - 6);
            boolean stale;
            try {
                stale = Integer.parseInt(index) >= count;
            } catch (NumberFormatException e) {
                stale = false;
            }
            if (stale && !f.delete()) {
                throw new IOException("Could not delete the stale " + f);
            }
        }
    }

    // ------------------------------------------------------------ reading

    private static byte[] read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /// Writes a file unless it already holds exactly these bytes, so that a
    /// build that changes nothing touches nothing.
    static boolean writeIfDifferent(File f, byte[] bytes) throws IOException {
        if (f.isFile() && f.length() == bytes.length && Arrays.equals(read(f), bytes)) {
            return false;
        }
        File dir = f.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Could not create " + dir);
        }
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
        return true;
    }

    private Scanned scan(byte[] bytes) {
        final Scanned s = new Scanned();
        final String fxml = names.annotation;
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                s.name = name;
                s.superName = superName;
                s.access = access;
            }

            @Override
            public FieldVisitor visitField(final int access, final String name, final String descriptor,
                    String signature, Object value) {
                if ((access & Opcodes.ACC_STATIC) != 0 && value instanceof String) {
                    if ("CN1_PATH".equals(name)) {
                        s.path = (String) value;
                    } else if ("CN1_CONTROLLER".equals(name)) {
                        s.controller = (String) value;
                    } else if ("CN1_HANDLERS".equals(name)) {
                        s.handlers = (String) value;
                    }
                }
                final int at = s.fields.size();
                s.fields.add(new Member(s.name, name, descriptor, access, false));
                return new FieldVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (fxml.equals(annotation)) {
                            s.annotated = true;
                            s.fields.set(at, new Member(s.name, name, descriptor, access, true));
                        }
                        return null;
                    }
                };
            }

            @Override
            public MethodVisitor visitMethod(final int access, final String name, final String descriptor,
                    String signature, String[] exceptions) {
                if ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0) {
                    return null;
                }
                final int at = s.methods.size();
                s.methods.add(new Member(s.name, name, descriptor, access, false));
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
                        if (fxml.equals(annotation)) {
                            s.annotated = true;
                            s.methods.set(at, new Member(s.name, name, descriptor, access, true));
                        }
                        return null;
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return s;
    }

    private static Controller controller(Scanned type, Map<String, Scanned> all, Set<String> handlerNames) {
        int depth = 0;
        for (Scanned s = all.get(type.superName); s != null; s = all.get(s.superName)) {
            depth++;
        }
        Controller c = new Controller(type, depth);
        Set<String> fields = new HashSet<String>();
        Set<String> methods = new HashSet<String>();
        for (Scanned s = type; s != null; s = all.get(s.superName)) {
            for (Member f : s.fields) {
                int sort = Type.getType(f.descriptor).getSort();
                // A public field is the document's to set without the
                // annotation, as it is for a desktop loader.
                boolean settable = (f.annotated || (f.access & Opcodes.ACC_PUBLIC) != 0)
                        && (f.access & (Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) == 0
                        && (sort == Type.OBJECT || sort == Type.ARRAY);
                // A field of a subclass hides the one it shadows.
                if (settable && fields.add(f.name)) {
                    c.fields.add(f);
                }
            }
            for (Member m : s.methods) {
                if (m.name.startsWith("<") || (m.access & Opcodes.ACC_ABSTRACT) != 0) {
                    continue;
                }
                Type[] args = Type.getArgumentTypes(m.descriptor);
                if ("initialize".equals(m.name) && args.length == 0 && (m.access & Opcodes.ACC_STATIC) == 0) {
                    if (c.initialize == null) {
                        c.initialize = m;
                    }
                    continue;
                }
                boolean wanted = m.annotated
                        || ((m.access & Opcodes.ACC_PUBLIC) != 0 && handlerNames.contains(m.name));
                boolean callable = args.length == 0 || (args.length == 1 && args[0].getSort() == Type.OBJECT);
                if (wanted && callable && methods.add(m.key())) {
                    c.handlers.add(m);
                }
            }
        }
        return c;
    }

    // ----------------------------------------------------------- widening

    private static int open(int access) {
        return (access & ~VISIBILITY) | Opcodes.ACC_PUBLIC;
    }

    private static byte[] widen(byte[] bytes, final Set<String> classes, final Set<String> members) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        final String[] self = new String[1];
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                self[0] = name;
                super.visit(version, open(access), name, signature, superName, interfaces);
            }

            @Override
            public void visitInnerClass(String name, String outerName, String innerName, int access) {
                super.visitInnerClass(name, outerName, innerName, classes.contains(name) ? open(access) : access);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                    Object value) {
                return super.visitField(members.contains(name + descriptor) ? open(access) : access, name,
                        descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                return super.visitMethod(members.contains(name + descriptor) ? open(access) : access, name,
                        descriptor, signature, exceptions);
            }
        }, 0);
        return writer.toByteArray();
    }

    // --------------------------------------------------------- generating

    private static void ifNotNamed(MethodVisitor mv, String literal, int argument, Label otherwise) {
        mv.visitLdcInsn(literal);
        mv.visitVarInsn(Opcodes.ALOAD, argument);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, otherwise);
    }

    private static void answer(MethodVisitor mv, boolean value) {
        mv.visitInsn(value ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
    }

    private static void end(MethodVisitor mv) {
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private byte[] registry(Map<String, Scanned> loaders, List<String> creatable, List<Controller> found) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, names.registry, null,
                names.dispatch, null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, names.dispatch, "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        end(mv);

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "has", "(Ljava/lang/String;)Z", null, null);
        mv.visitCode();
        for (String path : loaders.keySet()) {
            Label next = new Label();
            ifNotNamed(mv, path, 1, next);
            answer(mv, true);
            mv.visitLabel(next);
        }
        answer(mv, false);
        end(mv);

        String loadDescriptor = "(Ljava/lang/String;L" + names.context + ";)Ljava/lang/Object;";
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "load", loadDescriptor, null, new String[] {"java/lang/Exception"});
        mv.visitCode();
        for (Map.Entry<String, Scanned> e : loaders.entrySet()) {
            Label next = new Label();
            ifNotNamed(mv, e.getKey(), 1, next);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, e.getValue().name, "load",
                    "(L" + names.context + ";)Ljava/lang/Object;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(next);
        }
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        end(mv);

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "create", "(Ljava/lang/String;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        for (String binary : creatable) {
            Label next = new Label();
            ifNotNamed(mv, binary, 1, next);
            String internal = binary.replace('.', '/');
            mv.visitTypeInsn(Opcodes.NEW, internal);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internal, "<init>", "()V", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(next);
        }
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        end(mv);

        delegate(cw, found, "inject", "Ljava/lang/String;Ljava/lang/Object;", null);
        delegate(cw, found, "invoke", "Ljava/lang/String;Ljava/lang/Object;", new String[] {"java/lang/Exception"});
        delegate(cw, found, "init", "", new String[] {"java/lang/Exception"});
        cw.visitEnd();
        return cw.toByteArray();
    }

    /// One of the three methods that take a controller: the first class the
    /// controller is an instance of answers, through its part class.
    private void delegate(ClassWriter cw, List<Controller> found, String method, String rest, String[] throwing) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method, "(Ljava/lang/Object;" + rest + ")Z", null,
                throwing);
        mv.visitCode();
        int extra = rest.length() == 0 ? 0 : 2;
        for (int i = 0; i < found.size(); i++) {
            String type = found.get(i).type.name;
            Label next = new Label();
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, type);
            mv.visitJumpInsn(Opcodes.IFEQ, next);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, type);
            for (int a = 0; a < extra; a++) {
                mv.visitVarInsn(Opcodes.ALOAD, 2 + a);
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, names.registry + PART + i, method, "(L" + type + ";" + rest
                    + ")Z", false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(next);
        }
        answer(mv, false);
        end(mv);
    }

    private byte[] part(String name, Controller c) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        end(mv);
        String self = "L" + c.type.name + ";";

        mv = cw.visitMethod(Opcodes.ACC_STATIC, "inject", "(" + self + "Ljava/lang/String;Ljava/lang/Object;)Z",
                null, null);
        mv.visitCode();
        for (Member f : c.fields) {
            Type type = Type.getType(f.descriptor);
            Label next = new Label();
            Label set = new Label();
            ifNotNamed(mv, f.name, 1, next);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitJumpInsn(Opcodes.IFNULL, set);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitTypeInsn(Opcodes.INSTANCEOF, type.getInternalName());
            mv.visitJumpInsn(Opcodes.IFNE, set);
            // Not a value of the field's type: say so rather than cast.
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(type.getClassName());
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, names.dispatch, "mismatch",
                    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false);
            answer(mv, true);
            mv.visitLabel(set);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitTypeInsn(Opcodes.CHECKCAST, type.getInternalName());
            mv.visitFieldInsn(Opcodes.PUTFIELD, f.owner, f.name, f.descriptor);
            answer(mv, true);
            mv.visitLabel(next);
        }
        answer(mv, false);
        end(mv);

        mv = cw.visitMethod(Opcodes.ACC_STATIC, "invoke", "(" + self + "Ljava/lang/String;Ljava/lang/Object;)Z",
                null, new String[] {"java/lang/Exception"});
        mv.visitCode();
        // The form that takes the event first, so that it is preferred
        // whenever the event is of its type.
        for (int pass = 1; pass >= 0; pass--) {
            for (Member m : c.handlers) {
                Type[] args = Type.getArgumentTypes(m.descriptor);
                if (args.length != pass) {
                    continue;
                }
                Label next = new Label();
                ifNotNamed(mv, m.name, 1, next);
                boolean isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
                if (pass == 1) {
                    mv.visitVarInsn(Opcodes.ALOAD, 2);
                    mv.visitTypeInsn(Opcodes.INSTANCEOF, args[0].getInternalName());
                    mv.visitJumpInsn(Opcodes.IFEQ, next);
                }
                if (!isStatic) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                }
                if (pass == 1) {
                    mv.visitVarInsn(Opcodes.ALOAD, 2);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, args[0].getInternalName());
                }
                mv.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, m.owner, m.name,
                        m.descriptor, false);
                int size = Type.getReturnType(m.descriptor).getSize();
                if (size == 1) {
                    mv.visitInsn(Opcodes.POP);
                } else if (size == 2) {
                    mv.visitInsn(Opcodes.POP2);
                }
                answer(mv, true);
                mv.visitLabel(next);
            }
        }
        answer(mv, false);
        end(mv);

        mv = cw.visitMethod(Opcodes.ACC_STATIC, "init", "(" + self + ")Z", null,
                new String[] {"java/lang/Exception"});
        mv.visitCode();
        if (c.initialize != null) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, c.initialize.owner, "initialize", c.initialize.descriptor,
                    false);
            int size = Type.getReturnType(c.initialize.descriptor).getSize();
            if (size == 1) {
                mv.visitInsn(Opcodes.POP);
            } else if (size == 2) {
                mv.visitInsn(Opcodes.POP2);
            }
            answer(mv, true);
        } else {
            answer(mv, false);
        }
        end(mv);
        cw.visitEnd();
        return cw.toByteArray();
    }

    /// The classes of a directory as internal names, for a caller that has
    /// no list of its own.
    public static List<String> classesIn(File dir) {
        List<String> out = new ArrayList<String>();
        collect(dir, "", out);
        Collections.sort(out);
        return out;
    }

    private static void collect(File dir, String prefix, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, prefix + f.getName() + "/", out);
            } else if (f.getName().endsWith(".class")) {
                out.add(prefix + f.getName().substring(0, f.getName().length() - 6));
            }
        }
    }

    /// The names of an unrelocated runtime, as a test of this module or of
    /// the layer sees it, with the registry under a name of the caller's.
    public static Names unrelocated(String registry) {
        return new Names(registry, DISPATCH, CONTEXT, ANNOTATION);
    }
}
