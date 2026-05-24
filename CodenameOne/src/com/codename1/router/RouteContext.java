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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// Per-navigation context handed to `RouteBuilder`, `RouteGuard`, and listeners.
///
/// Exposes:
/// - the matched path-parameter values (e.g. `:id` from `/users/:id`)
/// - the query string parameters
/// - the originating `DeepLink`
/// - an arbitrary key/value bag of `extras` so guards can stash data for downstream
///   builders without resorting to globals.
///
/// Instances are mutable only via the `extras` bag; pattern and query maps are
/// unmodifiable. Treat the object itself as a single-navigation scratchpad — it
/// is not retained across navigations.
///
/// #### Since 8.0
public final class RouteContext {
    private final DeepLink link;
    private final Map<String, String> params;
    private final Map<String, String> query;
    private final Map<String, Object> extras = new HashMap<String, Object>();
    private final String matchedPattern;

    RouteContext(DeepLink link, Map<String, String> params, String matchedPattern) {
        this.link = link;
        this.params = (params == null) ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(params);
        this.query = link.getQueryParameters();
        this.matchedPattern = matchedPattern;
    }

    /// The deep link that triggered this navigation. Never null.
    public DeepLink getLink() { return link; }

    /// The route pattern that matched, e.g. `/users/:id`. Null when no route was
    /// matched (the not-found path).
    public String getMatchedPattern() { return matchedPattern; }

    /// Returns a named path parameter, or null if absent.
    /// For pattern `/users/:id` and path `/users/42`, `param("id")` returns `"42"`.
    public String param(String name) { return params.get(name); }

    /// All path parameters as an unmodifiable map.
    public Map<String, String> params() { return params; }

    /// Returns a query parameter, or null. Equivalent to `getLink().getQueryParameter(name)`.
    public String query(String name) { return query.get(name); }

    /// Stores a value in the per-navigation extras bag. Useful for guards passing
    /// resolved data to builders.
    public RouteContext put(String key, Object value) {
        extras.put(key, value);
        return this;
    }

    /// Reads a value from the per-navigation extras bag.
    public Object get(String key) { return extras.get(key); }
}
