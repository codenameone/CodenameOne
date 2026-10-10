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
package com.codename1.desktopcompat.com.formdev.flatlaf;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Test;

/// The layer's `com.formdev.flatlaf` classes held to the real FlatLaf and
/// FlatLaf Extras, which are test dependencies of this module for that
/// purpose alone.
///
/// An application compiles against the real jars and is then pointed at
/// these classes, so a member here that the real class lacks, or has with
/// another descriptor, is a member no application can reach, and a class
/// here must be what the real one is: interface or class, with a
/// superclass that is one of the real class's. Members named `cn1...`
/// are the layer's own.
///
/// The other direction is checked too: every public class of the real
/// jars is in the layer or is named in `flatlaf-absent.txt`, so leaving
/// one out is a decision that was written down.
public class FlatLafApiShapeTest {

    private static final String PREFIX = "com.codename1.desktopcompat.";

    private final List<String> problems = new ArrayList<String>();
    private final Set<String> ours = new HashSet<String>();

    private static boolean api(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    private static Class<?> real(String name) {
        try {
            return Class.forName(name, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError e) {
            return null;
        }
    }

    /// The real type a type of ours stands for, or null when it is a
    /// layer type with no counterpart.
    private static Class<?> map(Class<?> c) {
        if (c.isArray()) {
            Class<?> component = map(c.getComponentType());
            return component == null ? null : java.lang.reflect.Array.newInstance(component, 0).getClass();
        }
        return c.getName().startsWith(PREFIX) ? real(c.getName().substring(PREFIX.length())) : c;
    }

    private static Class<?>[] mapAll(Class<?>[] types) {
        Class<?>[] out = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            out[i] = map(types[i]);
            if (out[i] == null) {
                return null;
            }
        }
        return out;
    }

    private static Method findMethod(Class<?> in, String name, Class<?>[] params, Class<?> ret) {
        if (in == null) {
            return null;
        }
        for (Method m : in.getDeclaredMethods()) {
            if (m.getName().equals(name) && api(m.getModifiers()) && m.getReturnType() == ret
                    && java.util.Arrays.equals(m.getParameterTypes(), params)) {
                return m;
            }
        }
        Method up = findMethod(in.getSuperclass(), name, params, ret);
        for (Class<?> i : in.getInterfaces()) {
            if (up == null) {
                up = findMethod(i, name, params, ret);
            }
        }
        return up;
    }

    private static Field findField(Class<?> in, String name, Class<?> type) {
        if (in == null) {
            return null;
        }
        for (Field f : in.getDeclaredFields()) {
            if (f.getName().equals(name) && api(f.getModifiers()) && f.getType() == type) {
                return f;
            }
        }
        Field up = findField(in.getSuperclass(), name, type);
        for (Class<?> i : in.getInterfaces()) {
            if (up == null) {
                up = findField(i, name, type);
            }
        }
        return up;
    }

    private void walk(File dir, String pkg) throws Exception {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                walk(f, pkg + "." + f.getName());
            } else if (f.getName().endsWith(".class") && !f.getName().equals("package-info.class")) {
                String simple = f.getName().substring(0, f.getName().length() - 6);
                check(Class.forName(pkg + "." + simple, false, FlatLafApiShapeTest.class.getClassLoader()));
            }
        }
    }

    private void check(Class<?> mine) {
        for (Class<?> k = mine; k != null; k = k.getDeclaringClass()) {
            if (!api(k.getModifiers())) {
                return;
            }
        }
        if (mine.isAnonymousClass() || mine.isLocalClass()) {
            return;
        }
        String name = mine.getName();
        ours.add(name.substring(PREFIX.length()));
        Class<?> real = real(name.substring(PREFIX.length()));
        if (real == null) {
            problems.add(name + ": FlatLaf has no such class");
            return;
        }
        if (mine.isInterface() != real.isInterface()) {
            problems.add(name + ": interface here but not in FlatLaf, or the reverse");
        }
        if (Modifier.isFinal(real.getModifiers()) != Modifier.isFinal(mine.getModifiers())) {
            problems.add(name + ": final differs from FlatLaf");
        }
        if (Modifier.isAbstract(mine.getModifiers()) && !Modifier.isAbstract(real.getModifiers())) {
            problems.add(name + ": abstract here but instantiable in FlatLaf");
        }
        Class<?> sup = mine.getSuperclass();
        if (sup != null && sup != Object.class) {
            Class<?> mapped = map(sup);
            if (mapped == null || !mapped.isAssignableFrom(real) || mapped.isInterface()) {
                problems.add(name + ": superclass " + sup.getName() + " is not an ancestor of the FlatLaf class");
            }
        }
        for (Class<?> i : mine.getInterfaces()) {
            Class<?> mapped = map(i);
            if (mapped == null || !mapped.isAssignableFrom(real)) {
                problems.add(name + ": interface " + i.getName() + " is not implemented by the FlatLaf class");
            }
        }
        for (Constructor<?> c : mine.getDeclaredConstructors()) {
            if (!api(c.getModifiers()) || c.isSynthetic()) {
                continue;
            }
            Class<?>[] params = mapAll(c.getParameterTypes());
            boolean found = false;
            if (params != null) {
                try {
                    found = api(real.getDeclaredConstructor(params).getModifiers());
                } catch (NoSuchMethodException e) {
                    found = false;
                }
            }
            if (!found) {
                problems.add(name + ": constructor " + c + " is not in FlatLaf");
            }
        }
        for (Method m : mine.getDeclaredMethods()) {
            if (!api(m.getModifiers()) || m.isSynthetic() || m.isBridge() || m.getName().startsWith("cn1")) {
                continue;
            }
            Class<?>[] params = mapAll(m.getParameterTypes());
            Class<?> ret = map(m.getReturnType());
            Method found = params == null || ret == null ? null : findMethod(real, m.getName(), params, ret);
            if (found == null) {
                problems.add(name + ": method " + m + " is not in FlatLaf");
            } else if (Modifier.isStatic(found.getModifiers()) != Modifier.isStatic(m.getModifiers())) {
                problems.add(name + ": method " + m + " is static here but not in FlatLaf, or the reverse");
            } else if (Modifier.isAbstract(m.getModifiers()) && !Modifier.isAbstract(found.getModifiers())) {
                problems.add(name + ": method " + m + " is abstract here but concrete in FlatLaf");
            }
        }
        for (Field f : mine.getDeclaredFields()) {
            if (!api(f.getModifiers()) || f.isSynthetic() || f.getName().startsWith("cn1")) {
                continue;
            }
            Class<?> type = map(f.getType());
            Field found = type == null ? null : findField(real, f.getName(), type);
            if (found == null) {
                problems.add(name + ": field " + f + " is not in FlatLaf");
            } else if (Modifier.isStatic(found.getModifiers()) != Modifier.isStatic(f.getModifiers())) {
                problems.add(name + ": field " + f + " is static here but not in FlatLaf, or the reverse");
            } else if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class
                    && Modifier.isFinal(f.getModifiers())) {
                constant(name, f, found);
            }
        }
    }

    /// A string constant is compiled into the application, so ours is
    /// only ever read for the same value; it has to be that value.
    private void constant(String name, Field mine, Field theirs) {
        try {
            mine.setAccessible(true);
            theirs.setAccessible(true);
            Object a = mine.get(null);
            Object b = theirs.get(null);
            if (a == null ? b != null : !a.equals(b)) {
                problems.add(name + ": constant " + mine.getName() + " is \"" + a + "\" here and \"" + b
                        + "\" in FlatLaf");
            }
        } catch (IllegalAccessException e) {
            problems.add(name + ": constant " + mine.getName() + " cannot be read: " + e);
        } catch (LinkageError e) {
            problems.add(name + ": constant " + mine.getName() + " cannot be read: " + e);
        }
    }

    private static File jarOf(String className) throws Exception {
        Class<?> c = Class.forName(className, false, ClassLoader.getSystemClassLoader());
        return new File(c.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @Test
    public void everyClassMatchesFlatLafAndEveryAbsentClassIsWrittenDown() throws Exception {
        File root = new File(System.getProperty("swingcompat.classes", "target/classes"));
        walk(new File(root, "com/codename1/desktopcompat/com/formdev"), PREFIX + "com.formdev");
        assertTrue("no classes found", !ours.isEmpty());

        Set<String> absent = new HashSet<String>();
        List<String> packages = new ArrayList<String>();
        InputStream in = FlatLafApiShapeTest.class.getResourceAsStream("flatlaf-absent.txt");
        assertNotNull("flatlaf-absent.txt", in);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                if (line.endsWith(".*")) {
                    packages.add(line.substring(0, line.length() - 1));
                } else {
                    absent.add(line);
                }
            }
        } finally {
            reader.close();
        }
        Set<String> theirs = new HashSet<String>();
        File[] jars = {jarOf("com.formdev.flatlaf.FlatLaf"), jarOf("com.formdev.flatlaf.extras.FlatAnimatedLafChange")};
        for (File jar : jars) {
            ZipFile zip = new ZipFile(jar);
            try {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    String entry = entries.nextElement().getName();
                    if (entry.startsWith("com/formdev/") && entry.endsWith(".class") && entry.indexOf('$') < 0) {
                        theirs.add(entry.substring(0, entry.length() - 6).replace('/', '.'));
                    }
                }
            } finally {
                zip.close();
            }
        }
        assertTrue("the real jars were read", theirs.size() > 100);
        for (String name : theirs) {
            if (ours.contains(name) || absent.contains(name)) {
                continue;
            }
            boolean whole = false;
            for (String p : packages) {
                whole |= name.startsWith(p) && name.indexOf('.', p.length()) < 0;
            }
            Class<?> c = whole ? null : real(name);
            if (!whole && c != null && Modifier.isPublic(c.getModifiers())) {
                problems.add(name + ": a public FlatLaf class that is neither in the layer nor in flatlaf-absent.txt");
            }
        }
        for (String name : absent) {
            if (ours.contains(name)) {
                problems.add(name + ": named in flatlaf-absent.txt, and the layer has it");
            } else if (!theirs.contains(name)) {
                problems.add(name + ": named in flatlaf-absent.txt, and FlatLaf has no such class");
            }
        }
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(problems.size()).append(" FlatLaf API shape problem(s):\n");
            for (String p : problems) {
                sb.append("  ").append(p).append('\n');
            }
            fail(sb.toString());
        }
    }
}
