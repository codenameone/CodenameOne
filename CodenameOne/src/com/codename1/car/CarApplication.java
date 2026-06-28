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
package com.codename1.car;

/// The entry point an app implements to provide its in-car (Apple CarPlay / Google Android Auto)
/// experience. Register a single instance with `Car#setApplication(CarApplication)` from your app's
/// `init()` so it is in place before a head unit connects:
///
/// ```java
/// public void init(Object ctx) {
///     // ... normal app init ...
///     Car.setApplication(new MyCarApplication());
/// }
///
/// class MyCarApplication extends CarApplication {
///     public CarScreen onCreateRootScreen(CarContext context) {
///         return new LibraryScreen();
///     }
/// }
/// ```
///
/// Referencing any `com.codename1.car` type is what tells the build to wire the native in-car
/// support (CarPlay scene + entitlement, Android Auto `CarAppService` + dependency); apps that never
/// touch the package pay nothing. The app must also be in a head-unit-eligible category and declare
/// it via build hints (see the category hints in the developer guide).
public abstract class CarApplication {

    /// Builds the root screen shown when a head unit connects. Called once per connection.
    ///
    /// #### Parameters
    ///
    /// - `context`: the live car context for the connected head unit
    ///
    /// #### Returns
    ///
    /// the root `CarScreen`; must be non-null
    public abstract CarScreen onCreateRootScreen(CarContext context);

    /// Invoked after the root screen is created, when the connection is fully established. Default is
    /// a no-op; override for one-off setup (start playback, begin a location stream).
    ///
    /// #### Parameters
    ///
    /// - `context`: the live car context
    public void onCarConnected(CarContext context) {
    }

    /// Invoked when the head unit disconnects and the in-car experience ends. Default is a no-op;
    /// override to release car-only resources.
    public void onCarDisconnected() {
    }
}
