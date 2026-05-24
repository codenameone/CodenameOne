/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

import com.codename1.ui.Form;

/// Intercept back/pop attempts on a `Form`. Install with `Form#setPopGuard(PopGuard)`.
///
/// Modeled after Flutter's `PopScope`. Typical use is to confirm before leaving a
/// half-filled form, or to override hardware back to show a custom dialog.
///
/// #### Example
///
/// ```java
/// editForm.setPopGuard(new PopGuard() {
///     public boolean canPop(Form form, PopReason reason) {
///         if (!isDirty()) return true;
///         Dialog.show("Discard changes?", "You have unsaved edits.", "Stay", "Discard");
///         return false; // block the pop; we'll dismiss explicitly if user picks Discard.
///     }
/// });
/// ```
///
/// #### Since 8.0
public interface PopGuard {
    /// Decides whether a back/pop attempt should proceed.
    ///
    /// #### Parameters
    /// - `form`: the form being popped.
    /// - `reason`: what triggered the pop (back button, programmatic, etc.).
    ///
    /// #### Returns
    /// `true` to let the navigation proceed, `false` to block it. When blocking,
    /// the guard is responsible for any UI follow-up such as showing a confirm
    /// dialog and re-issuing the pop programmatically once confirmed.
    boolean canPop(Form form, PopReason reason);
}
