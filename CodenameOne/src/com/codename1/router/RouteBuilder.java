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

/// Builds the `Form` for a matched route. Registered via `Router#route`.
///
/// Builders must be idempotent given the same `RouteContext` -- the Router may call
/// them more than once across a session (e.g., on warm restore). They run on the
/// EDT; long work should be kicked off in #build and rendered into a placeholder.
///
/// #### Since 8.0
public interface RouteBuilder {
    /// Builds the Form for this route.
    ///
    /// #### Parameters
    /// - `ctx`: per-navigation context (path params, query, extras, originating link).
    ///
    /// #### Returns
    /// The Form to show. Must not be null. The Router will call `Form.show()` or
    /// `Form.showBack()` itself; do not call them inside the builder.
    Form build(RouteContext ctx);
}
