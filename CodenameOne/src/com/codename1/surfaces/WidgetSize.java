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
package com.codename1.surfaces;

/// The size families a widget kind supports.
///
/// The first four are the phone families: iOS maps them to the WidgetKit families
/// (`systemSmall` / `systemMedium` / `systemLarge`, and `accessoryRectangular` for `LOCKSCREEN`);
/// Android and desktop treat them as size hints, and `LOCKSCREEN` is ignored on Android.
///
/// The `WATCH_*` families are **complications** -- the small live readouts on a watch face. They
/// live here rather than in an API of their own because they are the same concept as a widget:
/// content-driven, rendered while your app is not running, and fed by the same [WidgetTimeline]. On
/// Apple a complication is literally a WidgetKit widget in an accessory family; on Wear OS the
/// simple families become complication data and the richer ones become a Tile.
///
/// Design them for a glance. A complication is a few dozen pixels someone reads in under a second,
/// so a `SurfaceVector` gauge or a single number beats any layout that has to be read.
public enum WidgetSize {
    /// Small square home-screen widget. iOS `systemSmall`.
    SMALL("small"),
    /// Medium home-screen widget. iOS `systemMedium`.
    MEDIUM("medium"),
    /// Large home-screen widget. iOS `systemLarge`.
    LARGE("large"),
    /// Lock-screen widget. iOS `accessoryRectangular`.
    LOCKSCREEN("lockscreen"),
    /// Round complication -- the corner or centre slots of a watch face. iOS `accessoryCircular`;
    /// Wear OS `RANGED_VALUE` or `MONOCHROMATIC_IMAGE`. Room for a gauge or one glyph.
    WATCH_CIRCULAR("watchCircular"),
    /// Wide complication, a band across the watch face. iOS `accessoryRectangular`; Wear OS
    /// `LONG_TEXT`, or a Tile when the layout is richer than text. The roomiest family.
    WATCH_RECTANGULAR("watchRectangular"),
    /// One line of text alongside the time. iOS `accessoryInline`; Wear OS `SHORT_TEXT`. Text only --
    /// anything else is dropped.
    WATCH_INLINE("watchInline"),
    /// Curved complication hugging the bezel of a round face. iOS `accessoryCorner`; renders as the
    /// circular family on Wear OS, which has no corner slot.
    WATCH_CORNER("watchCorner");

    private final String jsonName;

    WidgetSize(String jsonName) {
        this.jsonName = jsonName;
    }

    /// Returns the wire-format name used in the serialized descriptor.
    public String getJsonName() {
        return jsonName;
    }

    /// True for the watch complication families, which are published to a watch face rather than to
    /// a home or lock screen.
    ///
    /// #### Returns
    ///
    /// true if this is a complication family
    public boolean isWatchFamily() {
        return this == WATCH_CIRCULAR || this == WATCH_RECTANGULAR
                || this == WATCH_INLINE || this == WATCH_CORNER;
    }

    /// Resolves a wire-format name back to its family.
    ///
    /// #### Parameters
    ///
    /// - `jsonName`: the name produced by [#getJsonName()]
    ///
    /// #### Returns
    ///
    /// the matching family, or null when the name is unknown
    public static WidgetSize fromJsonName(String jsonName) {
        for (WidgetSize s : values()) {
            if (s.jsonName.equals(jsonName)) {
                return s;
            }
        }
        return null;
    }
}
