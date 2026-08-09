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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

/**
 * Verifies every class the engine is about to ship. A transform bug that produces
 * invalid bytecode must fail the build here, not at first launch on a device: the
 * same {@code CheckClassAdapter} data-flow verification the framework already uses
 * elsewhere is run over each output class.
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
            StringWriter sw = new StringWriter();
            try {
                CheckClassAdapter.verify(new ClassReader(e.getValue()), hierarchy, false, new PrintWriter(sw));
            } catch (Throwable t) {
                if (isUnresolvedTypeFailure(t)) {
                    // ASM's data-flow SimpleVerifier LOADS types to resolve the hierarchy and threw
                    // because one is absent from the supplied jars -- typically an application class whose
                    // superclass is supplied only by the target platform. That is not a bytecode defect:
                    // FrameClassWriter already computed this class's frames from the bytes, and the target
                    // JVM verifies it on-device. Fall back to structural verification here, which needs no
                    // hierarchy, so a transform that emitted structurally invalid bytecode is still caught.
                    verifyStructureOnly(e.getKey(), e.getValue());
                    continue;
                }
                throw new HardeningException("Hardened class '" + e.getKey()
                        + "' failed bytecode verification: " + t.getMessage(), t);
            }
            String report = sw.toString();
            if (report.length() > 0) {
                throw new HardeningException("Hardened class '" + e.getKey()
                        + "' failed bytecode verification:\n" + report);
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
