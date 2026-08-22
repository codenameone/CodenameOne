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
package com.codename1.impl.ios;

/**
 * The native half of the iOS device runtime: everything the interpreter needs
 * that only C can provide.
 *
 * <p>These are ParparVM native methods rather than a {@code NativeInterface}
 * because they must take and return {@code Object}, which the native-interface
 * convention does not allow.</p>
 *
 * <p>The argument-passing convention is deliberately flat. Primitives travel as
 * raw {@code long} bits in one array and references in another, selected per
 * argument by a kind code, so the C side never has to box or unbox -- it would
 * have to call generated accessors to do that, which is exactly the reflection
 * ParparVM does not have. The same convention carries the result back:
 * primitives through {@code resultOut[0]}, references through the return
 * value.</p>
 *
 * @author Shai Almog
 */
class InterpIOSNative {
    private InterpIOSNative() {
    }

    /**
     * The translator's symbol table as text, or an empty string when this build
     * has none (i.e. was not built with {@code ios.interpHost=true}).
     */
    static native String symbolTable();

    /**
     * Calls a method by its symbol-table id.
     *
     * @param methodId   id from the symbol table
     * @param target     receiver, or null for a static method or a constructor
     * @param prims      raw bits for primitive arguments, by position
     * @param objs       references for object arguments, by position
     * @param kinds      per-argument kind, matching InterpOpcodes' RET_* codes
     * @param argCount   number of arguments
     * @param returnKind kind of the return value
     * @param resultOut  receives the raw bits of a primitive result
     * @return the reference result, or null
     */
    static native Object invokeById(int methodId, Object target, long[] prims, Object[] objs,
                                    int[] kinds, int argCount, int returnKind, long[] resultOut);

    /** Reads an instance field by its symbol-table id. */
    static native Object getFieldById(int fieldId, Object target, int kind, long[] resultOut);

    /** Writes an instance field by its symbol-table id. */
    static native void setFieldById(int fieldId, Object target, int kind, long rawValue,
                                    Object refValue);

    /**
     * Reads a static field by its symbol-table id.
     *
     * <p>Separate from {@link #getFieldById} because a static is reached
     * differently: there is no receiver and no offset, only a generated
     * accessor registered under the id. Reading through it also runs the
     * declaring class's static initializer, as a compiled GETSTATIC would.</p>
     */
    static native Object getStaticById(int fieldId, int kind, long[] resultOut);

    /** Writes a static field by its symbol-table id. */
    static native void setStaticById(int fieldId, int kind, long rawValue, Object refValue);

    /** Whether {@code value} is an instance of the class with this id. */
    static native boolean isInstanceOfId(int classId, Object value);

    /** The {@code java.lang.Class} for a class id, or null. */
    static native Object classObjectById(int classId);

    /**
     * Runs a class's static initializer, if it has not run already.
     *
     * <p>The generated initializer is idempotent, so this is safe to call on a
     * class that is already initialized -- and necessary for one that declares
     * no static field, which has no accessor to reach it through.</p>
     */
    static native void initializeClassById(int classId);

    /// The class id of an object's actual class, or -1 for null.
    ///
    /// Needed for virtual dispatch: the call site names the type the code was
    /// compiled against, and only the receiver knows which override to run.
    static native int classIdOf(Object value);

    /**
     * Allocates a reference array of the class named by {@code arrayClassId}.
     *
     * <p>The id is the *array* class -- {@code [Ljava/lang/String;} -- not its
     * component, because that is what the allocation needs and what a checkcast
     * against `String[]` compares.</p>
     */
    static native Object newObjectArray(int arrayClassId, int length);

    /**
     * Allocates an empty array shaped exactly like {@code source} -- the same
     * class, rank and element size -- or null when it cannot be read.
     *
     * <p>This is what makes {@code clone()} keep a host array's type. The
     * component of an existing array is written into the object itself, so
     * copying it needs no registry entry and works for ranks and component
     * types no table anticipated.</p>
     */
    static native Object newArrayLike(Object source, int length);

    /** True when the running binary carries invoke thunks. */
    static native boolean isInterpHostBuild();
}
