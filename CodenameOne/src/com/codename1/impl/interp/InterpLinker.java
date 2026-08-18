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
package com.codename1.impl.interp;

/// How interpreted code reaches the app it was pushed into.
///
/// Every platform answers this differently, and the difference is the whole
/// reason the interface exists:
///
/// - **Android and the JavaSE simulator** have real reflection, so the backend
///   is `java.lang.reflect` and there is nothing to generate.
/// - **iOS / ParparVM** has none -- `Method.invoke` does not exist and
///   `struct clazz` carries no name-to-method table. What it does have, under
///   the interp-host build, is a per-method invoke thunk registered by method
///   id plus a symbol table mapping JVM name and descriptor to that id. The
///   iOS backend binds through those.
///
/// The interpreter above this interface never learns which one it is talking
/// to.
///
/// @author Shai Almog
public interface InterpLinker {
    /// Resolves a class by JVM internal name (a/b/C), or null if the host does
    /// not have it. Returning null rather than throwing matters: a pushed
    /// program may legitimately reference a class the installed app was built
    /// without, and the interpreter turns that into a diagnosable error at the
    /// point of use rather than at load.
    Object findClass(String internalName);

    /// Runs a host class's static initializer, if the platform has a way to ask
    /// for that and has not run it already.
    ///
    /// [#findClass(String)] deliberately does not: resolution happens for all
    /// sorts of reasons -- a cast, an `instanceof`, a symbol lookup -- and
    /// initializing on every one of them would run initializers Java does not.
    /// Initializing an interpreted class, on the other hand, has to initialize
    /// its host superclass first, or the parent's static state is built after
    /// the child's rather than before it.
    void initializeClass(String internalName) throws Throwable;

    /// Whether a host interface declares a default method.
    ///
    /// Only those are initialized on an implementor's behalf (JLS 12.4.1), and
    /// the bundle does not record it -- the interface belongs to the app, so
    /// the platform is the only thing that can answer. False is the safe answer
    /// for a platform that cannot tell: an interface initializes on its own
    /// first use either way, and the only thing at stake is the order.
    boolean declaresDefaultMethod(String internalName) throws Throwable;

    /// Constructs a host object.
    Object construct(Object hostClass, String descriptor, Object[] args) throws Throwable;

    /// Invokes a host instance method virtually -- dispatch follows the
    /// receiver's real type, not the named owner.
    Object invokeVirtual(Object target, String owner, String name, String descriptor,
                         Object[] args) throws Throwable;

    /// Invokes a host method without virtual dispatch, for `invokespecial`
    /// (a `super.` call or a private method).
    Object invokeSpecial(Object target, String owner, String name, String descriptor,
                         Object[] args) throws Throwable;

    /// Invokes a host static method.
    Object invokeStatic(String owner, String name, String descriptor, Object[] args)
            throws Throwable;

    /// Reads a host static field.
    Object getStatic(String owner, String name, String descriptor) throws Throwable;

    /// Writes a host static field.
    void setStatic(String owner, String name, String descriptor, Object value) throws Throwable;

    /// Reads a host instance field.
    Object getField(Object target, String owner, String name, String descriptor) throws Throwable;

    /// Writes a host instance field.
    void setField(Object target, String owner, String name, String descriptor, Object value)
            throws Throwable;

    /// Whether a value is an instance of a host type. Used by `instanceof` and
    /// `checkcast`, and by exception-table matching.
    boolean isInstance(Object hostClass, Object value);

    /// Allocates an array of a host component type.
    Object newArray(String componentDescriptor, int length) throws Throwable;

    /// Allocates a multi-dimensional array.
    Object newMultiArray(String arrayDescriptor, int[] dimensions) throws Throwable;

    /// An empty array of the same runtime type and length as `source`, or null
    /// when the platform cannot say what that type is.
    ///
    /// `clone()` on a `String[]` has to produce a `String[]`. The interpreter
    /// represents its own reference arrays as `Object[]`, but an array that came
    /// from the host carries a real component type, and a copy that lost it
    /// fails the moment it is passed back.
    Object cloneArray(Object source) throws Throwable;

    /// The `java.lang.Class` object for a host class, for `ldc` of a class
    /// literal.
    Object classObject(Object hostClass);
}
