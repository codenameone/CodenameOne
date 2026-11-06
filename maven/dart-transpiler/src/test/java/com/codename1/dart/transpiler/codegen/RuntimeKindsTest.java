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
package com.codename1.dart.transpiler.codegen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/// Holds JavaEmitter.RUNTIME_INTERFACES to the dart-runtime classes: the emitter extends a
/// dart.* runtime type unless it is listed there, and implements it when it is.
class RuntimeKindsTest {

    @Test
    void runtimeInterfacesMatchTheRuntime() throws Exception {
        URL marker = Class.forName("dart.core.DartList").getProtectionDomain().getCodeSource().getLocation();
        File root = new File(marker.toURI());
        java.util.List<String> names = new java.util.ArrayList<String>();
        if (root.isDirectory()) {
            for (File pkg : new File(root, "dart").listFiles()) {
                String[] files = pkg.list();
                for (int i = 0; files != null && i < files.length; i++) {
                    names.add("dart/" + pkg.getName() + "/" + files[i]);
                }
            }
        } else {
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(root)) {
                for (java.util.Enumeration<java.util.jar.JarEntry> en = jar.entries(); en.hasMoreElements();) {
                    names.add(en.nextElement().getName());
                }
            }
        }
        Set<String> interfaces = new TreeSet<String>();
        for (String n : names) {
            // top-level classes of dart.<package>, as the emitter names them
            if (!n.startsWith("dart/") || !n.endsWith(".class") || n.indexOf('$') >= 0
                    || n.split("/").length != 3) {
                continue;
            }
            Class<?> c = Class.forName(n.substring(0, n.length() - 6).replace('/', '.'),
                    false, getClass().getClassLoader());
            if (c.isInterface() && java.lang.reflect.Modifier.isPublic(c.getModifiers())) {
                interfaces.add(c.getName());
            }
        }
        assertTrue(interfaces.equals(new TreeSet<String>(JavaEmitter.RUNTIME_INTERFACES)),
                "runtime interfaces " + interfaces + " vs emitter " + new TreeSet<String>(JavaEmitter.RUNTIME_INTERFACES));
    }
}
