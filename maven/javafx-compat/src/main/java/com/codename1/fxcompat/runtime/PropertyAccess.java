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
package com.codename1.fxcompat.runtime;

/// What `javafx.scene.control.cell.PropertyValueFactory` finds by name with
/// reflection on a desktop, as one method the build implements with direct
/// calls: the public accessor of a bean, called by its name.
///
/// #### Who implements it
///
/// The build does, once per application, in the class [PropertyRegistry]:
/// after the application is compiled and relocated it reads the public
/// classes out of the class files and writes one call per public
/// `xxxProperty()`, `getXxx()` and `isXxx()` method that takes no argument.
/// It does so for a class that has at least one `xxxProperty()` method,
/// and for a class with an accessor for a name the application hands to a
/// `PropertyValueFactory` constructor as a constant.
///
/// This class is what the generated one falls back to, and here every
/// answer is [#NONE] -- which is also what a run without the build step
/// gets, a unit test of this module among them. Such a run installs an
/// implementation of its own with [#install(PropertyAccess)].
public abstract class PropertyAccess {

    /// The answer for a method the bean's class has no generated call for.
    /// It is not `null`, because a getter may return `null`.
    public static final Object NONE = new Object();

    private static PropertyAccess current;

    /// Creates an accessor that knows no bean.
    protected PropertyAccess() {
        // Every answer is NONE until a subclass overrides call.
    }

    /// The installed accessor: the application's generated one unless
    /// [#install(PropertyAccess)] replaced it.
    public static PropertyAccess current() {
        if (current == null) {
            current = new PropertyRegistry();
        }
        return current;
    }

    /// Installs an accessor; `null` returns to the generated one.
    public static void install(PropertyAccess access) {
        current = access;
    }

    /// Calls the public method `method` of `bean`, which takes no
    /// argument, and answers what it returned -- a primitive in its
    /// wrapper -- or [#NONE] when no call was generated for it.
    public Object call(Object bean, String method) {
        return NONE;
    }

    /// Whether any call was generated for the class of `bean`, which
    /// tells a misspelt property from a class the build did not look at.
    public boolean knows(Object bean) {
        return false;
    }
}
