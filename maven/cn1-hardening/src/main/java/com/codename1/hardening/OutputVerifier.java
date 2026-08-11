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
import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Verifies every class the engine is about to ship. A transform bug that produces
 * invalid bytecode must fail the build here, not at first launch on a device.
 * Each class gets a structural check ({@code CheckClassAdapter}, no type loading)
 * plus per-method data-flow analysis ({@code SimpleVerifier}); the data-flow pass
 * runs method-by-method so a method that references a target-only type absent from
 * the supplied jars can be tolerated without masking a genuine error elsewhere in
 * the same class.
 */
public final class OutputVerifier {

    private OutputVerifier() {
    }

    /**
     * @param hierarchy a classloader over the (renamed) input classes plus the library jars, so the
     *                  verifier's {@code SimpleVerifier} resolves application types instead of
     *                  loading them from the engine's own classpath (which would fail verification
     *                  on any class with a merge between application types). May be {@code null}.
     * @throws HardeningException on the first class that fails verification, naming it.
     */
    public static void verify(Map<String, byte[]> classesByInternalName, ClassLoader hierarchy)
            throws HardeningException {
        for (Map.Entry<String, byte[]> e : classesByInternalName.entrySet()) {
            // Structural checks first (visit order, access flags, names/descriptors); these load no types,
            // so a structurally-invalid transform output is always caught.
            verifyStructureOnly(e.getKey(), e.getValue());
            // Then data-flow, PER METHOD, rather than through CheckClassAdapter.verify's whole-class report.
            // A method that fails only because an absent target-only type cannot be resolved is tolerated,
            // but WITHOUT masking a genuine data-flow error in another method of the same class: ASM can
            // append both diagnostics to one report, where a substring check would misclassify the whole
            // report as an unresolved-type failure and discard the real error.
            verifyDataFlow(e.getKey(), e.getValue(), hierarchy);
        }
    }

    /**
     * Data-flow verification, one method at a time, so the missing-type tolerance is scoped to the method
     * that actually references the absent type. A method whose analysis fails only because a type is absent
     * from the supplied jars is accepted (FrameClassWriter computed its frames from the bytes and the
     * target JVM verifies it on-device); any other analyzer failure fails the build, naming the method.
     */
    private static void verifyDataFlow(String name, byte[] classBytes, ClassLoader hierarchy)
            throws HardeningException {
        ClassNode cn = new ClassNode();
        new ClassReader(classBytes).accept(cn, 0);
        Type currentClass = Type.getObjectType(cn.name);
        Type superClass = cn.superName == null ? null : Type.getObjectType(cn.superName);
        List<Type> interfaces = new ArrayList<Type>();
        if (cn.interfaces != null) {
            for (String i : cn.interfaces) {
                interfaces.add(Type.getObjectType(i));
            }
        }
        boolean isInterface = (cn.access & Opcodes.ACC_INTERFACE) != 0;
        if (cn.methods == null) {
            return;
        }
        for (MethodNode m : cn.methods) {
            if (m.instructions == null || m.instructions.size() == 0) {
                continue;   // abstract or native: no body to analyze
            }
            SimpleVerifier verifier = new SimpleVerifier(currentClass, superClass, interfaces, isInterface);
            verifier.setClassLoader(hierarchy);
            try {
                new Analyzer<BasicValue>(verifier).analyze(cn.name, m);
            } catch (Throwable t) {
                if (isUnresolvedTypeFailure(t)) {
                    continue;
                }
                throw new HardeningException("Hardened class '" + name + "' method '" + m.name + m.desc
                        + "' failed bytecode verification: " + t.getMessage(), t);
            }
        }
    }

    /**
     * Structural verification only (no data-flow, so no type loading): checks the class-file structure --
     * visit order, valid access flags, names and descriptors. Used as the fallback when the data-flow
     * verifier cannot resolve an absent type.
     */
    private static void verifyStructureOnly(String name, byte[] classBytes) throws HardeningException {
        try {
            new ClassReader(classBytes).accept(new CheckClassAdapter(new ClassWriter(0), false), 0);
        } catch (Throwable t) {
            throw new HardeningException("Hardened class '" + name
                    + "' failed structural bytecode verification: " + t.getMessage(), t);
        }
    }

    /** True when a verification failure is only a missing type (absent from the supplied jars), not a defect. */
    private static boolean isUnresolvedTypeFailure(Throwable t) {
        Throwable c = t;
        for (int guard = 0; c != null && guard < 32; guard++) {
            if (c instanceof TypeNotPresentException || c instanceof ClassNotFoundException
                    || c instanceof NoClassDefFoundError) {
                return true;
            }
            if (c == c.getCause()) {
                break;
            }
            c = c.getCause();
        }
        return false;
    }
}
