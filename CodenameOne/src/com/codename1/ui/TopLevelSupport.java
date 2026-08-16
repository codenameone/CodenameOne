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
package com.codename1.ui;

/// Package private helpers shared by `Form` and `Window`.
///
/// Java 5 has no default methods, so behaviour common to the two top levels lives
/// here as statics rather than on `TopLevelContainer`.
///
/// @author Shai Almog
final class TopLevelSupport {

    private TopLevelSupport() {
    }

    /// Resolves the top level containing the given component.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to resolve from, may be null
    ///
    /// #### Returns
    ///
    /// the enclosing top level, or null when the component is detached
    static TopLevelContainer of(Component cmp) {
        if (cmp == null) {
            return null;
        }
        return cmp.getTopLevelContainer();
    }

    /// Resolves the top level containing the given component and returns it as a
    /// `Container`, which is the form the package private top level hooks are
    /// declared in.
    ///
    /// #### Parameters
    ///
    /// - `cmp`: the component to resolve from, may be null
    ///
    /// #### Returns
    ///
    /// the enclosing top level as a container, or null when the component is detached
    static Container rootOf(Component cmp) {
        TopLevelContainer top = of(cmp);
        if (top == null) {
            return null;
        }
        return top.asContainer();
    }

    /// Throws when the running platform has no windowing system, so that misuse
    /// fails at the point of construction rather than at the first paint.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedOperationException`: if this platform cannot open native windows
    static void requireMultiWindow() {
        if (Display.impl == null || Display.impl.getWindowManager() == null) {
            throw new UnsupportedOperationException(
                    "Multiple native windows are not supported on this platform. "
                    + "Guard with Desktop.isSupported() or CN.isMultiWindowSupported().");
        }
    }
}
