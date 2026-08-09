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

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
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
 * library jars.
 *
 * <p>Resolution has two stages. First it tries to LOAD the two types (precise when
 * both, and their supertypes, are available). If loading throws -- typically because
 * a shared supertype is supplied only by the target platform and is absent from the
 * supplied jars -- it resolves the hierarchy from the class BYTES instead: reading the
 * {@code super_class} name of each type does not require loading the (absent) supertype,
 * so a real common supertype such as {@code Base} is still found. Only when the bytes
 * cannot be read either does it fall back to {@code java/lang/Object}. Collapsing to
 * {@code Object} too eagerly is not merely imprecise: if the merged value is then used
 * where {@code Base} is expected, {@code Object} is not assignable to {@code Base} and
 * the generated {@code StackMapTable} fails verification, rejecting a valid application.
 */
public final class FrameClassWriter extends ClassWriter {

    private final ClassLoader hierarchy;

    public FrameClassWriter(int flags, ClassLoader hierarchy) {
        super(flags);
        this.hierarchy = hierarchy;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        if (hierarchy == null) {
            return commonSuperFromBytes(type1, type2);
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
            // A type (or a supertype of it) can't be LOADED -- e.g. a shared Base supplied only by the
            // target platform. Resolve from the class bytes so a real common supertype is still found
            // rather than collapsing to Object, which would fail StackMapTable verification.
            return commonSuperFromBytes(type1, type2);
        }
    }

    /**
     * Finds a common supertype by reading {@code super_class} names from the class bytes, which does not
     * require the (possibly absent) supertypes to be loadable. Walks {@code type1}'s ancestor chain into
     * a set, then walks {@code type2}'s chain until it meets one of them. Falls back to
     * {@code java/lang/Object} only when the bytes cannot be read.
     */
    private String commonSuperFromBytes(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        Set<String> ancestors1 = ancestorsFromBytes(type1);
        String t = type2;
        Set<String> seen = new LinkedHashSet<String>();
        while (t != null && seen.add(t)) {
            if (ancestors1.contains(t)) {
                return t;
            }
            t = superNameFromBytes(t);
        }
        return "java/lang/Object";
    }

    /** {@code type} and every super_class above it that can be read from bytes, plus Object as the root. */
    private Set<String> ancestorsFromBytes(String type) {
        Set<String> set = new LinkedHashSet<String>();
        String t = type;
        while (t != null && set.add(t)) {
            t = superNameFromBytes(t);
        }
        set.add("java/lang/Object");
        return set;
    }

    /** The {@code super_class} name from {@code type}'s bytes, or null when Object or unreadable/absent. */
    private String superNameFromBytes(String type) {
        if (type == null || "java/lang/Object".equals(type) || hierarchy == null) {
            return null;
        }
        InputStream in = hierarchy.getResourceAsStream(type + ".class");
        if (in == null) {
            return null;
        }
        try {
            return new ClassReader(in).getSuperName();
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                in.close();
            } catch (Throwable ignore) {
                // best effort
            }
        }
    }
}
