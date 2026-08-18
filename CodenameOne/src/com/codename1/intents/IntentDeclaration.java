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
package com.codename1.intents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Everything the framework knows about one declared intent.
///
/// The build-time processor turns each `AppIntent` method into one of these and
/// bakes it into the generated registry, so the list is fixed by the time the
/// app runs -- which is exactly what the platforms require, since their intent
/// catalogues are compiled into the native binary.
///
/// Applications read declarations through [Intents#getDeclarations()]; the
/// simulator's Intents window is built entirely from them, so what a developer
/// sees there is what actually shipped.
public final class IntentDeclaration {

    private final String id;
    private final String title;
    private final String description;
    private final boolean headless;
    private final boolean discoverable;
    private final boolean destructive;
    private final String opensRoute;
    private final int timeoutSeconds;
    private final List<String> phrases;
    private final List<IntentParameterInfo> parameters;
    private final List<Exposure> exposure;

    /// Framework entry point: builds a declaration. Called by generated code and
    /// for a [DynamicIntent]; applications do not construct these.
    public IntentDeclaration(String id, String title, String description,
                             boolean headless, boolean discoverable, boolean destructive,
                             String opensRoute, int timeoutSeconds,
                             List<String> phrases,
                             List<IntentParameterInfo> parameters,
                             List<Exposure> exposure) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is required");
        }
        this.id = id;
        this.title = IntentText.orFallback(title, id);
        this.description = description == null ? "" : description;
        this.headless = headless;
        this.discoverable = discoverable;
        this.destructive = destructive;
        this.opensRoute = opensRoute == null ? "" : opensRoute;
        this.timeoutSeconds = timeoutSeconds;
        this.phrases = unmodifiable(phrases);
        this.parameters = unmodifiable(parameters);
        this.exposure = unmodifiable(exposure);
    }

    private static <T> List<T> unmodifiable(List<T> in) {
        return in == null
                ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(in));
    }

    /// The stable id the platform and the wire format use.
    public String getId() {
        return id;
    }

    /// The human-readable name shown in the Shortcuts app and the simulator.
    public String getTitle() {
        return title;
    }

    /// The longer explanation shown alongside the title, or an empty string.
    public String getDescription() {
        return description;
    }

    /// True when this intent is allowed to run without bringing the app to the
    /// foreground. See the package documentation for what a headless handler may
    /// and may not touch.
    public boolean isHeadless() {
        return headless;
    }

    /// True when an invocation of this intent actually runs with no window.
    ///
    /// Not the same question as [#isHeadless()], which reports what the declaration *said*. An
    /// intent that names a route is foregrounded however it was declared, because the route has
    /// to open somewhere a person can see -- iOS decides that statically through
    /// `openAppWhenRun`, and every Java caller has to reach the same answer.
    ///
    /// It exists because that combination was resolved separately in four places -- the Android
    /// trampoline, the service's post-bootstrap recheck, the parked-request path and the
    /// shortcut generator -- and each was fixed as its own bug. One definition, one answer.
    public boolean runsHeadless() {
        return headless && opensRoute.length() == 0;
    }

    /// True when the platform may offer this intent before the user has ever run
    /// it. A false value means the intent only appears after a donation.
    public boolean isDiscoverable() {
        return discoverable;
    }

    /// True when the platform should confirm with the user before running this.
    public boolean isDestructive() {
        return destructive;
    }

    /// The route template this intent navigates to, or an empty string. A
    /// non-empty value is what makes the platform open the app when the intent
    /// runs.
    public String getOpensRoute() {
        return opensRoute;
    }

    /// The handler's own time budget in seconds, before the framework gives up
    /// and reports a failure to the platform.
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /// The spoken phrases that invoke this intent. Empty on platforms that have
    /// no voice invocation, and empty for intents that never declared any.
    public List<String> getPhrases() {
        return phrases;
    }

    /// The declared parameters, in the order the handler takes them.
    public List<IntentParameterInfo> getParameters() {
        return parameters;
    }

    /// The consumers this intent is offered to.
    public List<Exposure> getExposure() {
        return exposure;
    }

    /// True when this intent is offered to the given consumer.
    ///
    /// #### Parameters
    ///
    /// - `e`: the consumer to test
    public boolean isExposedTo(Exposure e) {
        return exposure.contains(e);
    }

    /// The parameter with this name, or null.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter name
    public IntentParameterInfo getParameter(String name) {
        for (IntentParameterInfo p : parameters) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}
