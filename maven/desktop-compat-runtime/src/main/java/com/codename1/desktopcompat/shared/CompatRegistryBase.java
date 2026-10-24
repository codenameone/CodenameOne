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
package com.codename1.desktopcompat.shared;

import java.util.HashMap;
import java.util.Map;

/// What the build knows about one application's classes: the superclass of
/// the registry it generates for every application built from desktop
/// sources, `com.codename1.generated.compat.CompatRegistry`.
///
/// A device creates no class from its name, so the desktop runtime's
/// `java.util.ResourceBundle` asks the installed registry instead. The
/// generated subclass records, while it installs:
///
/// - every resource the application ships, in [Resources]'s index;
/// - every resource bundle class of the application, under a number its
///   [#cn1Create(int)] turns into a `new` and its [#cn1Type(int)] into a
///   class constant.
///
/// It then makes itself the installed registry. The generated `cn1Install()`
/// does all of that once, however often it is called.
public abstract class CompatRegistryBase {

    private static CompatRegistryBase installed;

    private final Map<String, Integer> classes = new HashMap<String, Integer>();

    /// For the generated subclass.
    protected CompatRegistryBase() {
    }

    /// Whether a generated registry is installed already.
    public static boolean cn1Installed() {
        return installed != null;
    }

    /// Forgets the installed registry, so that the next `cn1Install()`
    /// installs again. For tests, which define several applications in one
    /// process.
    public static void cn1ClearInstalled() {
        installed = null;
    }

    /// A new instance of the application class `className`, as
    /// `Class.getName()` answers it, or null when the installed registry has
    /// no such class or none is installed.
    public static Object cn1NewInstance(String className) {
        CompatRegistryBase r = installed;
        return r == null ? null : r.newInstance(className);
    }

    /// The application class `className`, or null; see [#cn1NewInstance(String)].
    public static Class<?> cn1TypeOf(String className) {
        CompatRegistryBase r = installed;
        return r == null ? null : r.type(className);
    }

    /// Records that the application has the class `className`, as
    /// `Class.getName()` answers it, created and typed under `id`.
    public final void cn1Class(String className, int id) {
        classes.put(className, Integer.valueOf(id));
    }

    /// Makes this the installed registry.
    public final void cn1Activate() {
        installed = this;
    }

    /// A new instance of the class recorded under `id`, or null.
    protected abstract Object cn1Create(int id);

    /// The class recorded under `id`, or null.
    protected abstract Class<?> cn1Type(int id);

    private Object newInstance(String className) {
        Integer id = classes.get(className);
        return id == null ? null : cn1Create(id.intValue());
    }

    private Class<?> type(String className) {
        Integer id = classes.get(className);
        return id == null ? null : cn1Type(id.intValue());
    }
}
