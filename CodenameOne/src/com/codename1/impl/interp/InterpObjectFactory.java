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

/// Produces the host-visible object for an interpreted class that extends or
/// implements something from the host app.
///
/// This is the hard half of the device runtime, and the half that differs most
/// between platforms. An interpreted `class MyForm extends Form` has to be an
/// object the framework accepts as a `Form` and whose overrides the framework
/// calls -- but the framework was compiled before the class existed, and
/// neither platform lets you define a class at run time:
///
/// - **iOS / ParparVM**: no `defineClass`, and iOS forbids writing executable
///   memory. What ParparVM does have is a heap-allocated, slot-indexed vtable
///   per class, so a subclass is built by copying the parent's `struct clazz`,
///   copying its vtable, and repointing the overridden slots at a trampoline
///   into the interpreter. Nothing is generated and nothing is written to
///   executable pages.
/// - **Android / ART**: no patchable vtable, and Play forbids loading dex at
///   run time, so the guard has to be compiled ahead of time -- a generated
///   subclass per extensible framework class, each overridable method either
///   delegating to the interpreter or calling `super`. Interfaces are easier:
///   `java.lang.reflect.Proxy` covers them with no generation at all.
///
/// Both satisfy this interface, so the interpreter never learns which is in
/// play.
///
/// @author Shai Almog
public interface InterpObjectFactory {
    /// Creates the host-visible peer for an interpreted object.
    ///
    /// #### Parameters
    ///
    /// - `object`: the interpreted instance the peer stands for
    /// - `hostSuperclassName`: JVM internal name of the nearest host
    ///   superclass, or null if the class only implements host interfaces
    /// - `hostInterfaceNames`: JVM internal names of the host interfaces the
    ///   class implements
    /// - `superConstructorDescriptor`: descriptor of the host superclass
    ///   constructor to run, or null for the no-arg one
    /// - `superConstructorArgs`: arguments for that constructor
    ///
    /// #### Returns
    ///
    /// an object the host will accept as an instance of `hostSuperclassName`
    /// and of every entry in `hostInterfaceNames`
    ///
    /// Supertypes arrive as internal names rather than as whatever the linker
    /// uses to represent a class, because the two platforms disagree about what
    /// a class even is: a `java.lang.Class` on Android, a numeric class id on
    /// iOS where `Class` carries no member information. A name is the one
    /// handle both can act on, and it is what the generated shim registry is
    /// keyed by.
    Object createPeer(InterpObject object,
                      String hostSuperclassName,
                      String[] hostInterfaceNames,
                      String superConstructorDescriptor,
                      Object[] superConstructorArgs) throws Throwable;

    /// The JVM internal name of a peer's own class.
    ///
    /// The factory knows this; `peer.getClass().getName()` does not, at least
    /// not everywhere. ParparVM derives `Class.getName()` from the mangled C
    /// symbol, where package separators and underscores are the same character,
    /// so `Interp_Form` comes back as `Interp/Form` and resolves against
    /// nothing. Asking the factory instead removes the guesswork.
    String peerClassName(Object peer);

    /// Whether this factory can produce a peer for the given host supertype,
    /// named as a JVM internal name or null for none.
    ///
    /// A platform answers false when the type is outside what it can extend --
    /// on Android, a class with no generated shim; on iOS, a final class. The
    /// runtime turns that into an error naming the type, which is far easier to
    /// act on than a peer that exists but is never dispatched to.
    boolean canExtend(String hostSuperclassName);
}
