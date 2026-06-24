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
package com.codename1.analytics;

/// Enumerates the analytics features a provider (or a server-side subscription
/// tier) may support. Code can introspect a provider via
/// {@link AnalyticsProvider#supports(AnalyticsCapability)} to decide whether a
/// given call will actually be honoured, and tooling can render the capability
/// matrix of the current account tier.
public enum AnalyticsCapability {
    /// Reporting of screen / page views.
    SCREEN_VIEWS,
    /// Arbitrary named events with parameters.
    EVENTS,
    /// Per-user properties / custom dimensions.
    USER_PROPERTIES,
    /// Crash and exception reporting.
    CRASH_REPORTING,
    /// Live (near real-time) reporting.
    REAL_TIME,
    /// Conversion funnels.
    FUNNELS,
    /// Raw, per-event data export.
    RAW_EXPORT
}
