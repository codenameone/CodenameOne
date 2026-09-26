/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.InheritedValueProvider;
import com.codename1.flutter.provider.SingleChildWidget;

import java.util.List;

/**
 * Publishes the localized-resource objects loaded from a MaterialApp's
 * {@code localizationsDelegates} to its subtree, keyed by runtime type. This is
 * how {@code GalleryLocalizations.of(context)} (which resolves through
 * {@link com.codename1.flutter.widgets.Localizations#of}) finds its instance:
 * {@code providedValueFor} returns the first loaded object assignable to the
 * requested type.
 */
public class LocalizationsScope extends SingleChildWidget implements InheritedValueProvider {

    private List<Object> resources;
    private final dart.runtime.Funcs.Func0<List<Object>> supplier;

    public LocalizationsScope(List<Object> resources) {
        this.resources = resources;
        this.supplier = null;
    }

    /**
     * A scope whose resources load on first lookup. Deferring matters: loading
     * during the app's build reads the delegate list at the earliest possible
     * moment, which on a lazily-initialised backend can be before the class
     * holding it has run its static initialiser.
     */
    public LocalizationsScope(dart.runtime.Funcs.Func0<List<Object>> supplier) {
        this.supplier = supplier;
    }

    private List<Object> resources() {
        if (resources == null && supplier != null) {
            resources = supplier.call();
            if (resources != null && resources.isEmpty()) {
                try {
                    com.codename1.io.Log.p("Flutter runtime: no localizations resolved for this app; "
                            + "every Foo.of(context) below will be null");
                } catch (Throwable ignore) {
                    // headless: Log has no storage backend
                }
            }
        }
        return resources;
    }

    @Override
    public Object providedValueFor(Class<?> type) {
        List<Object> rs = resources();
        if (rs != null && type != null) {
            for (Object r : rs) {
                if (com.codename1.flutter.Element.isInstanceOf(type, r)) {
                    return r;
                }
            }
        }
        return null;
    }
}
