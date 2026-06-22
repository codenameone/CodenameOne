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
package com.codename1.maps;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.events.ActionListener;

/// A fluent builder describing a [Marker] before it is added to a
/// [MapSurface]. Only the position is required; every other property has a
/// sensible default.
///
/// ```java
/// map.addMarker(new MarkerOptions(new LatLng(37.78, -122.40))
///         .icon(pin)
///         .title("Union Square")
///         .anchor(0.5f, 1.0f)
///         .onClick(e -> showDetails()));
/// ```
public final class MarkerOptions {

    private LatLng position;
    private EncodedImage icon;
    private String title;
    private String snippet;
    private float anchorU = 0.5f;
    private float anchorV = 1.0f;
    private boolean draggable;
    private ActionListener onClick;

    /// Starts a builder for a marker at `position`.
    public MarkerOptions(LatLng position) {
        this.position = position;
    }

    /// Starts a builder with the location supplied later via [#position].
    public MarkerOptions() {
    }

    /// Sets the marker location.
    public MarkerOptions position(LatLng position) {
        this.position = position;
        return this;
    }

    /// Sets the marker icon. When `null` the surface renders its default pin.
    public MarkerOptions icon(EncodedImage icon) {
        this.icon = icon;
        return this;
    }

    /// Sets the info-window title.
    public MarkerOptions title(String title) {
        this.title = title;
        return this;
    }

    /// Sets the info-window secondary text.
    public MarkerOptions snippet(String snippet) {
        this.snippet = snippet;
        return this;
    }

    /// Sets the icon anchor in normalized [0,1] image space.
    public MarkerOptions anchor(float u, float v) {
        this.anchorU = u;
        this.anchorV = v;
        return this;
    }

    /// Makes the marker draggable (native providers only).
    public MarkerOptions draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    /// Sets the tap listener.
    public MarkerOptions onClick(ActionListener onClick) {
        this.onClick = onClick;
        return this;
    }

    /// Builds an immutable-by-convention [Marker] from this builder.
    public Marker build() {
        return new Marker(this);
    }

    LatLng getPosition() {
        return position;
    }

    EncodedImage getIcon() {
        return icon;
    }

    String getTitle() {
        return title;
    }

    String getSnippet() {
        return snippet;
    }

    float getAnchorU() {
        return anchorU;
    }

    float getAnchorV() {
        return anchorV;
    }

    boolean isDraggable() {
        return draggable;
    }

    ActionListener getOnClick() {
        return onClick;
    }
}
