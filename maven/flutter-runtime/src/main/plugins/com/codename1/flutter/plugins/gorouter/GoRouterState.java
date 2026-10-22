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
package com.codename1.flutter.plugins.gorouter;

import dart.core.DartMap;
import dart.core.DartUri;

/**
 * go_router's {@code GoRouterState}: the location being shown, the parameters its
 * pattern captured and the {@code extra} object it was navigated to with.
 */
public class GoRouterState {
    private final DartUri uri;
    private final String matchedLocation;
    private final String fullPath;
    private final String name;
    private final DartMap<String, String> pathParameters;
    private final Object extra;
    private final Object error;

    GoRouterState(DartUri uri, String matchedLocation, String fullPath, String name,
            DartMap<String, String> pathParameters, Object extra, Object error) {
        this.uri = uri;
        this.matchedLocation = matchedLocation;
        this.fullPath = fullPath;
        this.name = name;
        this.pathParameters = pathParameters;
        this.extra = extra;
        this.error = error;
    }

    public DartUri uri() {
        return uri;
    }

    public String matchedLocation() {
        return matchedLocation;
    }

    public String fullPath() {
        return fullPath;
    }

    public String name() {
        return name;
    }

    public String path() {
        return fullPath;
    }

    public DartMap<String, String> pathParameters() {
        return pathParameters;
    }

    public DartMap<String, String> queryParameters() {
        return uri.queryParameters();
    }

    public Object extra() {
        return extra;
    }

    public Object error() {
        return error;
    }

    /** The router of the app -- {@code GoRouterState.of(context)} answers its current state. */
    public static GoRouterState of(com.codename1.flutter.BuildContext context) {
        GoRouter r = GoRouter.of(context);
        return r == null ? null : r.state();
    }
}
