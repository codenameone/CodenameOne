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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds every constant of the layer to the value of the same constant in
/// the JDK.
///
/// A compiler inlines a `public static final` primitive or `String` into the
/// application that reads it, so a constant here that differs from the JDK
/// does not take effect in code compiled against the JDK: the application
/// keeps the JDK's number. For each public class under
/// `com.codename1.desktopcompat.java` and `.javax` every such field is
/// compared with the field of the same name of the JDK class. Classes the
/// JDK lacks are left to `ApiShapeTest`, and fields named `cn1...` are the
/// layer's own.
///
/// The directory read is `target/classes`, or the `swingcompat.classes`
/// system property.
public class ConstantParityTest {

    private static final String PREFIX = "com.codename1.desktopcompat.";

    private final List<String> problems = new ArrayList<String>();

    private int compared;

    @Test
    public void constantsHaveTheValuesOfTheJdk() throws Exception {
        File root = new File(System.getProperty("swingcompat.classes", "target/classes"));
        File base = new File(root, "com/codename1/desktopcompat");
        walk(new File(base, "java"), "com.codename1.desktopcompat.java");
        walk(new File(base, "javax"), "com.codename1.desktopcompat.javax");
        if (!problems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(problems.size()).append(" constant(s) differ from the JDK:\n");
            for (String p : problems) {
                sb.append("  ").append(p).append('\n');
            }
            fail(sb.toString());
        }
        assertTrue("no constants compared under " + base, compared > 0);
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
                check(Class.forName(pkg + "." + simple, false, ConstantParityTest.class.getClassLoader()));
            }
        }
    }

    private void check(Class<?> ours) throws Exception {
        if (!Modifier.isPublic(ours.getModifiers())) {
            return;
        }
        Class<?> real;
        try {
            real = Class.forName(ours.getName().substring(PREFIX.length()), false,
                    ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            return;
        } catch (LinkageError e) {
            return;
        }
        for (Field f : ours.getDeclaredFields()) {
            int m = f.getModifiers();
            if (!Modifier.isPublic(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)
                    || f.getName().startsWith("cn1")) {
                continue;
            }
            Class<?> type = f.getType();
            if (!type.isPrimitive() && type != String.class) {
                continue;
            }
            Field other;
            try {
                other = real.getField(f.getName());
            } catch (NoSuchFieldException e) {
                continue;
            }
            if (!Modifier.isStatic(other.getModifiers()) || other.getType() != type) {
                continue;
            }
            compared++;
            Object mine = f.get(null);
            Object theirs = other.get(null);
            if (!mine.equals(theirs)) {
                problems.add(ours.getName() + "." + f.getName() + " is " + mine + " but the JDK has " + theirs);
            }
        }
    }
}
