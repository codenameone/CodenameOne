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
package com.codename1.androidcompat.runtime;

/// Placeholder for the view model factory. The `remap-android` build goal
/// replaces this class in the application with one generated from the
/// compiled classes: for every public, concrete `ViewModel` it compares the
/// class name and calls `new` with the constructor the class declares --
/// `(Application, SavedStateHandle)`, `(SavedStateHandle)`,
/// `(Application)` or `()`, in that order of preference -- which is what
/// `ViewModelProvider` uses instead of reflection. Without that step (the
/// runtime's own tests) the runtime's view models are still created here.
public final class ViewModelFactory {

    private ViewModelFactory() {
    }

    /// The view model `className` names, or null. `application` and
    /// `savedState` are passed to constructors that take them; a null
    /// `savedState` means the caller's factory does not provide one.
    public static Object create(String className, Object application, Object savedState) {
        return null;
    }
}
