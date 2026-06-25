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

/// A turn-by-turn navigation surface: a drawable map (via a `CarSurfaceCallback`), an optional
/// next-manoeuvre / arrival info strip, and map-control action strips. Maps to
/// `androidx.car.app.navigation.model.NavigationTemplate` and `CPMapTemplate`.
///
/// Navigation is the only category with a pixel surface and it is the most tightly gated: the app
/// must declare the navigation category and hold the platform navigation entitlement
/// (`ios.carplay.navigation` build hint on iOS; the builder injects the
/// `androidx.car.app.category.NAVIGATION` filter and `androidx.car.app.MAP_TEMPLATES` permission on
/// Android).
public class CarNavigationTemplate extends CarTemplate {
    private CarSurfaceCallback surfaceCallback;
    private String nextManeuver;
    private String distanceRemaining;
    private String timeRemaining;
    private CarActionStrip mapActions;
    private CarActionStrip headerActions;
    private boolean navigating;

    /// Returns the map drawing callback, or null.
    public CarSurfaceCallback getSurfaceCallback() {
        return surfaceCallback;
    }

    /// Sets the callback that draws the moving map onto the head-unit surface.
    ///
    /// #### Parameters
    ///
    /// - `surfaceCallback`: the drawing callback
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setSurfaceCallback(CarSurfaceCallback surfaceCallback) {
        this.surfaceCallback = surfaceCallback;
        return this;
    }

    /// Returns the next-manoeuvre instruction text, or null.
    public String getNextManeuver() {
        return nextManeuver;
    }

    /// Sets the next-manoeuvre instruction (e.g. "Turn right onto Main St").
    ///
    /// #### Parameters
    ///
    /// - `nextManeuver`: the instruction text
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setNextManeuver(String nextManeuver) {
        this.nextManeuver = nextManeuver;
        return this;
    }

    /// Returns the remaining-distance string, or null.
    public String getDistanceRemaining() {
        return distanceRemaining;
    }

    /// Sets the remaining-distance string (e.g. "300 m").
    ///
    /// #### Parameters
    ///
    /// - `distanceRemaining`: the distance text
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setDistanceRemaining(String distanceRemaining) {
        this.distanceRemaining = distanceRemaining;
        return this;
    }

    /// Returns the remaining-time / ETA string, or null.
    public String getTimeRemaining() {
        return timeRemaining;
    }

    /// Sets the remaining-time / ETA string (e.g. "12 min").
    ///
    /// #### Parameters
    ///
    /// - `timeRemaining`: the time text
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setTimeRemaining(String timeRemaining) {
        this.timeRemaining = timeRemaining;
        return this;
    }

    /// Returns the map-control action strip (zoom, recenter), or null.
    public CarActionStrip getMapActions() {
        return mapActions;
    }

    /// Sets the map-control action strip (zoom in/out, recenter).
    ///
    /// #### Parameters
    ///
    /// - `mapActions`: the action strip
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setMapActions(CarActionStrip mapActions) {
        this.mapActions = mapActions;
        return this;
    }

    /// Returns the header action strip, or null.
    public CarActionStrip getHeaderActions() {
        return headerActions;
    }

    /// Sets the header action strip.
    ///
    /// #### Parameters
    ///
    /// - `headerActions`: the action strip
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setHeaderActions(CarActionStrip headerActions) {
        this.headerActions = headerActions;
        return this;
    }

    /// Returns true when turn-by-turn guidance is active (the info strip is shown).
    public boolean isNavigating() {
        return navigating;
    }

    /// Marks whether guidance is active; when true the head unit shows the manoeuvre/ETA strip.
    ///
    /// #### Parameters
    ///
    /// - `navigating`: true while guiding
    ///
    /// #### Returns
    ///
    /// this template, for chaining
    public CarNavigationTemplate setNavigating(boolean navigating) {
        this.navigating = navigating;
        return this;
    }
}
