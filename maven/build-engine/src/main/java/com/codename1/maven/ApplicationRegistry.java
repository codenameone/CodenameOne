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

import com.codename1.build.Log;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Generates `com.codename1.generated.compat.CompatRegistry`, the registry
/// the class library's `java.util.ResourceBundle` and `java.util.ServiceLoader`
/// read through `com.codename1.impl.JdkHost` on a device.
///
/// It extends the core's `com.codename1.impl.compat.CompatRegistryBase`, and
/// its static `cn1Install()` -- which does its work once, however often it is
/// called -- records:
///
/// - every shipped resource path in `com.codename1.impl.compat.Resources`'s
///   index;
/// - every class the application may need created by name, its resource
///   bundle classes and its service providers, each under a number that
///   `cn1Create(int)` turns into a `new` and `cn1Type(int)` into a class
///   constant, since a device creates nothing by name;
/// - the providers of each service, in the order they were listed;
///
/// and then installs itself as the registry `JdkHost` serves.
///
/// The name is in a package of its own, never in a runtime jar, so that no
/// placeholder of the same name ships anywhere: an Android build would see
/// two classes of one name. The registrations sit in part classes beside it,
/// `CompatRegistry$Part0` onwards, each with one static `install` method,
/// because a method's code is limited to 64KB and an application has as many
/// resources as it has.
final class ApplicationRegistry {

    /// The generated class, by internal name.
    static final String NAME = "com/codename1/generated/compat/CompatRegistry";
    static final String BASE = "com/codename1/impl/compat/CompatRegistryBase";
    static final String RESOURCES = "com/codename1/impl/compat/Resources";
    private static final String PART = "$Part";
    /// How many registrations one part holds.
    private static final int CHUNK = 1000;

    private ApplicationRegistry() {
    }

