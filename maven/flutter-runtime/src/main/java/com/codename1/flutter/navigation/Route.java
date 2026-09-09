/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.navigation;

/**
 * Base type of a navigable route — Flutter's {@code Route<T>}. Minimal marker
 * added so typed route factories (a demo function returning {@code
 * Route<String>}) accept the concrete Cupertino route subclasses. The
 * navigation category may later flesh this out; the Cupertino routes only
 * rely on it as a common supertype.
 *
 * @param <T> the value type the route completes with when popped
 */
public abstract class Route<T> {

    private RouteSettings settings;

    /**
     * Flutter's {@code Route.settings}. Accepts an {@code Object} because super-parameter
     * forwarding erases the argument type to {@code dynamic}; only a {@link RouteSettings} is
     * retained.
     */
    public void settings(Object v) {
        this.settings = (v instanceof RouteSettings) ? (RouteSettings) v : null;
    }

    public RouteSettings settings() {
        return settings;
    }
}
