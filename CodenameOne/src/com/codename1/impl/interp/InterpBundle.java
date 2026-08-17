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

/// A program pushed to the device runtime: the interpreted classes, the symbols
/// they reference in the host app, and the source the user wrote.
///
/// The bundle is produced on the developer's machine (see the translator's
/// `InterpBundleWriter`) and consumed by [InterpRuntime]. Nothing here parses a
/// class file: the constant pool is already resolved into flat tables and jump
/// targets are already instruction indices, so the device only walks arrays.
///
/// The IR deliberately keeps the JVM's own opcodes rather than inventing a new
/// instruction set. Every semantic question -- what `dup2` does to a category-2
/// value, when `athrow` unwinds, how `invokespecial` differs from
/// `invokevirtual` -- then has one authoritative answer instead of a
/// reinterpretation that has to be rediscovered by testing. What the IR removes
/// is only the parts a device should not pay for: constant-pool lookups, symbol
/// resolution and label arithmetic.
///
/// @author Shai Almog
public final class InterpBundle {
    /// Magic word at the head of every bundle: 'C','N','1','I'.
    public static final int MAGIC = 0x434E3149;

    /// Bundle format version. The runtime refuses anything it does not know,
    /// because a bundle is pushed from a machine whose SDK moves independently
    /// of the installed app.
    public static final int VERSION = 2;

    /// Extern kinds -- what a reference into the host app names.
    public static final int EXTERN_CLASS = 0;
    public static final int EXTERN_METHOD = 1;
    public static final int EXTERN_FIELD = 2;

    String[] strings;

    /// Parallel arrays over the extern table. For a method or field, owner/name/
    /// desc are string-pool indices; for a class only owner is meaningful.
    int[] externKind;
    int[] externOwner;
    int[] externName;
    int[] externDesc;

    /// Resolved lazily by the linker, so a program that never touches a symbol
    /// never pays to resolve it -- and never fails because of it.
    Object[] externResolved;
    boolean[] externResolveAttempted;

    InterpClass[] classes;

    /// Interpreted class name -> InterpClass, in JVM internal form (a/b/C).
    Hashtable classesByName = new Hashtable();

    /// Source file name -> source text. Mandatory: the runtime refuses a bundle
    /// whose interpreted classes are not all covered, because the App Store's
    /// educational-code allowance is conditional on the user being able to see
    /// and edit what runs (guideline 2.5.2).
    Hashtable sources = new Hashtable();

    /// The program's own resources -- theme.res, CSS, images -- keyed by the
    /// path an application loads them by. Handed to the implementation layer
    /// when the bundle is loaded, so `Resources.openLayered("/theme")` finds
    /// the pushed program's theme rather than the runtime host's.
    Hashtable resources = new Hashtable();

    String mainClass;

    InterpBundle() {
    }

    /// The string pool entry at the given index.
    public String string(int index) {
        return strings[index];
    }

    /// The interpreted class with this JVM internal name, or null if the class
    /// is not part of this bundle (i.e. it belongs to the host app).
    public InterpClass findClass(String internalName) {
        return (InterpClass)classesByName.get(internalName);
    }

    /// The interpreted classes in this bundle.
    public InterpClass[] getClasses() {
        return classes;
    }

    /// The class whose main method the runtime should enter, or null when the
    /// bundle is a library rather than a program.
    public String getMainClass() {
        return mainClass;
    }

    /// The source text for a file name, or null. Used by the on-device viewer.
    public String getSource(String fileName) {
        return (String)sources.get(fileName);
    }

    /// The names of every source file carried by this bundle.
    public java.util.Enumeration getSourceFileNames() {
        return sources.keys();
    }

    /// The resources this bundle carries, keyed by load path.
    public Hashtable getResources() {
        return resources;
    }
}
