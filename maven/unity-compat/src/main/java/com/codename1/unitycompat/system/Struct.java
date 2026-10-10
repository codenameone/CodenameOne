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
package com.codename1.unitycompat.system;

/// What every struct is on the JVM, besides an object: something that can be
/// copied and reset to its default value by code that does not know its
/// class. The translator adds this to each struct it writes; the structs of
/// the base library declare it themselves.
///
/// It exists for the runtime's own code. `Array.Clone` and `Array.Clear`
/// receive an array of any element type, there is no reflection to ask what
/// the elements are, and `Object.clone()` answers null on ParparVM.
@SuppressWarnings("PMD.MethodNamingConventions") // $-names are the protocol translated code calls
public interface Struct {
    /// A copy that shares nothing with this value.
    Object $copyValue();

    /// Resets every field to its default.
    void $clear();
}
