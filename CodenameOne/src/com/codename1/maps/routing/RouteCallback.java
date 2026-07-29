/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.routing;

import java.util.List;

/// Receives the outcome of a routing request. Exactly one method is invoked
/// per request, always on the Codename One event dispatch thread, so it is
/// safe to touch the UI directly from either one.
public interface RouteCallback {

    /// Delivers the routes found, best first. The list holds at least one
    /// [Route] and only holds more when [RouteRequest#setAlternatives] asked
    /// for alternatives and the backend found some.
    void routesFound(List routes);

    /// Reports that no route could be produced -- the network failed, the
    /// service rejected the request, or no road connects the points (routing
    /// across an ocean by car, for example).
    ///
    /// #### Parameters
    ///
    /// - `message`: a human readable explanation, never `null`
    ///
    /// - `error`: the underlying exception when one was thrown, or `null` when
    ///   the failure carried none -- a request rejected before it was sent, or
    ///   an error status the service answered with. `message` stands on its own
    ///   either way; `error` is there for logging and diagnostics.
    void routeFailed(String message, Throwable error);
}
