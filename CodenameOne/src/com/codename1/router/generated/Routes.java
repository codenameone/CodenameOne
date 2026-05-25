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
package com.codename1.router.generated;

/// Stub overwritten by the Codename One Maven plugin's route processor when an
/// application declares one or more `com.codename1.annotations.Route` targets.
///
/// `com.codename1.ui.Display` calls `#bootstrap` once during startup. With this
/// stub on the classpath the call is a no-op and the application sees no
/// deep-link routing -- the framework keeps working exactly as before. When
/// the maven plugin runs against a project that declares routes, the plugin
/// emits a new `Routes.class` in the project's target directory; that file
/// shadows this stub at runtime and its real `bootstrap` installs the
/// generated `RouteDispatcher`.
///
/// Application code should not call this class directly.
public final class Routes {

    private Routes() {
    }

    /// Invoked once by the framework during initialization. The stub does
    /// nothing; the generated replacement installs the project's route
    /// dispatcher.
    public static void bootstrap() {
    }
}
