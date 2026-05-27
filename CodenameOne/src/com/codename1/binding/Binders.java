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
package com.codename1.binding;

import com.codename1.ui.Container;

import java.util.HashMap;
import java.util.Map;

/// Public entry point for the build-time component binding framework.
///
/// `@Bindable` classes are picked up by the Maven plugin's annotation
/// processor at build time. The generated `Binder` is wired into this
/// registry through a generated `com.codename1.binding.generated.BindersIndex`
/// whose no-arg constructor fires the first time the registry is touched.
/// (Projects with no `@Bindable` classes fall through to an empty no-op
/// stub shipped with cn1-core, so the lookup degrades cleanly.)
///
/// ```java
/// Form f = (Form) Resources.getGlobalResources().getForm("LoginForm");
/// LoginModel model = new LoginModel();
/// Binding b = Binders.bind(model, f);
/// // user types -- model is updated; mutate the model and call b.refresh().
/// ```
public final class Binders {

    private static final Map<Class<?>, Binder<?>> BY_TYPE = new HashMap<Class<?>, Binder<?>>();
    /// Doubles as the "index loaded" sentinel and as the place we pin the
    /// instantiated `BindersIndex` against SpotBugs' no-side-effect check:
    /// the cn1-core stub has an empty constructor, so without an
    /// assignment SpotBugs flags `new BindersIndex()` as a no-op. Reading
    /// the field as the gate keeps SpotBugs from flipping that to
    /// `URF_UNREAD_FIELD` instead.
    private static volatile Object indexInstance;

    private Binders() { }

    /// Installs a binder for `binder.type()`. Generated binders call this
    /// from the `BindersIndex` static initializer; hand-written binders for
    /// classes outside the build's annotation scan call it explicitly.
    public static <T> void register(Binder<T> binder) {
        if (binder == null) {
            throw new IllegalArgumentException("binder is null");
        }
        BY_TYPE.put(binder.type(), binder);
    }

    /// Looks up the binder for `type` or null when no binder is registered.
    @SuppressWarnings("unchecked")
    public static <T> Binder<T> get(Class<T> type) {
        ensureIndexLoaded();
        return (Binder<T>) BY_TYPE.get(type);
    }

    /// Pushes the model values into the matching components in `container`,
    /// wiring up the two-way listeners declared on its `@Bind` fields.
    /// Throws `IllegalStateException` when no binder is registered for
    /// `model.getClass()`.
    @SuppressWarnings("unchecked")
    public static <T> Binding bind(T model, Container container) {
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }
        if (container == null) {
            throw new IllegalArgumentException("container is null");
        }
        Binder<T> binder = (Binder<T>) get((Class<T>) model.getClass());
        if (binder == null) {
            throw new IllegalStateException("No binder registered for "
                    + model.getClass().getName() + ". Add @Bindable and "
                    + "ensure the cn1:process-annotations Mojo ran during build.");
        }
        return binder.bind(model, container);
    }

    private static synchronized void ensureIndexLoaded() {
        if (indexInstance != null) return;
        try {
            indexInstance = new com.codename1.binding.generated.BindersIndex();
        } catch (NoClassDefFoundError e) {
            // No @Bindable types in this project. Pin a sentinel so we
            // don't retry the lookup on every Binders.get call.
            indexInstance = Boolean.FALSE;
        } catch (RuntimeException e) {
            indexInstance = Boolean.FALSE;
        }
    }
}
