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

/// A marker pinned to a geographic location on a map. Create one from a
/// [MarkerOptions] via [MapSurface#addMarker(MarkerOptions)]; the returned
/// instance is a live handle whose mutators ([#setPosition], [#setVisible])
/// update the rendered marker on the next repaint.
public final class Marker extends MapObject {

    private LatLng position;
    private final EncodedImage icon;
    private final String title;
    private final String snippet;
    private final float anchorU;
    private final float anchorV;
    private final boolean draggable;
    private boolean visible;
    private final ActionListener onClick;

    Marker(MarkerOptions options) {
        this.position = options.getPosition();
        this.icon = options.getIcon();
        this.title = options.getTitle();
        this.snippet = options.getSnippet();
        this.anchorU = options.getAnchorU();
        this.anchorV = options.getAnchorV();
        this.draggable = options.isDraggable();
        this.visible = true;
        this.onClick = options.getOnClick();
    }

    /// The marker location.
    public LatLng getPosition() {
        return position;
    }

    /// Moves the marker to a new location.
    public void setPosition(LatLng position) {
        this.position = position;
    }

    /// The marker icon, or `null` to use the surface's default pin.
    public EncodedImage getIcon() {
        return icon;
    }

    /// The marker title shown in an info window (provider dependent).
    public String getTitle() {
        return title;
    }

    /// The secondary text shown beneath the title (provider dependent).
    public String getSnippet() {
        return snippet;
    }

    /// The horizontal icon anchor in normalized [0,1] image space
    /// (0 = left, 1 = right). Defaults to 0.5 (centered).
    public float getAnchorU() {
        return anchorU;
    }

    /// The vertical icon anchor in normalized [0,1] image space
    /// (0 = top, 1 = bottom). Defaults to 1 (pin tip at the location).
    public float getAnchorV() {
        return anchorV;
    }

    /// Whether the user may drag this marker (native providers only).
    public boolean isDraggable() {
        return draggable;
    }

    /// Whether the marker is currently rendered.
    public boolean isVisible() {
        return visible;
    }

    /// Shows or hides the marker.
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /// The listener invoked when the marker is tapped, or `null`.
    public ActionListener getOnClick() {
        return onClick;
    }
}
