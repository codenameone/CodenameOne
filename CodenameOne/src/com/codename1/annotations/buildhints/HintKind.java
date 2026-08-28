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

package com.codename1.annotations.buildhints;

/// What a `String` build hint really holds.
///
/// The attribute's Java type says whether a hint is a flag, a number, a list or
/// one of a closed set. It cannot say which KIND of string a string is, and the
/// Settings editor picks its control from exactly that: a version gets a version
/// field, a secret gets a masked one, an XML fragment gets a multi-line box.
///
/// Named rather than spelled out in a string member, so a typo is a compile
/// error instead of a hint that quietly renders as a plain text field.
public enum HintKind {

    /// Whatever the attribute's Java type says on its own.
    DEFAULT,

    /// Prose or code long enough to want more than one line.
    TEXT_BLOCK,

    /// An XML fragment spliced into a manifest or a project file.
    XML,

    /// A filesystem path.
    PATH,

    /// A URL.
    URL,

    /// A version number.
    VERSION,

    /// A credential, which the editor must not show in the clear.
    SECRET
}
