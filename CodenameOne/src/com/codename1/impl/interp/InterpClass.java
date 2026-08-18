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

import java.util.Hashtable;
import java.util.Vector;

/// One interpreted class.
///
/// A class in a pushed bundle may extend another interpreted class or a class
/// that lives in the host app; the two cases are deliberately different.
/// Extending an interpreted class is bookkeeping -- method resolution walks
/// [#superInterp]. Extending a host class means an object the AOT framework has
/// to accept as an instance of that class, which the host cannot manufacture
/// from a name; that is what [InterpObjectFactory] exists for.
///
/// @author Shai Almog
public final class InterpClass {
    String name;
    int accessFlags;

    /// Superclass when it is itself interpreted; null when the superclass lives
    /// in the host app (or when this is java/lang/Object's stand-in).
    InterpClass superInterp;

    /// Extern index of the superclass when it lives in the host app, else -1.
    /// Read by [#collectHostSupertypes].
    int superExtern = -1;

    /// Extern indices of implemented interfaces that live in the host app.
    int[] hostInterfaces = new int[0];

    /// Interfaces that are themselves interpreted.
    InterpClass[] interpInterfaces = new InterpClass[0];

    InterpMethod[] methods = new InterpMethod[0];

    /// Instance field names declared here, in declaration order. Field storage
    /// is a flat Object[] per instance rather than typed slots: an interpreted
    /// class's fields are never read by AOT code, so nothing constrains their
    /// layout, and one representation avoids a per-field type switch on every
    /// getfield.
    String[] fieldNames = new String[0];
    String[] fieldDescs = new String[0];

    /// Stands in for a null static value.
    ///
    /// Static storage is a `Hashtable`, which is what the CLDC-era subset the
    /// core is written against provides -- and which rejects null values. A
    /// reference-typed static starts at null, so it needs a stand-in rather
    /// than an absent entry: absence is how "this class does not declare that
    /// field" is expressed, and conflating the two would send a lookup up the
    /// superclass chain to a shadowed field.
    static final Object NULL_STATIC = new Object();

    /// Static field storage, by field name. Access through [#staticValue] and
    /// [#setStaticValue] rather than directly, so the null stand-in stays an
    /// implementation detail.
    Hashtable statics = new Hashtable();

    /// The value of a static field declared by this class.
    Object staticValue(String fieldName) {
        Object v = statics.get(fieldName);
        return v == NULL_STATIC ? null : v;
    }

    /// Sets a static field declared by this class.
    void setStaticValue(String fieldName, Object value) {
        statics.put(fieldName, value == null ? NULL_STATIC : value);
    }

    /// Whether this class declares the named static field.
    boolean declaresStatic(String fieldName) {
        return statics.containsKey(fieldName);
    }

    /// Resolved (name+desc -> InterpMethod) including inherited interpreted
    /// methods. Built once, on first use.
    private Hashtable vtable;

    /// Offset of this class's own fields within an instance's flat field array;
    /// inherited interpreted fields come first.
    int fieldBase;

    /// Not yet initialized.
    static final int INIT_NONE = 0;

    /// Some thread is running the class initializer right now.
    static final int INIT_RUNNING = 1;

    /// The class initializer completed, or the class has none.
    static final int INIT_DONE = 2;

    /// The class initializer threw. The class is permanently unusable.
    static final int INIT_FAILED = 3;

    /// Where this class is in the four-state initialization sequence.
    ///
    /// A boolean cannot express it. "Running" has to be distinguishable from
    /// "done" or a second thread reads the static fields of a class whose
    /// `<clinit>` is halfway through, and "failed" has to be distinguishable
    /// from both or a class whose initializer threw is treated ever after as
    /// though it had succeeded. Guarded by this object's monitor.
    int initState;

    /// The thread running `<clinit>`, so its own re-entry is allowed through.
    ///
    /// `<clinit>` reaching back into its own class is legal and common -- a
    /// static field read from a static method called by the initializer -- so
    /// the initializing thread must not block on itself. Guarded with
    /// {@link #initState}.
    Thread initThread;

    String sourceFile;

    InterpClass(String name) {
        this.name = name;
    }

    /// The JVM internal name (a/b/C), or a descriptor for an array type.
    public String getName() {
        return name;
    }

    /// The element type when this token stands for an array type, else null.
    ///
    /// `Entry[].class` needs a token of its own: sharing the leaf's would make
    /// `Entry[].class == Entry.class` true, `getName()` answer `Entry`, and
    /// `isInstance` test the elements rather than the array.
    InterpClass arrayComponent;

    /// The token for an array of this type, created once and reused so
    /// `Entry[].class == Entry[].class` holds.
    synchronized InterpClass arrayType() {
        if (arrayToken == null) {
            InterpClass t = new InterpClass("[" + descriptorOf(this));
            t.arrayComponent = this;
            arrayToken = t;
        }
        return arrayToken;
    }

    private InterpClass arrayToken;

    /// Whether this token stands for an array type.
    public boolean isArray() {
        return arrayComponent != null;
    }

    private static String descriptorOf(InterpClass c) {
        return c.isArray() ? c.name : "L" + c.name + ";";
    }

    /// Whether this interpreted type is an interface. An interface has no
    /// instances of its own, so the object factory never has to produce a peer
    /// for one -- only for the classes that implement it.
    public boolean isInterface() {
        return (accessFlags & 0x0200) != 0;
    }

    /// The source file this class was compiled from, for stack traces and the
    /// on-device source viewer.
    public String getSourceFile() {
        return sourceFile;
    }

