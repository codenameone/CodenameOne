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
package com.codename1.cil.translate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// What the Java runtime library actually provides, read from its compiled
/// classes.
///
/// C# compiles against reference assemblies that declare all of .NET, and the
/// runtime implements a small part of it. Without this, a call to a missing
/// method would translate cleanly and fail as a `NoSuchMethodError` on the
/// device, on the one code path that reaches it. Every reference the
/// translator emits into the runtime is checked here instead, and the misses
/// are reported together when the translation ends.
final class RuntimeIndex {
    private static final class Info {
        String superName;
        String[] interfaces;
        final Set<String> members = new HashSet<String>();
    }

    private final Map<String, Info> classes = new HashMap<String, Info>();
    /// What is missing, and for each the places that use it.
    private final Map<String, Set<String>> missing = new TreeMap<String, Set<String>>();

    void addDirectory(File dir) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                addDirectory(child);
            } else if (child.getName().endsWith(".class")) {
                add(Files.readAllBytes(child.toPath()));
            }
        }
    }

    private void add(byte[] classFile) {
        InfoReader reader = new InfoReader();
        new ClassReader(classFile).accept(reader,
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        classes.put(reader.name, reader.info);
    }

    /// Collects one class file's name, supertypes and member descriptors.
    private static final class InfoReader extends ClassVisitor {
        final Info info = new Info();
        String name;

        InfoReader() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String className, String signature, String superName,
                String[] interfaces) {
            name = className;
            info.superName = superName;
            info.interfaces = interfaces;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            info.members.add(methodName + descriptor);
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String fieldName, String descriptor, String signature,
                Object value) {
            info.members.add(fieldName + ":" + descriptor);
            return null;
        }
    }

    private boolean has(String owner, String member) {
        Info info = classes.get(owner);
        if (info == null) {
            return !owner.startsWith(Names.RUNTIME) && jdkHas(owner, member);
        }
        if (info.members.contains(member)) {
            return true;
        }
        if (info.superName != null && has(info.superName, member)) {
            return true;
        }
        if (info.interfaces != null) {
            for (String i : info.interfaces) {
                if (has(i, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// A member a runtime class inherits from the JDK, looked up in the JDK
    /// this tool runs on. Answering "yes" for every class outside the runtime
    /// would make the whole index vacuous: every lookup that misses climbs to
    /// `java.lang.Object`, and would be waved through there.
    private static boolean jdkHas(String owner, String member) {
        Class<?> c;
        try {
            c = Class.forName(owner.replace('/', '.'), false, RuntimeIndex.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            // Not a class this tool can see, so not one it can judge.
            return true;
        } catch (LinkageError e) {
            return true;
        }
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            if (declares(k, member)) {
                return true;
            }
            for (Class<?> i : k.getInterfaces()) {
                if (declares(i, member)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean declares(Class<?> c, String member) {
        for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
            if ((m.getName() + org.objectweb.asm.Type.getMethodDescriptor(m)).equals(member)) {
                return true;
            }
        }
        for (java.lang.reflect.Constructor<?> m : c.getDeclaredConstructors()) {
            if (("<init>" + org.objectweb.asm.Type.getConstructorDescriptor(m)).equals(member)) {
                return true;
            }
        }
        for (java.lang.reflect.Field f : c.getDeclaredFields()) {
            if ((f.getName() + ":" + org.objectweb.asm.Type.getDescriptor(f.getType())).equals(member)) {
                return true;
            }
        }
        return false;
    }

    private void miss(String what, String neededFor) {
        Set<String> uses = missing.get(what);
        if (uses == null) {
            uses = new TreeSet<String>();
            missing.put(what, uses);
        }
        uses.add(neededFor);
    }

    void requireClass(String owner, String neededFor) {
        if (!classes.isEmpty() && owner.startsWith(Names.RUNTIME) && !classes.containsKey(owner)) {
            miss("class  " + owner, neededFor);
        }
    }

    void requireMethod(String owner, String name, String descriptor, String neededFor) {
        if (!classes.isEmpty() && owner.startsWith(Names.RUNTIME) && !has(owner, name + descriptor)) {
            miss("method " + owner + "." + name + descriptor, neededFor);
        }
    }

    void requireField(String owner, String name, String descriptor, String neededFor) {
        if (!classes.isEmpty() && owner.startsWith(Names.RUNTIME) && !has(owner, name + ":" + descriptor)) {
            miss("field  " + owner + "." + name + " " + descriptor, neededFor);
        }
    }

    /// How many members are missing.
    int missingCount() {
        return missing.size();
    }

    /// The report: a line for each missing member, and under it a line for
    /// each user of it -- `File.cs:line` and the method, when the assembly
    /// came with its PDB, the method alone when it did not.
    List<String> missing() {
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Set<String>> e : missing.entrySet()) {
            out.add(e.getKey());
            for (String use : e.getValue()) {
                out.add("    used by " + use);
            }
        }
        return out;
    }
}
