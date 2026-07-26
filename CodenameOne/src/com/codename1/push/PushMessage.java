/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.push;

import com.codename1.io.JSONParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

///
/// Immutable typed push message used by clients and sending code.
///
/// <p>Common fields are provider-independent. Provider-specific options belong
/// under {@link Builder#platform(String, Map)}. Received JSON must contain a
/// supported schema number and is exposed as immutable maps and lists.</p>
public final class PushMessage {
    /// Current JSON envelope schema.
    public static final int SCHEMA_VERSION = 3;

    private final Map<String, Object> values;

    private PushMessage(Map<String, Object> values) {
        this.values = freezeMap(values);
    }

    ///
    /// Creates a message builder initialized with the current schema.
    ///
    /// @return a new builder
    public static Builder builder() {
        return new Builder();
    }

    ///
    /// Parses a schema-3 JSON envelope.
    ///
    /// @param json complete JSON object
    /// @return an immutable message
    /// @throws IOException if JSON is empty, invalid, or has an unsupported
    ///                     schema
    public static PushMessage parse(String json) throws IOException {
        if (json == null || json.trim().length() == 0) {
            throw new IOException("Push envelope must be a JSON object");
        }
        Map<String, Object> parsed = JSONParser.parseJSON(json);
        if (parsed == null) {
            throw new IOException("Push envelope must be a JSON object");
        }
        Object schema = parsed.get("schema");
        if (!(schema instanceof Number) || ((Number) schema).intValue() < SCHEMA_VERSION) {
            throw new IOException("Unsupported push envelope schema");
        }
        return new PushMessage(parsed);
    }

    /// @return the message identifier, or {@code null}
    public String getId() {
        return string("id");
    }

    /// @return the visible notification title, or {@code null}
    public String getTitle() {
        return string("title");
    }

    /// @return the visible notification body, or {@code null}
    public String getBody() {
        return string("body");
    }

    /// @return the rich-notification image URL, or {@code null}
    public String getImageUrl() {
        return string("image");
    }

    /// @return the application deep link, or {@code null}
    public String getDeepLink() {
        return string("deepLink");
    }

    /// @return the provider collapse key, or {@code null}
    public String getCollapseKey() {
        return string("collapseKey");
    }

    /// @return immutable application data, possibly empty
    public Map<String, Object> getData() {
        return map("data");
    }

    /// @return immutable widget or live-surface command data, possibly empty
    public Map<String, Object> getSurface() {
        return map("surface");
    }

    /// @return immutable provider-specific options keyed by provider
    public Map<String, Object> getPlatformOptions() {
        return map("platform");
    }

    /// @return the complete immutable envelope
    public Map<String, Object> toMap() {
        return values;
    }

    /// @return this message encoded as JSON
    public String toJson() {
        return JSONParser.mapToJson(values);
    }

    private String string(String key) {
        Object value = values.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(String key) {
        Object value = values.get(key);
        return value instanceof Map ? (Map<String, Object>) value
                : Collections.<String, Object>emptyMap();
    }

    private static Map<String, Object> freezeMap(Map<String, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            copy.put(entry.getKey(), freeze(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private static Object freeze(Object value) {
        if (value instanceof Map) {
            return freezeMap((Map<String, ?>) value);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<Object>();
            for (Object item : (List<?>) value) {
                copy.add(freeze(item));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    ///
    /// Builds an immutable {@link PushMessage}.
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<String, Object>();
        private final Map<String, Object> data = new LinkedHashMap<String, Object>();
        private final Map<String, Object> platform = new LinkedHashMap<String, Object>();

        private Builder() {
            values.put("schema", Integer.valueOf(SCHEMA_VERSION));
        }

        /// @param value message ID @return this builder
        public Builder id(String value) {
            return put("id", value);
        }

        /// @param value visible title @return this builder
        public Builder title(String value) {
            return put("title", value);
        }

        /// @param value visible body @return this builder
        public Builder body(String value) {
            return put("body", value);
        }

        /// @param value HTTPS image URL @return this builder
        public Builder imageUrl(String value) {
            return put("image", value);
        }

        /// @param value application deep link @return this builder
        public Builder deepLink(String value) {
            return put("deepLink", value);
        }

        /// @param value provider collapse key @return this builder
        public Builder collapseKey(String value) {
            return put("collapseKey", value);
        }

        ///
        /// Sets provider retention time.
        ///
        /// @param value positive lifetime in seconds
        /// @return this builder
        public Builder ttlSeconds(int value) {
            values.put("ttl", Integer.valueOf(value));
            return this;
        }

        ///
        /// Selects data-only delivery when {@code true}.
        ///
        /// @param value whether the message is silent
        /// @return this builder
        public Builder silent(boolean value) {
            values.put("silent", Boolean.valueOf(value));
            return this;
        }

        ///
        /// Adds application data, or removes the key when value is null.
        ///
        /// @param key application-defined key
        /// @param value JSON-compatible value
        /// @return this builder
        public Builder data(String key, Object value) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
            return this;
        }
        ///
        /// Adds provider-specific options.
        ///
        /// @param platformId provider ID such as {@code fcm}, {@code apns},
        ///                   {@code huawei}, {@code wns}, or {@code web}
        /// @param options JSON-compatible options, or {@code null} to remove
        /// @return this builder
        public Builder platform(String platformId, Map<String, Object> options) {
            if (options == null) {
                platform.remove(platformId);
            } else {
                platform.put(platformId, new LinkedHashMap<String, Object>(options));
            }
            return this;
        }
        ///
        /// Sets a widget or live-surface command.
        ///
        /// @param command command map, or {@code null} to remove it
        /// @return this builder
        public Builder surface(Map<String, Object> command) {
            if (command == null) {
                values.remove("surface");
            } else {
                values.put("surface", new LinkedHashMap<String, Object>(command));
            }
            return this;
        }
        private Builder put(String key, String value) {
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
            return this;
        }
        ///
        /// Creates an immutable message snapshot. The builder may be reused.
        ///
        /// @return the message
        public PushMessage build() {
            if (!data.isEmpty()) {
                values.put("data", new LinkedHashMap<String, Object>(data));
            } else {
                values.remove("data");
            }
            if (!platform.isEmpty()) {
                values.put("platform", new LinkedHashMap<String, Object>(platform));
            } else {
                values.remove("platform");
            }
            return new PushMessage(values);
        }
    }
}
