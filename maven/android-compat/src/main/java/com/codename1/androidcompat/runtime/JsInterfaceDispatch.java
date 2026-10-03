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

/// Placeholder for the JavaScript interface dispatcher. The `remap-android`
/// build goal replaces this class in the application with one generated from
/// the compiled classes: for every public method annotated
/// `@JavascriptInterface` it emits a type test and a direct call, which is
/// how `WebView.addJavascriptInterface` reaches Java without reflection.
/// Without that step (for example in the runtime's own tests) an object
/// exposes no methods.
public final class JsInterfaceDispatch {

    private JsInterfaceDispatch() {
    }

    /// The names of the interface methods `target` has, each followed by a
    /// comma; a name may repeat when it is overloaded.
    public static String methods(Object target) {
        return "";
    }

    /// Calls the interface method `method` of `target` that takes
    /// `args.length` arguments, converting each from its JavaScript string
    /// form. Returns the result as JavaScript source (`undefined` for a void
    /// method), or null when `target` has no such method.
    public static String invoke(Object target, String method, String[] args) {
        return null;
    }
}
