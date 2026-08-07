/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.hardening;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Tier 2 keep rules, derived from the input classes with ASM. Codename One has no reflection --
 * {@code Class.forName} never resolved an obfuscated app class -- so there is nothing to keep for a
 * class named only by a string. What ProGuard cannot infer declaratively is the <em>naming
 * convention</em> that binds a native interface to its generated peer: for a native interface
 * {@code com.foo.Bar} the build produces {@code com.foo.BarImpl} / {@code com.foo.BarStub} and
 * resolves them by name. This scanner finds the native interfaces (phase 1) and keeps exactly those
 * peers (phase 2), which is far narrower than the previous {@code **Impl} / {@code **Stub}.
 */
public final class InputJarKeepScanner {

    private static final String NATIVE_INTERFACE = "com/codename1/system/NativeInterface";

    /** internal name -> its direct super-interfaces (from the class's interfaces[]). */
    private final java.util.Map<String, String[]> interfacesOf =
            new java.util.HashMap<String, String[]>();
    private final Set<String> nativeInterfaceTypes = new LinkedHashSet<String>();

    /** Scans every class in {@code classesByInternalName} (keyed {@code a/b/C}). */
    public void scan(Map<String, byte[]> classesByInternalName) {
        for (byte[] classBytes : classesByInternalName.values()) {
            ClassReader cr = new ClassReader(classBytes);
            cr.accept(new HierarchyCollector(), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
                    | ClassReader.SKIP_FRAMES);
        }
        // A type is a native interface if NativeInterface is in its transitive super-interface
        // closure. Resolve transitively across the classes we saw (an interface may extend another
        // native interface rather than NativeInterface directly).
        for (String type : interfacesOf.keySet()) {
            if (extendsNativeInterface(type, new LinkedHashSet<String>())) {
                nativeInterfaceTypes.add(type);
            }
        }
    }

    private boolean extendsNativeInterface(String type, Set<String> visiting) {
        if (!visiting.add(type)) {
            return false;
        }
        String[] ifaces = interfacesOf.get(type);
        if (ifaces == null) {
            return false;
        }
        for (String i : ifaces) {
            if (NATIVE_INTERFACE.equals(i) || extendsNativeInterface(i, visiting)) {
                return true;
            }
        }
        return false;
    }

    /** The derived keep rules: the generated {@code Impl}/{@code Stub} peer of each native interface. */
    public List<String> keepRules() {
        List<String> rules = new ArrayList<String>();
        for (String type : nativeInterfaceTypes) {
            String dotted = type.replace('/', '.');
            rules.add("-keep class " + dotted + "Impl { *; }");
            rules.add("-keep class " + dotted + "Stub { *; }");
        }
        return rules;
    }

    /** Visible for testing: the native interface types found in the input (dotted names). */
    List<String> nativeInterfaces() {
        List<String> out = new ArrayList<String>();
        for (String type : nativeInterfaceTypes) {
            out.add(type.replace('/', '.'));
        }
        return out;
    }

    private final class HierarchyCollector extends ClassVisitor {
        HierarchyCollector() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            interfacesOf.put(name, interfaces == null ? new String[0] : interfaces);
        }
    }
}