    /// Total number of instance fields including inherited interpreted ones.
    int totalFieldCount() {
        return fieldBase + fieldNames.length;
    }

    /// Finds a method declared directly on this class.
    InterpMethod declaredMethod(String methodName, String desc) {
        for (InterpMethod m : methods) {
            if (m.name.equals(methodName) && m.desc.equals(desc)) {
                return m;
            }
        }
        return null;
    }

    /// Resolves a method against this class and its interpreted supertypes.
    /// Returns null when nothing in the interpreted hierarchy declares it --
    /// which means the call has to reach the host app instead.
    ///
    /// Public because an [InterpObjectFactory] lives outside this package -- it
    /// is inherently platform-specific -- and a proxy handler has to ask
    /// whether the interpreted class actually provides the method it was just
    /// handed.
    public InterpMethod resolve(String methodName, String desc) {
        if (vtable == null) {
            buildVtable();
        }
        return (InterpMethod) vtable.get(methodName + desc);
    }

    /// The interfaces ordered so a supertype is copied before its subtype.
    ///
    /// A selection sort rather than an insertion sort: "extends" is a partial
    /// order, so an unrelated interface sitting between two related ones is not
    /// a barrier and an insertion sort that stops at the first non-swap leaves
    /// `C implements B, X, A` (with `B extends A`) in exactly the wrong order.
    /// This repeatedly takes an interface that nothing remaining is a supertype
    /// of, which is well defined however the interfaces are interleaved.
    private static InterpClass[] sortBySpecificity(InterpClass[] ifaces) {
        InterpClass[] remaining = new InterpClass[ifaces.length];
        System.arraycopy(ifaces, 0, remaining, 0, ifaces.length);
        InterpClass[] out = new InterpClass[ifaces.length];
        for (int written = 0; written < out.length; written++) {
            int pick = -1;
            for (int i = 0; i < remaining.length; i++) {
                if (remaining[i] == null) {
                    continue;
                }
                boolean anyBelow = false;
                for (int j = 0; j < remaining.length; j++) {
                    if (j != i && remaining[j] != null
                            && extendsInterface(remaining[j], remaining[i])) {
                        // remaining[i] is a supertype of something still here,
                        // so it has to be copied first.
                        anyBelow = true;
                        break;
                    }
                }
                if (anyBelow) {
                    pick = i;
                    break;
                }
                if (pick < 0) {
                    pick = i;
                }
            }
            if (pick < 0) {
                break;
            }
            out[written] = remaining[pick];
            remaining[pick] = null;
        }
        return out;
    }

    /// Whether `sub` reaches `parent` through its interpreted superinterfaces.
    private static boolean extendsInterface(InterpClass sub, InterpClass parent) {
        for (InterpClass up : sub.interpInterfaces) {
            if (up == parent || (up != null && extendsInterface(up, parent))) {  //NOPMD CompareObjectsWithEquals - one class object, not an equal one
                return true;
            }
        }
        return false;
    }

    private void buildVtable() {
        Hashtable t = new Hashtable();
        // Supertypes first so an override replaces the inherited entry.
        // Interfaces before the superclass: a default method is only reached
        // when no class in the chain provides an implementation, and the
        // superclass pass below overwrites anything it does declare.
        // Least specific first, so a sub-interface's override lands on top of
        // the one it overrides. `class C implements B, A` where `B extends A`
        // is legal and declaration order says nothing about which default wins
        // -- Java says the most specific one does.
        for (InterpClass iface : sortBySpecificity(interpInterfaces)) {
            copyInto(t, iface);
        }
        if (superInterp != null) {
            copyInto(t, superInterp);
        }
        for (InterpMethod m : methods) {
            if (!m.isStatic()) {
                t.put(m.name + m.desc, m);
            }
        }
        vtable = t;
    }

    private static void copyInto(Hashtable target, InterpClass source) {
        if (source.vtable == null) {
            source.buildVtable();
        }
        java.util.Enumeration e = source.vtable.keys();
        while (e.hasMoreElements()) {
            Object k = e.nextElement();
            target.put(k, source.vtable.get(k));
        }
    }

    /// Whether this interpreted class is, transitively, a subtype of the named
    /// interpreted type. Host supertypes are not considered here -- the linker
    /// answers those, since only it knows the host's type graph.
    boolean isSubclassOfInterp(String otherName) {
        InterpClass c = this;
        while (c != null) {
            if (c.name.equals(otherName)) {
                return true;
            }
            for (int i = 0; i < c.interpInterfaces.length; i++) {
                if (c.interpInterfaces[i].isSubclassOfInterp(otherName)) {
                    return true;
                }
            }
            c = c.superInterp;
        }
        return false;
    }

    /// Every host type this class must be assignable to: its nearest host
    /// superclass plus every host interface in the hierarchy. Used to decide
    /// what the object factory has to produce.
    void collectHostSupertypes(Vector externIndices) {
        InterpClass c = this;
        while (c != null) {
            if (c.superExtern >= 0) {
                Integer boxed = Integer.valueOf(c.superExtern);
                if (!externIndices.contains(boxed)) {
                    externIndices.addElement(boxed);
                }
            }
            for (int i = 0; i < c.hostInterfaces.length; i++) {
                Integer boxed = Integer.valueOf(c.hostInterfaces[i]);
                if (!externIndices.contains(boxed)) {
                    externIndices.addElement(boxed);
                }
            }
            for (int i = 0; i < c.interpInterfaces.length; i++) {
                c.interpInterfaces[i].collectHostSupertypes(externIndices);
            }
            c = c.superInterp;
        }
    }
}
