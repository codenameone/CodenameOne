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

/// Runtime contract a build-time-generated component binder implements for one
/// `@Bindable` class.
///
/// Application code rarely references `Binder` directly -- it goes through
/// `Binders#bind`. The interface exists so generated code has a single
/// ServiceLoader-friendly shape and hand-written extensions can sit on the
/// same type.
public interface Binder<T> {

    /// The class this binder handles.
    Class<T> type();

    /// Pushes every `@Bind` field on `model` into the matching component in
    /// `container`. Components are located by name via a recursive scan that
    /// matches `Component#getName()` against `@Bind(name=...)`. Wires up
    /// two-way listeners on editable text fields and toggle buttons so
    /// subsequent user input updates the model.
    Binding bind(T model, Container container);
}
