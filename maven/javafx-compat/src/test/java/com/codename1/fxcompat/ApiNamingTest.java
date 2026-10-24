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
package com.codename1.fxcompat;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/// Holds the `javafx.*` classes to the rule that keeps them honest: a
/// public or protected member JavaFX does not have is named `cn1...` (or
/// `CN1_...` for a constant), and every such member is listed in
/// `cn1-hooks.txt`.
///
/// The list is what makes a hook a decision. Application code compiles
/// against these classes under the real names, so a hook is one more
/// member an application could call by accident and then fail to compile
/// against JavaFX itself; adding one takes a line in a file a reviewer
/// reads.
///
/// An entry is `binary.class.Name#member`, named for the class that
/// introduces the hook; overrides in subclasses are covered by it.
public class ApiNamingTest {

    private static final class Info {
        String name;
        String superName;
        String[] interfaces;
        boolean exported;
        final Set<String> hooks = new TreeSet<String>();
    }

    private static boolean isHookName(String name) {
        return name.startsWith("cn1") || name.startsWith("CN1_");
    }

    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".class")) {
                out.add(f);
            }
        }
    }

    private static Info read(File file) throws IOException {
        final Info info = new Info();
        InputStream in = new FileInputStream(file);
        try {
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName,
                        String[] interfaces) {
                    info.name = name;
                    info.superName = superName;
                    info.interfaces = interfaces;
                    info.exported = (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
                }

                @Override
                public void visitInnerClass(String name, String outerName, String innerName, int access) {
                    if (name.equals(info.name)) {
                        info.exported = (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
                    }
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                        Object value) {
                    member(access, name);
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                    member(access, name);
                    return null;
                }

                private void member(int access, String name) {
                    if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                            && (access & Opcodes.ACC_SYNTHETIC) == 0 && isHookName(name)) {
                        info.hooks.add(name);
                    }
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } finally {
            in.close();
        }
        return info;
    }

    /// Collects the topmost classes of a hierarchy that declare a member:
    /// the ones that introduce it.
    private static boolean introducers(Map<String, Info> classes, String owner, String member, Set<String> out) {
        Info info = owner == null ? null : classes.get(owner);
        if (info == null) {
            return false;
        }
        boolean above = introducers(classes, info.superName, member, out);
        if (info.interfaces != null) {
            for (String i : info.interfaces) {
                above |= introducers(classes, i, member, out);
            }
        }
        if (!above && info.hooks.contains(member)) {
            out.add(info.name.replace('/', '.') + "#" + member);
            return true;
        }
        return above;
    }

    @Test
    public void everyHookIsListedAndEveryListedHookExists() throws IOException {
        File root = new File("target/classes");
        assertTrue("run after compile: " + root.getAbsolutePath(), root.isDirectory());
        List<File> files = new ArrayList<File>();
        collect(root, files);
        Map<String, Info> classes = new HashMap<String, Info>();
        for (File f : files) {
            Info info = read(f);
            classes.put(info.name, info);
        }

        Set<String> listed = new HashSet<String>();
        InputStream in = ApiNamingTest.class.getResourceAsStream("/cn1-hooks.txt");
        assertTrue("cn1-hooks.txt is missing", in != null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    listed.add(line);
                }
            }
        } finally {
            reader.close();
        }

        Set<String> required = new HashSet<String>();
        Set<String> unlisted = new TreeSet<String>();
        for (Info info : classes.values()) {
            for (String hook : info.hooks) {
                // A nested class is as visible as its outermost holder lets it be,
                // and javac records that only on the inner class entry.
                if (info.name.startsWith("javafx/") && info.exported) {
                    Set<String> owners = new TreeSet<String>();
                    introducers(classes, info.name, hook, owners);
                    for (String entry : owners) {
                        required.add(entry);
                        if (!listed.contains(entry)) {
                            unlisted.add(entry);
                        }
                    }
                }
            }
        }
        List<String> stale = new ArrayList<String>();
        for (String entry : listed) {
            if (!required.contains(entry)) {
                stale.add(entry);
            }
        }
        Collections.sort(stale);
        if (!unlisted.isEmpty() || !stale.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("cn1-hooks.txt is out of date.\nAdd (each is API an application can see):\n");
            for (String s : unlisted) {
                sb.append(s).append('\n');
            }
            sb.append("Remove (no javafx class has or inherits it, or a superclass introduces it):\n");
            for (String s : stale) {
                sb.append(s).append('\n');
            }
            fail(sb.toString());
        }
    }
}
