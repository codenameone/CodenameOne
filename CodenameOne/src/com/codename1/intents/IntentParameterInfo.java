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

/// One parameter of a declared intent, as the framework and the simulator see
/// it. Built by the build-time generated registry from the `IntentParam`
/// annotation; applications read these but never construct them.
public final class IntentParameterInfo {

    private final String name;
    private final String title;
    private final IntentParameterType type;
    private final boolean required;
    private final String entityType;
    private final String defaultValue;
    private final List<String> options;

    /// Framework entry point: builds a parameter description. Called by
    /// generated code.
    ///
    /// #### Parameters
    ///
    /// - `name`: the parameter name used on the wire
    /// - `title`: the prompt shown when the platform asks for this value
    /// - `type`: the parameter kind
    /// - `required`: whether the intent can run without it
    /// - `entityType`: the entity type id when `type` is `ENTITY`, else null
    /// - `defaultValue`: the value used when an optional parameter is absent
    /// - `options`: the closed vocabulary, or null when the value is free
    public IntentParameterInfo(String name, String title, IntentParameterType type,
                               boolean required, String entityType, String defaultValue,
                               List<String> options) {
        this.name = name;
        // Trimmed, not merely checked for empty: a title of spaces is present, so the
        // fallback never fired and the platform showed a prompt with nothing in it. The build
        // normalizes the same way, so a declaration reads identically in the simulator and on
        // a device.
        this.title = title == null || title.trim().length() == 0 ? name : title;
        this.type = type;
        this.required = required;
        this.entityType = entityType;
        this.defaultValue = defaultValue;
        this.options = options == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(options));
    }

    /// The parameter name used on the wire and in the parameter map.
    public String getName() {
        return name;
    }

    /// The prompt the platform shows when it has to ask for this value --
    /// "Which playlist?". Falls back to the name when the declaration gave none.
    public String getTitle() {
        return title;
    }

    /// The parameter kind.
    public IntentParameterType getType() {
        return type;
    }

    /// Whether the intent can run without this value.
    public boolean isRequired() {
        return required;
    }

    /// The entity type id when [#getType()] is [IntentParameterType#ENTITY],
    /// otherwise null.
    public String getEntityType() {
        return entityType;
    }

    /// The value substituted when an optional parameter is absent, or null.
    public String getDefaultValue() {
        return defaultValue;
    }

    /// The closed vocabulary this parameter accepts, or an empty list when any
    /// value is allowed.
    public List<String> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return name + ":" + type;
    }
}
