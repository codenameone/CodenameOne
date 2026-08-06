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

import org.objectweb.asm.ClassWriter;

/**
 * A {@link ClassWriter} that resolves the class hierarchy from the application and
 * library jars instead of the engine's own classloader.
 *
 * <p>{@code COMPUTE_FRAMES} has to find the common superclass of two reference types
 * at a control-flow join, and ASM's default implementation does that by loading the
 * types through {@code getClassLoader()}. The engine runs as {@code java -jar
 * cn1-hardening.jar}, so the application classes and the supplied library jars are
 * not on that classloader; the default resolver would then fail with a missing-type
 * exception and abort hardening on any class with a merge between application types.
 * This writer is given a classloader built over the (renamed) input classes plus the
 * library jars, and falls back to {@code java/lang/Object} -- always a valid, if
 * imprecise, common superclass for the verifier -- when a type still can't be
 * resolved, so frame computation never crashes the build.
 */
public final class FrameClassWriter extends ClassWriter {

    private final ClassLoader hierarchy;

    public FrameClassWriter(int flags, ClassLoader hierarchy) {
        super(flags);
        this.hierarchy = hierarchy;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (hierarchy == null) {
            return safeDefault(type1, type2);
        }
        try {
            Class<?> c1 = Class.forName(type1.replace('/', '.'), false, hierarchy);
            Class<?> c2 = Class.forName(type2.replace('/', '.'), false, hierarchy);
            if (c1.isAssignableFrom(c2)) {
                return type1;
            }
            if (c2.isAssignableFrom(c1)) {
                return type2;
            }
            if (c1.isInterface() || c2.isInterface()) {
                return "java/lang/Object";
            }
            Class<?> c = c1;
            do {
                c = c.getSuperclass();
                if (c == null) {
                    return "java/lang/Object";
                }
            } while (!c.isAssignableFrom(c2));
            return c.getName().replace('.', '/');
        } catch (Throwable t) {
            // A type that can't be resolved (renamed, or absent from the supplied jars):
            // Object is always a safe common superclass for the verifier.
            return "java/lang/Object";
        }
    }

    private static String safeDefault(String type1, String type2) {
        return type1.equals(type2) ? type1 : "java/lang/Object";
    }
}
