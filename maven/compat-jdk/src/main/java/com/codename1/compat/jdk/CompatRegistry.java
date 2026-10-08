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
package com.codename1.compat.jdk;

/// What the build knows about one application's resources. THIS source is a
/// placeholder: the remap step never ships it. For every application it
/// generates a class of this exact name in its place, whose
/// [#cn1Install()] hands [Resources] the paths of the classpath resources
/// the application ships and [ResourceBundle] its bundles -- the
/// `.properties` files by path, and every `ListResourceBundle` subclass
/// through a factory that creates it with `new`.
///
/// The placeholder registers nothing, which is the right answer wherever no
/// build step ran: the tests of this module, and a compatibility layer's own
/// tests. [Resources] then reads a resource under the path it was asked for.
public final class CompatRegistry {

    private CompatRegistry() {
    }

    /// Called once, by [CompatBoot#cn1Init()].
    public static void cn1Install() {
        // Deliberately empty: see the class description.
    }
}
