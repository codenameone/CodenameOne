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

/// `java.lang.Module`, as far as an application without modules has one: the
/// single unnamed module every class of it is in.
///
/// `Class.getModule()` is redirected to [#cn1Of(Class)] by the build. The
/// module has no name, no layer and no descriptor, which is what the JDK
/// answers for a class loaded from the class path, and what code that asks
/// in order to choose between the module path and the class path needs to
/// hear. Nothing else of the module system is here.
public final class Module {

    private static final Module UNNAMED = new Module();

    private Module() {
    }

    /// `cls.getModule()`: the unnamed module, for every class.
    public static Module cn1Of(Class<?> cls) {
        if (cls == null) {
            throw new NullPointerException();
        }
        return UNNAMED;
    }

    /// False: the one module there is has no name.
    public boolean isNamed() {
        return false;
    }

    /// Null, as for every unnamed module.
    public String getName() {
        return null;
    }

    /// Null: an unnamed module is in no layer.
    public ModuleLayer getLayer() {
        return null;
    }

    @Override
    public String toString() {
        return "unnamed module @" + Integer.toHexString(hashCode());
    }
}
