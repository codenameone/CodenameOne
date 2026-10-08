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
///
/// It has the SHAPE of the generated class -- the same constructor and the
/// same two methods -- because the build's compliance check reads a class's
/// members from this artifact's jar, and so checks the generated class's
/// references to itself against what is declared here. What the generated
/// class needs beyond these members it puts in classes of its own
/// (`CompatRegistry$Part0`, ...), which only the application has.
public final class CompatRegistry implements ResourceBundle.Cn1Factory {

    /// The generated class passes an instance of itself to
    /// [ResourceBundle#cn1RegisterBundleClass(String, ResourceBundle.Cn1Factory, int)].
    public CompatRegistry() {
        // Nothing to set up: the class has no state.
    }

    /// Called once, by [CompatBoot#cn1Init()].
    public static void cn1Install() {
        // Deliberately empty: see the class description.
    }

    /// Creates the bundle class registered under `id`. The placeholder
    /// registers none.
    @Override
    public ResourceBundle cn1Create(int id) {
        return null;
    }
}
