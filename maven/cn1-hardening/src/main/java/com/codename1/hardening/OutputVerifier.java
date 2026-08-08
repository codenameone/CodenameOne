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
}
