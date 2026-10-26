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
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/// What the FXML compiler knows about the classes a document names: their
/// constructors, setters, getters and constants, read out of the class
/// files on the application's class path.
///
/// No class is ever loaded. The class path is the application's, built for
/// a device, and running a static initializer of it inside the build would
/// be running device code on the build machine; a class file read as data
/// says everything the compiler asks. Classes of the JDK itself are read
/// the same way, as resources of the build's own runtime.
final class ClassModel implements Closeable {

    /// One method or constructor.
    static final class Method {
        final String name;
        final String descriptor;
        final String signature;
        final int access;
        final Type[] parameters;
        /// The `@NamedArg` name of each parameter, `null` where one has
        /// none.
        final String[] argNames;
        /// The `@NamedArg` default of each parameter, `null` where none
        /// was given.
        final String[] argDefaults;

        Method(String name, String descriptor, String signature, int access) {
            this.name = name;
            this.descriptor = descriptor;
            this.signature = signature;
            this.access = access;
            this.parameters = Type.getArgumentTypes(descriptor);
            this.argNames = new String[parameters.length];
            this.argDefaults = new String[parameters.length];
        }

        boolean isPublic() {
            return (access & Opcodes.ACC_PUBLIC) != 0;
        }

        boolean isStatic() {
            return (access & Opcodes.ACC_STATIC) != 0;
        }

        Type returnType() {
            return Type.getReturnType(descriptor);
        }

        boolean allArgsNamed() {
            for (int i = 0; i < argNames.length; i++) {
                if (argNames[i] == null) {
                    return false;
                }
            }
            return true;
        }
    }

    /// One field.
    static final class Field {
        final String name;
        final String descriptor;
        final int access;

        Field(String name, String descriptor, int access) {
            this.name = name;
            this.descriptor = descriptor;
            this.access = access;
        }
    }

    /// One class.
    static final class Info {
        String name;
        String superName;
        String[] interfaces = new String[0];
        int access;
        String defaultProperty;
        final List<Method> methods = new ArrayList<Method>();
        final List<Field> fields = new ArrayList<Field>();

        boolean isEnum() {
            return (access & Opcodes.ACC_ENUM) != 0;
        }

        boolean isAbstract() {
            return (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0;
        }

        boolean isPublic() {
            return (access & Opcodes.ACC_PUBLIC) != 0;
        }

        /// The name as Java source spells it.
        String sourceName() {
            return name.replace('/', '.').replace('$', '.');
        }
    }

    private static final Info ABSENT = new Info();

    private final List<File> directories = new ArrayList<File>();
    private final List<ZipFile> jars = new ArrayList<ZipFile>();
    private final Map<String, Info> cache = new HashMap<String, Info>();
    private final String namedArg;
    private final String defaultProperty;

    /// Creates a model over a class path of directories and jars. Entries
    /// that do not exist are ignored, as javac ignores them.
    ClassModel(List<File> classpath) throws IOException {
        this(classpath, "Ljavafx/beans/NamedArg;", "Ljavafx/beans/DefaultProperty;");
    }

