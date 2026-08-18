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

/// An instance of an interpreted class.
///
/// Fields are a flat `Object[]`, indexed by the declaring class's
/// [InterpClass#fieldBase] plus the field's position. Nothing in the host reads
/// these, so there is no layout to match and no reason to keep typed slots.
///
/// When the interpreted class extends a host class, `hostPeer` holds the object
/// the host actually sees -- produced by [InterpObjectFactory]. The two are
/// distinct because the host's object has the host's layout and the host's
/// vtable, while interpreted state has to live somewhere the host does not know
/// about.
///
/// @author Shai Almog
public final class InterpObject {
    final InterpClass type;
    final Object[] fields;

    /// The host-visible object when this interpreted class extends or
    /// implements something from the host app; null for a class whose whole
    /// hierarchy is interpreted (other than java.lang.Object).
    Object hostPeer;

    /// JVM internal name of the peer's own class.
    ///
    /// Recorded when the factory builds the peer rather than read back from
    /// `getClass()`: ParparVM derives `Class.getName()` from the mangled C
    /// symbol, so a class whose simple name contains an underscore -- which
    /// every generated shim's does, `Interp_Form` -- comes back as
    /// `Interp/Form` and matches no symbol at all.
    String hostPeerOwner;

    /// Name and position of an interpreted enum constant, or null and -1.
    ///
    /// An enum constant has no peer. `java.lang.Enum` cannot be subclassed from
    /// Java source, so no shim for it can exist, and it needs none: everything
    /// `Enum` does is two final fields and the handful of methods that read
    /// them, which the interpreter implements directly. What the host sees of
    /// an interpreted enum is whatever interfaces it declares.
    String enumName;
    int enumOrdinal = -1;

    /// The runtime that created this object, so that host code converting it
    /// to a string reaches the interpreted `toString`.
    InterpRuntime runtime;

    InterpObject(InterpClass type) {
        this.type = type;
        this.fields = new Object[type.totalFieldCount()];
        initFields(type);
    }

    private void initFields(InterpClass c) {
        if (c == null) {
            return;
        }
        initFields(c.superInterp);
        for (int i = 0; i < c.fieldNames.length; i++) {
            fields[c.fieldBase + i] = InterpValues.defaultValue(c.fieldDescs[i]);
        }
    }

    /// The interpreted class of this object.
    public InterpClass getType() {
        return type;
    }

    /// The host-visible peer, or null.
    public Object getHostPeer() {
        return hostPeer;
    }

    int indexOf(InterpClass declaring, String fieldName) {
        for (int i = 0; i < declaring.fieldNames.length; i++) {
            if (declaring.fieldNames[i].equals(fieldName)) {
                return declaring.fieldBase + i;
            }
        }
        // Walk up: javac names the declaring class of an inherited field as the
        // static type at the access site, which may be a subclass.
        InterpClass c = declaring.superInterp;
        while (c != null) {
            for (int i = 0; i < c.fieldNames.length; i++) {
                if (c.fieldNames[i].equals(fieldName)) {
                    return c.fieldBase + i;
                }
            }
            c = c.superInterp;
        }
        return -1;
    }

    /// The interpreted class's own `toString`, when it has one.
    ///
    /// An interpreted object with no host peer is handed to the framework as
    /// itself, so anything that converts it to a string -- `StringBuilder`,
    /// `System.out.println`, a `Label` -- lands here rather than on any
    /// interpreted method. Without this an enum constant prints as
    /// `Color@interp` instead of `RED`, and so does every class that defines a
    /// perfectly good `toString`.
    ///
    /// An object that *does* have a peer never reaches this: the framework sees
    /// the peer, whose generated override routes to the interpreter already.
    @Override
    public String toString() {
        // The override first, the constant name second. An enum is allowed to
        // define toString -- `RED` printing as `red` is the ordinary reason to
        // write one -- and answering the name here made the override apply to
        // interpreted callers and not to host ones, so the same constant
        // printed two different ways depending on who asked.
        if (runtime != null) {
            Object r = runtime.dispatch(this, "toString", "()Ljava/lang/String;",
                    new Object[0]);
            if (!isMiss(r)) {
                return (String) r;
            }
        }
        if (enumName != null) {
            // What java.lang.Enum.toString does, for a constant that did not
            // override it.
            return enumName;
        }
        return type.name.replace('/', '.') + "@interp";
    }

    /// Delegates to an interpreted `equals`, for the same reason `toString`
    /// does.
    ///
    /// A peerless object reaches host code as itself, and host code puts it in
    /// a `HashMap`. Leaving equality as identity there does not merely lose a
    /// nicety: two keys the program considers equal hash differently and every
    /// lookup misses, quietly.
    @Override
    public boolean equals(Object other) {
        if (runtime != null) {
            Object r = runtime.dispatch(this, "equals", "(Ljava/lang/Object;)Z",
                    new Object[] {other});
            if (!isMiss(r)) {
                return ((Boolean) r).booleanValue();
            }
        }
        return other == this;  //NOPMD CompareObjectsWithEquals - Object.equals is identity
    }

    /// Delegates to an interpreted `hashCode`. Overriding `equals` without this
    /// is the classic way to break every hash-based collection, and here the
    /// collection belongs to the host.
    @Override
    public int hashCode() {
        if (runtime != null) {
            Object r = runtime.dispatch(this, "hashCode", "()I", new Object[0]);
            if (!isMiss(r)) {
                return ((Integer) r).intValue();
            }
        }
        return System.identityHashCode(this);
    }

    /// Whether the interpreter answered with a sentinel rather than a value.
    ///
    /// Two of them, and both mean "use the default behaviour here":
    /// NOT_OVERRIDDEN because the pushed class does not declare the method,
    /// DETACHED because the program that did has been stopped. Host code can
    /// still hold a peerless object -- a key in a collection, something waiting
    /// to be logged -- and casting the sentinel would turn printing it into a
    /// ClassCastException.
    private static boolean isMiss(Object answer) {
        //NOPMD CompareObjectsWithEquals - sentinels, not equal objects
        return answer == InterpRuntime.NOT_OVERRIDDEN || answer == InterpRuntime.DETACHED;
    }
}
