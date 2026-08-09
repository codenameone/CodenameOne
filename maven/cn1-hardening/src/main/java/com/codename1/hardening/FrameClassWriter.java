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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

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
    private boolean hierarchyIncomplete;

    public FrameClassWriter(int flags, ClassLoader hierarchy) {
        super(flags);
        this.hierarchy = hierarchy;
    }

    /**
     * True when a frame merge had to collapse to {@code java/lang/Object} because the type hierarchy was
     * INCOMPLETE -- a supertype on the path to the real common base could not be read (absent from the
     * supplied jars), not because the two types genuinely share only {@code Object}. In that case the
     * recomputed {@code Object} stack-map type may be weaker than the type the target expects (e.g. a
     * shared {@code Base} reachable only through two missing intermediates), so the caller must ship the
     * class UNHARDENED -- with its original, javac-computed frames -- rather than a possibly-invalid one.
     */
    public boolean isHierarchyIncomplete() {
        return hierarchyIncomplete;
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
     * Finds a common supertype from the class bytes, which does not require the (possibly absent)
     * supertypes to be loadable. Mirrors the load-based logic exactly, using byte-based assignability
     * that walks BOTH {@code super_class} and {@code interfaces[]}: if one type is a supertype (a
     * superclass OR an implemented/extended interface) of the other it is returned; a merge involving an
     * unrelated interface is {@code java/lang/Object} (the verifier treats an interface as Object); two
     * unrelated classes resolve to the first shared {@code super_class}. Falls back to
     * {@code java/lang/Object} only when the bytes cannot be read.
     */
    private String commonSuperFromBytes(String type1, String type2) {
        // These are always non-array reference types (internal names like "pkg/A"), never array
        // descriptors like "[Lpkg/A;", so readerFor()'s "<name>.class" lookup is correct. That is ASM's
        // contract for getCommonSuperClass, verified empirically against ASM 9.8 (the shaded version):
        // for a merge of same-dimension reference arrays A[] and B[], ASM strips the array dimension and
        // hands us the ELEMENT names "pkg/A" and "pkg/B", then re-wraps OUR result back into "[Lpkg/Base;"
        // itself -- so a shared but unloadable Base still resolves to Base[] via the scalar path below.
        // A merge of array-vs-scalar, or of arrays of different dimension, is resolved to Object by ASM
        // internally and getCommonSuperClass is never called at all. So an array-descriptor branch here
        // would be unreachable dead code; the scalar resolution is the whole contract.
        if (type1.equals(type2)) {
            return type1;
        }
        if (isAssignableFromBytes(type1, type2)) {
            return type1;
        }
        if (isAssignableFromBytes(type2, type1)) {
            return type2;
        }
        if (isInterfaceFromBytes(type1) || isInterfaceFromBytes(type2)) {
            return "java/lang/Object";
        }
        String c = superNameFromBytes(type1);
        Set<String> seen = new LinkedHashSet<String>();
        while (c != null && seen.add(c)) {
            if (isAssignableFromBytes(c, type2)) {
                return c;
            }
            c = superNameFromBytes(c);
        }
        // Falling through to Object. That is only SOUND when both chains are fully readable up to Object
        // and genuinely share no closer ancestor. If either chain is broken by a missing intermediate, the
        // real common base (reachable only through the absent class) is invisible here, so Object is a
        // guess that may be too weak -- flag it so the caller ships the class unhardened with its original
        // frames instead of emitting a possibly-invalid recomputed one.
        if (!chainReachesObject(type1) || !chainReachesObject(type2)) {
            hierarchyIncomplete = true;
        }
        return "java/lang/Object";
    }

    /**
     * True when {@code type}'s superclass chain can be walked entirely to {@code java/lang/Object} (or a
     * readable root) through readable bytes. False when a link in the chain is absent from the supplied
     * jars, which means the byte-based resolver cannot see a shared base that lies beyond that gap.
     */
    private boolean chainReachesObject(String type) {
        String c = type;
        Set<String> seen = new HashSet<String>();
        while (c != null && seen.add(c)) {
            if ("java/lang/Object".equals(c)) {
                return true;
            }
            ClassReader cr = readerFor(c);
            if (cr == null) {
                return false;
            }
            c = cr.getSuperName();
        }
        return true;
    }

    /** True when {@code sub} is {@code sup}, or extends/implements it (transitively) per the class bytes. */
    private boolean isAssignableFromBytes(String sup, String sub) {
        if (sup.equals(sub) || "java/lang/Object".equals(sup)) {
            return true;
        }
        Set<String> visited = new HashSet<String>();
        Deque<String> stack = new ArrayDeque<String>();
        stack.push(sub);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (!visited.add(cur)) {
                continue;
            }
            if (cur.equals(sup)) {
                return true;
            }
            ClassReader cr = readerFor(cur);
            if (cr == null) {
                continue;
            }
            if (cr.getSuperName() != null) {
                stack.push(cr.getSuperName());
            }
            String[] interfaces = cr.getInterfaces();
            if (interfaces != null) {
                for (int i = 0; i < interfaces.length; i++) {
                    stack.push(interfaces[i]);
                }
            }
        }
        return false;
    }

    private boolean isInterfaceFromBytes(String type) {
        ClassReader cr = readerFor(type);
        return cr != null && (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    }

    /** The {@code super_class} name from {@code type}'s bytes, or null when Object or unreadable/absent. */
    private String superNameFromBytes(String type) {
        if (type == null || "java/lang/Object".equals(type)) {
            return null;
        }
        ClassReader cr = readerFor(type);
        return cr == null ? null : cr.getSuperName();
    }

    /** A {@link ClassReader} over {@code type}'s bytes from the hierarchy loader, or null if unreadable. */
    private ClassReader readerFor(String type) {
        if (type == null || hierarchy == null) {
            return null;
        }
        InputStream in = hierarchy.getResourceAsStream(type + ".class");
        if (in == null) {
            return null;
        }
        try {
            return new ClassReader(in);
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