    ClassModel(List<File> classpath, String namedArgDescriptor, String defaultPropertyDescriptor)
            throws IOException {
        this.namedArg = namedArgDescriptor;
        this.defaultProperty = defaultPropertyDescriptor;
        for (File f : classpath) {
            if (f.isDirectory()) {
                directories.add(f);
            } else if (f.isFile()) {
                jars.add(new ZipFile(f));
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOException first = null;
        for (ZipFile jar : jars) {
            try {
                jar.close();
            } catch (IOException e) {
                first = first == null ? e : first;
            }
        }
        jars.clear();
        if (first != null) {
            throw first;
        }
    }

    private byte[] bytes(String internalName) throws IOException {
        String entry = internalName + ".class";
        for (File dir : directories) {
            File f = new File(dir, entry);
            if (f.isFile()) {
                InputStream in = new FileInputStream(f);
                try {
                    return drain(in);
                } finally {
                    in.close();
                }
            }
        }
        for (ZipFile jar : jars) {
            ZipEntry e = jar.getEntry(entry);
            if (e != null) {
                InputStream in = jar.getInputStream(e);
                try {
                    return drain(in);
                } finally {
                    in.close();
                }
            }
        }
        if (internalName.startsWith("java/")) {
            InputStream in = ClassLoader.getSystemResourceAsStream(entry);
            if (in != null) {
                try {
                    return drain(in);
                } finally {
                    in.close();
                }
            }
        }
        return null;
    }

    private static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /// The class of an internal name (`javafx/scene/control/Button`), or
    /// `null` when the class path has none.
    Info find(String internalName) throws IOException {
        Info known = cache.get(internalName);
        if (known == null) {
            byte[] bytes = bytes(internalName);
            known = bytes == null ? ABSENT : read(bytes);
            cache.put(internalName, known);
        }
        return known == ABSENT ? null : known;
    }

    private Info read(byte[] bytes) {
        final Info info = new Info();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                info.name = name;
                info.superName = superName;
                info.access = access;
                info.interfaces = interfaces == null ? new String[0] : interfaces;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!defaultProperty.equals(descriptor)) {
                    return null;
                }
                return new AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String name, Object value) {
                        if ("value".equals(name) && value instanceof String) {
                            info.defaultProperty = (String) value;
                        }
                    }
                };
            }

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                    Object value) {
                info.fields.add(new Field(name, descriptor, access));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                if ((access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0) {
                    return null;
                }
                final Method m = new Method(name, descriptor, signature, access);
                info.methods.add(m);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitParameterAnnotation(final int parameter, String annotation,
                            boolean visible) {
                        if (!namedArg.equals(annotation) || parameter >= m.argNames.length) {
                            return null;
                        }
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(String key, Object value) {
                                if ("value".equals(key) && value instanceof String) {
                                    m.argNames[parameter] = (String) value;
                                } else if ("defaultValue".equals(key) && value instanceof String) {
                                    m.argDefaults[parameter] = (String) value;
                                }
                            }
                        };
                    }
                };
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return info;
    }

    /// Whether a value of the class `from` can be assigned to a variable of
    /// the class `to`. A class the class path does not have is assumed not
    /// to be assignable to anything but itself and `Object`.
    boolean isAssignable(String from, String to) throws IOException {
        if (from.equals(to) || "java/lang/Object".equals(to)) {
            return true;
        }
        Info info = find(from);
        if (info == null) {
            return false;
        }
        if (info.superName != null && isAssignable(info.superName, to)) {
            return true;
        }
        for (String i : info.interfaces) {
            if (isAssignable(i, to)) {
                return true;
            }
        }
        return false;
    }

    /// The public methods named `name` of a class and of everything it
    /// extends or implements, the most derived first.
    List<Method> methods(String internalName, String name) throws IOException {
        List<Method> out = new ArrayList<Method>();
        collect(internalName, name, out, new ArrayList<String>());
        return out;
    }

    private void collect(String internalName, String name, List<Method> out, List<String> seen)
            throws IOException {
        if (internalName == null || seen.contains(internalName)) {
            return;
        }
        seen.add(internalName);
        Info info = find(internalName);
        if (info == null) {
            return;
        }
        for (Method m : info.methods) {
            if (m.name.equals(name) && m.isPublic()) {
                boolean shadowed = false;
                for (Method have : out) {
                    shadowed |= have.descriptor.equals(m.descriptor);
                }
                if (!shadowed) {
                    out.add(m);
                }
            }
        }
        collect(info.superName, name, out, seen);
        for (String i : info.interfaces) {
            collect(i, name, out, seen);
        }
    }

    /// The default property of a class: what `@DefaultProperty` says on it
    /// or on the nearest class above it that has one, or `null`.
    String annotatedDefaultProperty(String internalName) throws IOException {
        String at = internalName;
        while (at != null) {
            Info info = find(at);
            if (info == null) {
                return null;
            }
            if (info.defaultProperty != null) {
                return info.defaultProperty;
            }
            at = info.superName;
        }
        return null;
    }
}
