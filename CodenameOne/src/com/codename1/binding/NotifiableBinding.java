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

/// Internal contract every generated `Binding` implementation provides so
/// `Binders#notifyChanged(Object)` can route a model mutation to the
/// bindings that observe it. Application code never references this
/// directly -- the `Binding` interface remains the public handle returned
/// from `Binders.bind`.
public interface NotifiableBinding extends Binding {

    /// `model.getClass().getName()` -- the registry key
    /// `Binders.notifyChanged` uses to find the bindings that care about
    /// this model class. Stored at bind time so it survives obfuscation:
    /// the value is whatever `getName()` returned at bind, which is
    /// guaranteed to match `notifyChanged`'s lookup within a single
    /// execution.
    String modelTypeName();

    /// True when this binding's source object IS `model` (identity, not
    /// equality). The notification fan-out uses identity so multiple
    /// independent instances of the same `@Bindable` class don't refresh
    /// each other.
    boolean matches(Object model);
}
