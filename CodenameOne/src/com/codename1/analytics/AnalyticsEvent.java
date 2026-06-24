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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// An immutable analytics event: a named action with an optional category and
/// an ordered set of parameters. Build one via {@link #create(String)}:
///
/// ```java
/// Analytics.event(AnalyticsEvent.create("purchase")
///         .category("commerce")
///         .param("sku", "abc-123")
///         .param("value", 9.99)
///         .build());
/// ```
public final class AnalyticsEvent {
    private final String name;
    private final String category;
    private final Map<String, Object> parameters;
    private final long timestamp;

    AnalyticsEvent(String name, String category, Map<String, Object> parameters, long timestamp) {
        this.name = name;
        this.category = category;
        this.parameters = Collections.unmodifiableMap(parameters);
        this.timestamp = timestamp;
    }

    /// Starts building an event with the given name.
    ///
    /// #### Parameters
    ///
    /// - `name`: the event name, must not be null
    ///
    /// #### Returns
    ///
    /// a new builder
    public static Builder create(String name) {
        return new Builder(name);
    }

    /// The event name.
    ///
    /// #### Returns
    ///
    /// the name
    public String getName() {
        return name;
    }

    /// The optional event category.
    ///
    /// #### Returns
    ///
    /// the category or null
    public String getCategory() {
        return category;
    }

    /// The event parameters as an unmodifiable, insertion-ordered map.
    ///
    /// #### Returns
    ///
    /// the parameters
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /// The client timestamp in milliseconds since the epoch.
    ///
    /// #### Returns
    ///
    /// the timestamp
    public long getTimestamp() {
        return timestamp;
    }

    /// Mutable builder for {@link AnalyticsEvent}.
    public static final class Builder {
        private final String name;
        private String category;
        private final Map<String, Object> parameters = new LinkedHashMap<String, Object>();
        private long timestamp = System.currentTimeMillis();

        Builder(String name) {
            this.name = name;
        }

        /// Sets the event category.
        ///
        /// #### Parameters
        ///
        /// - `category`: the category
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder category(String category) {
            this.category = category;
            return this;
        }

        /// Adds or replaces a parameter.
        ///
        /// #### Parameters
        ///
        /// - `key`: the parameter name
        ///
        /// - `value`: the parameter value (String, Number or Boolean)
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder param(String key, Object value) {
            parameters.put(key, value);
            return this;
        }

        /// Overrides the default client timestamp.
        ///
        /// #### Parameters
        ///
        /// - `timestamp`: milliseconds since the epoch
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /// Builds the immutable event.
        ///
        /// #### Returns
        ///
        /// a new {@link AnalyticsEvent}
        public AnalyticsEvent build() {
            return new AnalyticsEvent(name, category, parameters, timestamp);
        }
    }
}
