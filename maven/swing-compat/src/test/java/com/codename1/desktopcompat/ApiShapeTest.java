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
package com.codename1.desktopcompat;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds every public class of the layer to the shape of the JDK class it
/// stands in for.
///
/// An application is compiled against the JDK's `java.awt` and `javax.swing`
/// and only then relocated onto this module, so a member that is spelled
/// differently here -- another parameter type, another return type, a method
/// the JDK never had -- is a link error on the device or an API nobody can
/// reach. For each public class under `com.codename1.desktopcompat.java` and
/// `.javax` the JDK class of the original name is loaded and compared:
/// every public or protected constructor, method and field must exist there
/// with the same descriptor once the package prefix is mapped back, the
/// superclass must map to an ancestor of the JDK class and every interface
/// to one the JDK class implements. Members named `cn1...` are the layer's
/// own hooks and are exempt.
///
/// The directory read is `target/classes`, or the `swingcompat.classes`
/// system property.
public class ApiShapeTest {

    private static final String PREFIX = "com.codename1.desktopcompat.";

    private final List<String> problems = new ArrayList<String>();
    private int checked;

    @Test
    public void everyPublicClassMatchesTheJdk() throws Exception {
        File root = new File(System.getProperty("swingcompat.classes", "target/classes"));
        File base = new File(root, "com/codename1/desktopcompat");
        walk(new File(base, "java"), "com.codename1.desktopcompat.java");
        walk(new File(base, "javax"), "com.codename1.desktopcompat.javax");
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(problems.size()).append(" API shape problem(s):\n");
            for (String p : problems) {
                sb.append("  ").append(p).append('\n');
            }
            fail(sb.toString());
        }
        assertTrue("no classes found under " + base, checked > 0);
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
                check(Class.forName(pkg + "." + simple, false, ApiShapeTest.class.getClassLoader()));
            }
        }
    }

    private static boolean api(int modifiers) {
        return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
    }

    /// Whether the class and every class it is nested in can be named by an
    /// application.
    private static boolean visible(Class<?> c) {
        for (Class<?> k = c; k != null; k = k.getDeclaringClass()) {
            if (!api(k.getModifiers())) {
                return false;
            }
        }
        return !c.isAnonymousClass() && !c.isLocalClass();
    }

    private void check(Class<?> ours) {
        if (!visible(ours)) {
            return;
        }
        checked++;
        String name = ours.getName();
        Class<?> real;
        try {
            real = Class.forName(name.substring(PREFIX.length()), false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            problems.add(name + ": the JDK has no such class");
            return;
        } catch (LinkageError e) {
            problems.add(name + ": the JDK class does not load: " + e);
            return;
        }
        if (ours.isInterface() != real.isInterface()) {
            problems.add(name + ": interface here but not in the JDK, or the reverse");
        }
        if (Modifier.isFinal(real.getModifiers()) != Modifier.isFinal(ours.getModifiers())) {
            problems.add(name + ": final differs from the JDK");
        }
        if (Modifier.isAbstract(ours.getModifiers()) && !Modifier.isAbstract(real.getModifiers())) {
            problems.add(name + ": abstract here but instantiable in the JDK");
        }
        Class<?> sup = ours.getSuperclass();
        if (sup != null && sup != Object.class) {
            Class<?> mapped = map(sup);
            if (mapped == null || !mapped.isAssignableFrom(real) || mapped.isInterface()) {
                problems.add(name + ": superclass " + sup.getName() + " is not an ancestor of the JDK class");
            }
        }
        for (Class<?> i : ours.getInterfaces()) {
            Class<?> mapped = map(i);
            if (mapped == null || !mapped.isAssignableFrom(real)) {
                problems.add(name + ": interface " + i.getName() + " is not implemented by the JDK class");
            }
        }
        for (Constructor<?> c : ours.getDeclaredConstructors()) {
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
                problems.add(name + ": constructor " + c + " is not in the JDK");
            }
        }
        for (Method m : ours.getDeclaredMethods()) {
            if (!api(m.getModifiers()) || m.isSynthetic() || m.isBridge() || m.getName().startsWith("cn1")) {
                continue;
            }
            Class<?>[] params = mapAll(m.getParameterTypes());
            Class<?> ret = map(m.getReturnType());
            Method found = params == null || ret == null ? null : findMethod(real, m.getName(), params, ret);
            if (found == null) {
                problems.add(name + ": method " + m + " is not in the JDK");
            } else if (Modifier.isStatic(found.getModifiers()) != Modifier.isStatic(m.getModifiers())) {
                problems.add(name + ": method " + m + " is static here but not in the JDK, or the reverse");
            } else if (Modifier.isAbstract(m.getModifiers()) && !Modifier.isAbstract(found.getModifiers())) {
                problems.add(name + ": method " + m + " is abstract here but concrete in the JDK");
            }
        }
        for (Field f : ours.getDeclaredFields()) {
            if (!api(f.getModifiers()) || f.isSynthetic() || f.getName().startsWith("cn1")) {
                continue;
            }
            Class<?> type = map(f.getType());
            Field found = type == null ? null : findField(real, f.getName(), type);
            if (found == null) {
                problems.add(name + ": field " + f + " is not in the JDK");
            } else if (Modifier.isStatic(found.getModifiers()) != Modifier.isStatic(f.getModifiers())) {
                problems.add(name + ": field " + f + " is static here but not in the JDK, or the reverse");
            }
        }
    }

    private static Method findMethod(Class<?> real, String name, Class<?>[] params, Class<?> ret) {
        if (real == null) {
            return null;
        }
        for (Method m : real.getDeclaredMethods()) {
            if (m.getName().equals(name) && api(m.getModifiers()) && m.getReturnType() == ret
                    && java.util.Arrays.equals(m.getParameterTypes(), params)) {
                return m;
            }
        }
        Method up = findMethod(real.getSuperclass(), name, params, ret);
        if (up != null) {
            return up;
        }
        for (Class<?> i : real.getInterfaces()) {
            up = findMethod(i, name, params, ret);
            if (up != null) {
                return up;
            }
        }
        return null;
    }

    private static Field findField(Class<?> real, String name, Class<?> type) {
        if (real == null) {
            return null;
        }
        for (Field f : real.getDeclaredFields()) {
            if (f.getName().equals(name) && api(f.getModifiers()) && f.getType() == type) {
                return f;
            }
        }
        Field up = findField(real.getSuperclass(), name, type);
        if (up != null) {
            return up;
        }
        for (Class<?> i : real.getInterfaces()) {
            up = findField(i, name, type);
            if (up != null) {
                return up;
            }
        }
        return null;
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

    /// The JDK type a type of ours stands for, or null when it is a layer
    /// type the JDK has no counterpart of.
    private static Class<?> map(Class<?> c) {
        if (c.isArray()) {
            Class<?> component = map(c.getComponentType());
            return component == null ? null : java.lang.reflect.Array.newInstance(component, 0).getClass();
        }
        String name = c.getName();
        if (!name.startsWith(PREFIX)) {
            return c;
        }
        try {
            return Class.forName(name.substring(PREFIX.length()), false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        } catch (LinkageError e) {
            return null;
        }
    }
}
