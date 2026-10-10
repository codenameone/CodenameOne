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
package javafx.beans.binding;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

/// A value computed from other observable values.
///
/// A binding is valid once its value has been computed and turns invalid
/// when a dependency changes; it tells its listeners so once and recomputes
/// only when the value is next read.
public interface Binding<T> extends ObservableValue<T> {

    /// Returns whether the cached value is current.
    boolean isValid();

    /// Marks the value stale, so the next read computes it again.
    void invalidate();

    /// Returns what this binding depends on, as an unmodifiable list.
    ObservableList<?> getDependencies();

    /// Tells the binding it is no longer used, so it can stop observing its
    /// dependencies. Bindings observe weakly, so calling this is optional.
    void dispose();
}
