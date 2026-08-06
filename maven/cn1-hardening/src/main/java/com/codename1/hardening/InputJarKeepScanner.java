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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Tier 2 keep rules, derived from the input classes with ASM. This covers what
 * ProGuard cannot infer declaratively: a class named by a string constant that is
 * then resolved by reflection ({@code Class.forName}, {@code UIBuilder}, the
 * annotation-generated mappers). Over-keeping here is safe -- it costs a little
 * obfuscation coverage; under-keeping would break the app at runtime -- so any app
 * class whose name appears verbatim as a string constant anywhere in the jar is
 * kept.
 */
public final class InputJarKeepScanner {

    private final Set<String> classBinaryNames = new LinkedHashSet<String>();
    private final Set<String> stringConstants = new LinkedHashSet<String>();

    /** Scans every class in {@code classesByInternalName} (keyed {@code a/b/C}). */
    public void scan(Map<String, byte[]> classesByInternalName) {
        for (Map.Entry<String, byte[]> e : classesByInternalName.entrySet()) {
            classBinaryNames.add(e.getKey().replace('/', '.'));
        }
        for (byte[] classBytes : classesByInternalName.values()) {
            ClassReader cr = new ClassReader(classBytes);
            cr.accept(new ConstantCollector(), ClassReader.SKIP_FRAMES);
        }
    }

    /** The derived keep rules. */
    public List<String> keepRules() {
        List<String> rules = new ArrayList<String>();
        Set<String> kept = new LinkedHashSet<String>();
        for (String s : stringConstants) {
            String candidate = s.trim();
            // Accept both dotted and slash forms of a reference.
            String dotted = candidate.replace('/', '.');
            if (classBinaryNames.contains(dotted) && kept.add(dotted)) {
                rules.add("-keep class " + dotted + " { *; }");
            }
        }
        return rules;
    }

    /** Visible for testing: the class names that were kept for reflection safety. */
    List<String> reflectivelyReferencedClasses() {
        List<String> out = new ArrayList<String>();
        Set<String> seen = new LinkedHashSet<String>();
        for (String s : stringConstants) {
            String dotted = s.trim().replace('/', '.');
            if (classBinaryNames.contains(dotted) && seen.add(dotted)) {
                out.add(dotted);
            }
        }
        return out;
    }

    private final class ConstantCollector extends ClassVisitor {
        ConstantCollector() {
            super(Opcodes.ASM9);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof String) {
                        stringConstants.add((String) value);
                    }
                }
            };
        }

        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                         String signature, Object value) {
            // A reflective class name may live only in a static-final String field's ConstantValue
            // attribute, never as an LDC (e.g. read by an external framework). Collect those too.
            if (value instanceof String) {
                stringConstants.add((String) value);
            }
            return null;
        }
    }
}
