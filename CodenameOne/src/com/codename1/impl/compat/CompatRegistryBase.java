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
package com.codename1.impl.compat;

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What the build knows about one application's classes: the superclass of
/// the registry it generates for every application built from desktop or
/// Android sources, `com.codename1.generated.compat.CompatRegistry`.
///
/// A device creates no class from its name and reads no `META-INF/services`
/// directory, so the class library's `java.util.ResourceBundle` and
/// `java.util.ServiceLoader` ask the installed [JdkHost.Registry] instead.
/// The generated subclass records, while it installs:
///
/// - every resource the application ships, in [Resources]'s index;
/// - every class the application may need created by name -- its resource
///   bundle classes and its service providers -- under a number its
///   [#cn1Create(int)] turns into a `new` and its [#cn1Type(int)] into a
///   class constant;
/// - the providers of every service, in the order `ServiceLoader` offers
///   them.
///
/// It then makes itself the registry `JdkHost` serves. The generated
/// `cn1Install()` does all of that once, however often it is called.
public abstract class CompatRegistryBase implements JdkHost.Registry {

    private final Map<String, Integer> classes = new HashMap<String, Integer>();
    private final Map<String, List<String>> providers = new HashMap<String, List<String>>();

    /// For the generated subclass.
    protected CompatRegistryBase() {
    }

    /// Whether a generated registry is installed already.
    public static boolean cn1Installed() {
        return JdkHost.getRegistry() instanceof CompatRegistryBase;
    }

    /// Records that the application has the class `className`, as
    /// `Class.getName()` answers it, created and typed under `id`.
    public final void cn1Class(String className, int id) {
        classes.put(className, Integer.valueOf(id));
    }

    /// Records that `className` provides `service`, both as
    /// `Class.getName()` answers them. A provider recorded twice for one
    /// service is kept once, where it was first recorded.
    public final void cn1Provider(String service, String className) {
        List<String> list = providers.get(service);
        if (list == null) {
            list = new ArrayList<String>();
            providers.put(service, list);
        }
        if (!list.contains(className)) {
            list.add(className);
        }
    }

    /// Makes this the registry `JdkHost` serves.
    public final void cn1Activate() {
        JdkHost.setRegistry(this);
    }

    /// A new instance of the class recorded under `id`, or null.
    protected abstract Object cn1Create(int id);

    /// The class recorded under `id`, or null.
    protected abstract Class<?> cn1Type(int id);

    @Override
    public final String[] providers(String service) {
        List<String> list = providers.get(service);
        if (list == null) {
            return null;
        }
        return list.toArray(new String[list.size()]);
    }

    @Override
    public final Object newInstance(String className) {
        Integer id = classes.get(className);
        return id == null ? null : cn1Create(id.intValue());
    }

    @Override
    public final Class<?> type(String className) {
        Integer id = classes.get(className);
        return id == null ? null : cn1Type(id.intValue());
    }
}
