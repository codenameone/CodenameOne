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
 * The data with which a route was pushed ({@code RouteSettings} in Flutter): its
 * name and optional arguments. new_gallery's {@code onGenerateRoute} matches on
 * {@link #name()} to build the right page.
 */
public class RouteSettings {

    private String name;
    private Object arguments;

    public RouteSettings() {
    }

    // Named-parameter setters.
    public void name(String v) {
        this.name = v;
    }

    public void arguments(Object v) {
        this.arguments = v;
    }

    public String name() {
        return name;
    }

    public Object arguments() {
        return arguments;
    }

    /** Returns a copy with the supplied fields overridden (null keeps current). */
    public RouteSettings copyWith(String name, Object arguments) {
        RouteSettings c = new RouteSettings();
        c.name = name != null ? name : this.name;
        c.arguments = arguments != null ? arguments : this.arguments;
        return c;
    }
}
