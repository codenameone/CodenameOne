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

/// How a sandboxed macOS application may reach files the user chooses.
///
/// `com.apple.security.files.user-selected.read-write` and its read-only
/// sibling are two different entitlements, and neither is a yes/no: an
/// application can ask for write access, for read access, or for nothing at
/// all. A boolean cannot say which of the two it wants, which is why this is an
/// enum -- expressing it as one would have made ON mean "some access, builder's
/// choice" and OFF silently drop the read-write access that is the default.
public enum FileAccess {

    /// Say nothing, and let the build server apply its own default.
    @HintUnset
    DEFAULT,

    /// Read and write the files the user opened. The build's default.
    @HintValue("readwrite")
    READ_WRITE,

    /// Read the files the user opened, without writing them back.
    @HintValue("readonly")
    READ_ONLY,

    /// No user-selected file access at all.
    @HintValue("none")
    NONE;
}
