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
package com.codename1.interp;

/// Descriptor parsing for [InterpLinker] implementations, which live outside
/// this package.
///
/// A linker is inherently platform-specific -- reflection on Android, invoke
/// thunks on iOS -- so it cannot live in the core, but every one of them has to
/// take a method descriptor apart in exactly the same way. Exposing the parser
/// keeps that one implementation rather than one per platform, each with its
/// own bugs around array and object descriptors.
///
/// @author Shai Almog
public final class InterpValuesAccess {
    private InterpValuesAccess() {
    }

    /// The parameter type descriptors of a method descriptor, in order.
    ///
    /// #### Parameters
    ///
    /// - `methodDescriptor`: a JVM method descriptor, e.g. `(ILjava/lang/String;)V`
    ///
    /// #### Returns
    ///
    /// one descriptor per parameter, e.g. `{"I", "Ljava/lang/String;"}`
    public static String[] argumentTypes(String methodDescriptor) {
        return InterpValues.argumentTypes(methodDescriptor);
    }

    /// The descriptor of a method's return type; `V` for void.
    public static String returnType(String methodDescriptor) {
        return methodDescriptor.substring(methodDescriptor.indexOf(')') + 1);
    }
}