    /// The registry and its parts, by internal name.
    ///
    /// #### Parameters
    ///
    /// - `resources`: the shipped resource paths, without a leading slash
    ///
    /// - `bundleClasses`: the application's resource bundle classes, dotted
    ///
    /// - `services`: each `{service, provider}`, the service dotted as
    ///   `Class.getName()` answers it on the device and the provider an
    ///   internal name, in the order `ServiceLoader` is to offer them
    static Map<String, byte[]> generate(List<String> resources, List<String> bundleClasses,
                                        List<String[]> services) {
        // Every class created by name, numbered in order of first mention.
        Map<String, Integer> ids = new LinkedHashMap<String, Integer>();
        for (String bundle : bundleClasses) {
            id(ids, bundle.replace('.', '/'));
        }
        for (String[] s : services) {
            id(ids, s[1]);
        }
        List<String> classes = new ArrayList<String>(ids.keySet());

        Map<String, byte[]> out = new TreeMap<String, byte[]>();
        int total = resources.size() + classes.size() + services.size();
        List<String> parts = new ArrayList<String>();
        for (int from = 0; from < total; from += CHUNK) {
            String part = NAME + PART + parts.size();
            parts.add(part);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cw.visit(Opcodes.V1_5, Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, part, null, "java/lang/Object", null);
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_STATIC, "install", "(L" + NAME + ";)V", null, null);
            mv.visitCode();
            for (int i = from; i < total && i < from + CHUNK; i++) {
                if (i < resources.size()) {
                    mv.visitLdcInsn(resources.get(i));
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, RESOURCES, "cn1AddResource", "(Ljava/lang/String;)V",
                            false);
                } else if (i < resources.size() + classes.size()) {
                    int id = i - resources.size();
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitLdcInsn(classes.get(id).replace('/', '.'));
                    mv.visitLdcInsn(Integer.valueOf(id));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NAME, "cn1Class", "(Ljava/lang/String;I)V", false);
                } else {
                    String[] s = services.get(i - resources.size() - classes.size());
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitLdcInsn(s[0]);
                    mv.visitLdcInsn(s[1].replace('/', '.'));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NAME, "cn1Provider",
                            "(Ljava/lang/String;Ljava/lang/String;)V", false);
                }
            }
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            cw.visitEnd();
            out.put(part, cw.toByteArray());
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, NAME, null, BASE, null);

        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, BASE, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor install = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "cn1Install", "()V", null,
                null);
        install.visitCode();
        Label first = new Label();
        install.visitMethodInsn(Opcodes.INVOKESTATIC, BASE, "cn1Installed", "()Z", false);
        install.visitJumpInsn(Opcodes.IFEQ, first);
        install.visitInsn(Opcodes.RETURN);
        install.visitLabel(first);
        install.visitTypeInsn(Opcodes.NEW, NAME);
        install.visitInsn(Opcodes.DUP);
        install.visitMethodInsn(Opcodes.INVOKESPECIAL, NAME, "<init>", "()V", false);
        install.visitVarInsn(Opcodes.ASTORE, 0);
        install.visitMethodInsn(Opcodes.INVOKESTATIC, RESOURCES, "cn1BeginIndex", "()V", false);
        for (String part : parts) {
            install.visitVarInsn(Opcodes.ALOAD, 0);
            install.visitMethodInsn(Opcodes.INVOKESTATIC, part, "install", "(L" + NAME + ";)V", false);
        }
        install.visitVarInsn(Opcodes.ALOAD, 0);
        install.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NAME, "cn1Activate", "()V", false);
        install.visitInsn(Opcodes.RETURN);
        install.visitMaxs(0, 0);
        install.visitEnd();

        // What a class is, which is asked without creating one, and the
        // class's new instance.
        for (int half = 0; half < 2; half++) {
            MethodVisitor mv = half == 0
                    ? cw.visitMethod(Opcodes.ACC_PROTECTED, "cn1Create", "(I)Ljava/lang/Object;", null, null)
                    : cw.visitMethod(Opcodes.ACC_PROTECTED, "cn1Type", "(I)Ljava/lang/Class;", null, null);
            mv.visitCode();
            Label absent = new Label();
            if (!classes.isEmpty()) {
                Label[] cases = new Label[classes.size()];
                for (int i = 0; i < cases.length; i++) {
                    cases[i] = new Label();
                }
                mv.visitVarInsn(Opcodes.ILOAD, 1);
                mv.visitTableSwitchInsn(0, cases.length - 1, absent, cases);
                for (int i = 0; i < cases.length; i++) {
                    String type = classes.get(i);
                    mv.visitLabel(cases[i]);
                    if (half == 0) {
                        mv.visitTypeInsn(Opcodes.NEW, type);
                        mv.visitInsn(Opcodes.DUP);
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, type, "<init>", "()V", false);
                    } else {
                        mv.visitLdcInsn(Type.getObjectType(type));
                    }
                    mv.visitInsn(Opcodes.ARETURN);
                }
            }
            mv.visitLabel(absent);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();
        out.put(NAME, cw.toByteArray());
        return out;
    }

    /// Removes the registry and its parts from a list of class names
    /// collected from a classes directory an earlier build wrote into.
    static void removeGenerated(List<String> internalNames) {
        for (Iterator<String> it = internalNames.iterator(); it.hasNext();) {
            if (isGenerated(it.next())) {
                it.remove();
            }
        }
    }

    /// Whether `internalName` is the registry or one of its parts: written
    /// by the build, never by the application.
    static boolean isGenerated(String internalName) {
        return internalName.equals(NAME) || internalName.startsWith(NAME + PART);
    }

    private static void id(Map<String, Integer> ids, String internalName) {
        if (!ids.containsKey(internalName)) {
            ids.put(internalName, Integer.valueOf(ids.size()));
        }
    }

    /// Writes the registry's classes into `classesDir` and removes the parts
    /// an earlier, larger registry left behind.
    static void write(File classesDir, Map<String, byte[]> classes, Log log) throws IOException {
        File dir = new File(classesDir, NAME + ".class").getParentFile();
        String pkg = NAME.substring(0, NAME.lastIndexOf('/') + 1);
        String prefix = NAME.substring(pkg.length()) + PART;
        File[] existing = dir.listFiles();
        if (existing != null) {
            for (File f : existing) {
                String name = f.getName();
                if (name.startsWith(prefix) && name.endsWith(".class")
                        && !classes.containsKey(pkg + name.substring(0, name.length() - 6))
                        && !f.delete()) {
                    log.warn("Could not delete " + f);
                }
            }
        }
        for (Map.Entry<String, byte[]> e : classes.entrySet()) {
            ClassRelocator.writeIfDifferent(new File(classesDir, e.getKey() + ".class"), e.getValue());
        }
    }
}
