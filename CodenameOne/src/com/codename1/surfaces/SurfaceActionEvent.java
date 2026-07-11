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

import java.util.HashMap;
import java.util.Map;

/// A user interaction with an external surface: the user tapped a node that carries an action id.
/// Because surfaces render outside the (possibly dead) app process, the tap first opens or
/// foregrounds the app and the framework then delivers this event to the handler registered with
/// `Surfaces.setActionHandler(...)` on the EDT. Events that arrive before a handler is registered
/// (typically because the tap launched the app) are queued and flagged as cold start.
public class SurfaceActionEvent {
    private final String source;
    private final String actionId;
    private final Map<String, Object> params;
    private boolean coldStart;

    /// Creates an action event. Framework/port use; apps only consume these.
    ///
    /// #### Parameters
    ///
    /// - `source`: the widget kind id or live activity type the tap came from
    /// - `actionId`: the app-defined action id of the tapped node
    /// - `params`: the action parameters, may be null
    public SurfaceActionEvent(String source, String actionId, Map<String, Object> params) {
        this.source = source;
        this.actionId = actionId;
        this.params = params == null ? new HashMap<String, Object>() : params;
    }

    /// Returns the widget kind id or live activity type the tap came from.
    public String getSource() {
        return source;
    }

    /// Returns the app-defined action id of the tapped node.
    public String getActionId() {
        return actionId;
    }

    /// Returns the action parameters, never null.
    public Map<String, Object> getParams() {
        return params;
    }

    /// Returns true when this event launched the app (it arrived before an action handler was
    /// registered and was queued until registration).
    public boolean isColdStart() {
        return coldStart;
    }

    void setColdStart(boolean coldStart) {
        this.coldStart = coldStart;
    }
}
