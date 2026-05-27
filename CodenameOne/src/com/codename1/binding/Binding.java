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

/// Handle returned by `Binders.bind`. Lets the caller refresh the components
/// from the model (e.g. after the model was mutated outside the form),
/// re-read the model from the components (e.g. before a save), or tear down
/// the listeners installed for two-way bindings.
public interface Binding {

    /// Pushes the current model values into every bound component.
    void refresh();

    /// Pulls current component values back into the model. Useful before
    /// validating / submitting a form when none of the bindings is two-way.
    void commit();

    /// Removes every listener the binder added so the form can be garbage-
    /// collected without keeping the model alive.
    void disconnect();
}
