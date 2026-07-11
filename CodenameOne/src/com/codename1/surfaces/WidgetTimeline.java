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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/// The content of a widget kind: a layout descriptor plus a timeline of dated state snapshots.
/// Publishing a timeline lets the widget change over time without waking the app -- the OS (or the
/// desktop surface) switches to the entry whose date has most recently passed and interpolates its
/// state map into the layout's `${key}` placeholders:
///
/// ```java
/// WidgetTimeline t = new WidgetTimeline()
///         .setContent(deliveryLayout)
///         .addEntry(now, stateMap("Out for delivery", eta, 0.7f))
///         .addEntry(eta, stateMap("Arriving now", eta, 1.0f));
/// Surfaces.publish("delivery_status", t);
/// ```
///
/// State values may be `String`, `Number`, `Boolean`, or `Long` epoch millis for the date keys of
/// `SurfaceDynamicText` / `SurfaceProgress`. A timeline published without entries gets a single
/// implicit entry effective immediately with an empty state map.
public class WidgetTimeline {
    /// After the last entry passes, ask the platform to request fresh content (the app's
    /// `BackgroundFetch` is the refresh hook). Maps to WidgetKit's `.atEnd` policy.
    public static final int RELOAD_AT_END = 0;

    /// Keep showing the last entry until the app publishes again.
    public static final int RELOAD_NEVER = 1;

    /// One dated state snapshot within a timeline.
    public static class Entry {
        private final Date date;
        private final Map<String, Object> state;

        Entry(Date date, Map<String, Object> state) {
            this.date = date;
            this.state = state;
        }

        /// Returns the moment this entry becomes the widget's content.
        public Date getDate() {
            return date;
        }

        /// Returns the state map interpolated into the layout, never null.
        public Map<String, Object> getState() {
            return state;
        }
    }

    private SurfaceNode defaultContent;
    private SurfaceNode smallContent;
    private SurfaceNode mediumContent;
    private SurfaceNode largeContent;
    private SurfaceNode lockscreenContent;
    private final List<Entry> entries = new ArrayList<Entry>();
    private int reloadPolicy = RELOAD_AT_END;

    /// Sets the layout used for every size family that has no explicit override.
    ///
    /// #### Parameters
    ///
    /// - `root`: the layout root node
    ///
    /// #### Returns
    ///
    /// this timeline, for chaining
    public WidgetTimeline setContent(SurfaceNode root) {
        this.defaultContent = root;
        return this;
    }

    /// Sets a size-specific layout override.
    ///
    /// #### Parameters
    ///
    /// - `size`: the size family the layout applies to
    /// - `root`: the layout root node
    ///
    /// #### Returns
    ///
    /// this timeline, for chaining
    public WidgetTimeline setContent(WidgetSize size, SurfaceNode root) {
        switch (size) {
            case SMALL:
                smallContent = root;
                break;
            case MEDIUM:
                mediumContent = root;
                break;
            case LARGE:
                largeContent = root;
                break;
            case LOCKSCREEN:
                lockscreenContent = root;
                break;
            default:
                break;
        }
        return this;
    }

    /// Appends a dated state snapshot. Entries may be added in any order; they are sorted by date
    /// when published.
    ///
    /// #### Parameters
    ///
    /// - `date`: the moment the entry becomes current
    /// - `state`: the state map interpolated into the layout, may be null
    ///
    /// #### Returns
    ///
    /// this timeline, for chaining
    public WidgetTimeline addEntry(Date date, Map<String, Object> state) {
        if (date == null) {
            throw new IllegalArgumentException("Timeline entries need a date");
        }
        Map<String, Object> s = state;
        if (s == null) {
            s = new java.util.HashMap<String, Object>();
        }
        entries.add(new Entry(date, s));
        return this;
    }

    /// Sets what happens after the last entry passes.
    ///
    /// #### Parameters
    ///
    /// - `policy`: `RELOAD_AT_END` or `RELOAD_NEVER`
    ///
    /// #### Returns
    ///
    /// this timeline, for chaining
    public WidgetTimeline setReloadPolicy(int policy) {
        this.reloadPolicy = policy;
        return this;
    }

    /// Returns the layout for the given size family: the explicit override when one was set,
    /// otherwise the default content.
    ///
    /// #### Parameters
    ///
    /// - `size`: the size family
    ///
    /// #### Returns
    ///
    /// the layout root, or null when neither an override nor a default was set
    public SurfaceNode getContent(WidgetSize size) {
        SurfaceNode override = null;
        switch (size) {
            case SMALL:
                override = smallContent;
                break;
            case MEDIUM:
                override = mediumContent;
                break;
            case LARGE:
                override = largeContent;
                break;
            case LOCKSCREEN:
                override = lockscreenContent;
                break;
            default:
                break;
        }
        return override != null ? override : defaultContent;
    }

    /// Returns the explicit per-size override, or null when the size family falls back to the
    /// default content. Used by the serializer so only real overrides are emitted per size.
    SurfaceNode getExplicitContent(WidgetSize size) {
        switch (size) {
            case SMALL:
                return smallContent;
            case MEDIUM:
                return mediumContent;
            case LARGE:
                return largeContent;
            case LOCKSCREEN:
                return lockscreenContent;
            default:
                return null;
        }
    }

    /// Returns the layout used for size families without an explicit override, or null.
    public SurfaceNode getDefaultContent() {
        return defaultContent;
    }

    /// Returns the entries in the order they were added.
    public List<Entry> getEntries() {
        return entries;
    }

    /// Returns the reload policy, `RELOAD_AT_END` or `RELOAD_NEVER`.
    public int getReloadPolicy() {
        return reloadPolicy;
    }
}
