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

/// Describes a live activity: an ongoing-state surface (delivery, timer, ride, score) that stays
/// visible outside the app while it is in progress. The layout is built once from surface nodes
/// with `${key}` placeholders; each `LiveActivity.update(...)` then ships only a fresh state map.
///
/// Platform lowering:
///
/// - **iOS**: ActivityKit live activity. `setContent(...)` is the lock screen / banner
///   presentation; the `set...(...)` region setters fill the Dynamic Island (ignored on devices
///   without one).
/// - **Android**: an ongoing notification whose collapsed and expanded views render
///   `setCompactLeading`/`setCompactTrailing` and `setContent` respectively.
/// - **Desktop**: a floating always-on-top pill window rendering `setContent`.
public class LiveActivityDescriptor {
    private final String activityType;
    private SurfaceNode content;
    private SurfaceNode compactLeading;
    private SurfaceNode compactTrailing;
    private SurfaceNode minimal;
    private SurfaceNode expandedLeading;
    private SurfaceNode expandedTrailing;
    private SurfaceNode expandedCenter;
    private SurfaceNode expandedBottom;
    private SurfaceColor tint;
    private String androidChannelId;

    /// Creates a live activity descriptor.
    ///
    /// #### Parameters
    ///
    /// - `activityType`: an app-defined type tag (e.g. `delivery`), delivered back as the source
    ///   of action events from this activity
    public LiveActivityDescriptor(String activityType) {
        this.activityType = activityType;
    }

    /// Sets the primary presentation: the iOS lock screen / banner layout, the Android expanded
    /// notification and the desktop pill content.
    ///
    /// #### Parameters
    ///
    /// - `root`: the layout root node
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setContent(SurfaceNode root) {
        this.content = root;
        return this;
    }

    /// Sets the Dynamic Island compact leading region (left of the camera cutout).
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setCompactLeading(SurfaceNode node) {
        this.compactLeading = node;
        return this;
    }

    /// Sets the Dynamic Island compact trailing region (right of the camera cutout).
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setCompactTrailing(SurfaceNode node) {
        this.compactTrailing = node;
        return this;
    }

    /// Sets the Dynamic Island minimal region, shown when multiple activities compete for the
    /// island.
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setMinimal(SurfaceNode node) {
        this.minimal = node;
        return this;
    }

    /// Sets the leading region of the expanded (long-pressed) Dynamic Island.
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setExpandedLeading(SurfaceNode node) {
        this.expandedLeading = node;
        return this;
    }

    /// Sets the trailing region of the expanded (long-pressed) Dynamic Island.
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setExpandedTrailing(SurfaceNode node) {
        this.expandedTrailing = node;
        return this;
    }

    /// Sets the center region of the expanded (long-pressed) Dynamic Island.
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setExpandedCenter(SurfaceNode node) {
        this.expandedCenter = node;
        return this;
    }

    /// Sets the bottom region of the expanded (long-pressed) Dynamic Island.
    ///
    /// #### Parameters
    ///
    /// - `node`: the region content
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setExpandedBottom(SurfaceNode node) {
        this.expandedBottom = node;
        return this;
    }

    /// Sets the accent color of the activity's chrome (island key line, notification accent).
    ///
    /// #### Parameters
    ///
    /// - `tint`: the accent color
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setTint(SurfaceColor tint) {
        this.tint = tint;
        return this;
    }

    /// Sets the Android notification channel the activity's ongoing notification posts to. When
    /// unset a default `cn1_live_activities` channel is used.
    ///
    /// #### Parameters
    ///
    /// - `channelId`: the channel id
    ///
    /// #### Returns
    ///
    /// this descriptor, for chaining
    public LiveActivityDescriptor setAndroidChannelId(String channelId) {
        this.androidChannelId = channelId;
        return this;
    }

    /// Returns the app-defined activity type tag.
    public String getActivityType() {
        return activityType;
    }

    /// Returns the primary presentation layout, or null.
    public SurfaceNode getContent() {
        return content;
    }

    /// Returns the compact leading island region, or null.
    public SurfaceNode getCompactLeading() {
        return compactLeading;
    }

    /// Returns the compact trailing island region, or null.
    public SurfaceNode getCompactTrailing() {
        return compactTrailing;
    }

    /// Returns the minimal island region, or null.
    public SurfaceNode getMinimal() {
        return minimal;
    }

    /// Returns the expanded leading island region, or null.
    public SurfaceNode getExpandedLeading() {
        return expandedLeading;
    }

    /// Returns the expanded trailing island region, or null.
    public SurfaceNode getExpandedTrailing() {
        return expandedTrailing;
    }

    /// Returns the expanded center island region, or null.
    public SurfaceNode getExpandedCenter() {
        return expandedCenter;
    }

    /// Returns the expanded bottom island region, or null.
    public SurfaceNode getExpandedBottom() {
        return expandedBottom;
    }

    /// Returns the accent color, or null.
    public SurfaceColor getTint() {
        return tint;
    }

    /// Returns the Android notification channel id, or null for the default channel.
    public String getAndroidChannelId() {
        return androidChannelId;
    }
}
