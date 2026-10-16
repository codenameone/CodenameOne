/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.push;

import com.codename1.backend.Json;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// A push message in the schema-3 envelope the Codename One push service
/// delivers and the app's `PushClient` receives.
///
/// ```java
/// PushNotification message = PushNotification.builder()
///         .title("Your driver is here")
///         .body("Look for the blue car")
///         .deepLink("app://trip/42")
///         .data("tripId", "42")
///         .build();
/// ```
///
/// A message carries at least a body, data, or a surface command; [CloudPush]
/// refuses one that has none of them, because the service would.
///
/// This is the server's counterpart of the app's `com.codename1.push.PushMessage`
/// and writes the same fields. It isn't that class shared with the backend:
/// `PushMessage` parses and writes through `com.codename1.io.JSONParser`, which
/// reaches `com.codename1.processing.Result` and through it the XML DOM, none of
/// which the server compiles. The two agree on the wire, which is the contract
/// that matters; the envelope is JSON.
///
/// A built notification is immutable: [#toMap()] answers a copy.
public final class PushNotification {
    /// The envelope schema this class writes.
    public static final int SCHEMA_VERSION = 3;

    static final String OPERATION_WIDGET = "widget";
    static final String OPERATION_LIVE_UPDATE = "live-update";
    static final String OPERATION_LIVE_END = "live-end";

    private final Map<String, Object> values;

    private PushNotification(Map<String, Object> values) {
        this.values = copyMap(values);
    }

    /// @return a builder with the schema already set
    public static Builder builder() {
        return new Builder();
    }

    /// The envelope as JSON.
    ///
    /// @return the JSON object the service receives as `message`
    public String toJson() {
        return Json.write(values);
    }

    /// The envelope as maps and lists, in the order the fields were set.
    ///
    /// @return a copy; changing it doesn't change this notification
    public Map<String, Object> toMap() {
        return copyMap(values);
    }

    /// Whether the service would admit this as a message: it needs a body,
    /// data, or a surface command.
    boolean hasContent() {
        return values.containsKey("body") || values.containsKey("data")
                || values.containsKey("surface");
    }

    /// The live envelope, for [CloudPush] to write without a second copy.
    Map<String, Object> envelope() {
        return values;
    }

    /// A surface command that updates a running Live Activity.
    ///
    /// The app applies it with `LiveActivity.updateRemote`, before any listener
    /// sees the message, so a lock-screen activity moves on even when the app
    /// isn't running.
    ///
    /// @param activityId the id the app started the activity under
    /// @param state the activity's new state, a flat map of names to JSON
    /// values; sent as a JSON string with its keys sorted, the way the
    /// app serializes state itself
    /// @return a command for [Builder#surface(Map)]
    /// @throws IllegalArgumentException if the id is empty or a state key is null
    public static Map<String, Object> liveUpdate(String activityId, Map<String, ?> state) {
        requireText(activityId, "A Live Activity id");
        Map<String, Object> command = new LinkedHashMap<String, Object>();
        command.put("operation", OPERATION_LIVE_UPDATE);
        command.put("id", activityId);
        command.put("state", serializeState(state));
        return command;
    }

    /// A surface command that ends a Live Activity.
    ///
    /// @param activityId the id the app started the activity under
    /// @param finalState the state to show as the activity ends, or null to
    /// leave the last one
    /// @param dismissImmediately true to remove the activity now rather than
    /// letting the system keep the final state on screen for a while
    /// @return a command for [Builder#surface(Map)]
    /// @throws IllegalArgumentException if the id is empty or a state key is null
    public static Map<String, Object> liveEnd(String activityId, Map<String, ?> finalState,
                                              boolean dismissImmediately) {
        requireText(activityId, "A Live Activity id");
        Map<String, Object> command = new LinkedHashMap<String, Object>();
        command.put("operation", OPERATION_LIVE_END);
        command.put("id", activityId);
        if (finalState != null) {
            command.put("state", serializeState(finalState));
        }
        command.put("dismissImmediately", Boolean.valueOf(dismissImmediately));
        return command;
    }

    /// A surface command that publishes new widget content.
    ///
    /// @param kind the widget kind the app declared
    /// @param timelineJson the timeline, already serialized as JSON
    /// @return a command for [Builder#surface(Map)]
    /// @throws IllegalArgumentException if either argument is empty
    public static Map<String, Object> widget(String kind, String timelineJson) {
        requireText(kind, "A widget kind");
        requireText(timelineJson, "A widget timeline");
        Map<String, Object> command = new LinkedHashMap<String, Object>();
        command.put("operation", OPERATION_WIDGET);
        command.put("kind", kind);
        command.put("timeline", timelineJson);
        return command;
    }

    /// The state map as the JSON STRING the envelope carries, keys sorted.
    ///
    /// Sorted because the app's own serializer sorts, so the same state written
    /// on either side is the same text. Only the top level is sorted, as the app
    /// does: the map is meant to be flat.
    static String serializeState(Map<String, ?> state) {
        Map<String, Object> sorted = new TreeMap<String, Object>();
        if (state != null) {
            Iterator<? extends Map.Entry<String, ?>> it = state.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ?> entry = it.next();
                // Through Object, not the declared String: a caller with a raw
                // Map can put any key in it, and a cast that fails is a crash on
                // the native runtime rather than an exception.
                Object key = entry.getKey();
                if (key == null) {
                    throw new IllegalArgumentException("A state key is null");
                }
                sorted.put(String.valueOf(key), copyValue(entry.getValue()));
            }
        }
        return Json.write(sorted);
    }

    static void requireText(String value, String label) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
    }

    /// A deep copy of the maps and lists in a value, so nothing a caller keeps a
    /// reference to can change a built notification.
    static Object copyValue(Object value) {
        if (value instanceof Map) {
            return copyMap((Map<?, ?>) value);
        }
        if (value instanceof Collection) {
            List<Object> copy = new ArrayList<Object>();
            Iterator<?> it = ((Collection<?>) value).iterator();
            while (it.hasNext()) {
                copy.add(copyValue(it.next()));
            }
            return copy;
        }
        return value;
    }

    static Map<String, Object> copyMap(Map<?, ?> map) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>();
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> entry = it.next();
            copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
        }
        return copy;
    }

    /// Builds a [PushNotification]. A builder can be reused: [#build()] takes a
    /// snapshot.
    public static final class Builder {
        private final Map<String, Object> values = new LinkedHashMap<String, Object>();
        private final Map<String, Object> data = new LinkedHashMap<String, Object>();
        private final Map<String, Object> platform = new LinkedHashMap<String, Object>();

        private Builder() {
            values.put("schema", Integer.valueOf(SCHEMA_VERSION));
        }

        /// @param value an id for the message, or null to clear it
        /// @return this builder
        public Builder id(String value) {
            return put("id", value);
        }

        /// @param value the visible title, or null to clear it
        /// @return this builder
        public Builder title(String value) {
            return put("title", value);
        }

        /// @param value the visible body, or null to clear it
        /// @return this builder
        public Builder body(String value) {
            return put("body", value);
        }

        /// @param value an HTTPS URL of an image to show, or null to clear it
        /// @return this builder
        public Builder imageUrl(String value) {
            return put("image", value);
        }

        /// @param value the deep link the app opens when the user taps the
        /// notification, or null to clear it
        /// @return this builder
        public Builder deepLink(String value) {
            return put("deepLink", value);
        }

        /// @param value a key under which a newer message replaces an older
        /// undelivered one, or null to clear it
        /// @return this builder
        public Builder collapseKey(String value) {
            return put("collapseKey", value);
        }

        /// How long the providers keep trying to deliver the message.
        ///
        /// @param value the lifetime in seconds, greater than zero
        /// @return this builder
        /// @throws IllegalArgumentException if the value isn't positive
        public Builder ttlSeconds(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("Push TTL must be greater than zero");
            }
            values.put("ttl", Integer.valueOf(value));
            return this;
        }

        /// @param value true for a data-only message that shows nothing
        /// @return this builder
        public Builder silent(boolean value) {
            values.put("silent", Boolean.valueOf(value));
            return this;
        }

        /// Adds a value the app reads from the message's data, or removes it.
        ///
        /// @param key the name
        /// @param value a JSON value (string, number, boolean, map or list), or
        /// null to remove the key
        /// @return this builder
        /// @throws IllegalArgumentException if the key is empty or has
        /// surrounding whitespace
        public Builder data(String key, Object value) {
            requireKey(key, "A data key");
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, copyValue(value));
            }
            return this;
        }

        /// Adds options for one provider, which the other providers ignore.
        ///
        /// @param providerId `fcm`, `apns`, `huawei`, `wns` or `web`, or one of
        /// the aliases [PushTarget#of(String, String)] accepts
        /// @param options the provider's options, or null to remove them
        /// @return this builder
        /// @throws IllegalArgumentException if the provider is unknown
        public Builder platform(String providerId, Map<String, ?> options) {
            String canonical = PushTarget.canonicalProvider(providerId);
            if (canonical == null) {
                throw new IllegalArgumentException("Unknown push provider \"" + providerId
                        + "\"; expected fcm, apns, huawei, wns or web");
            }
            if (options == null) {
                platform.remove(canonical);
            } else {
                platform.put(canonical, copyMap(options));
            }
            return this;
        }

        /// Sets a widget or Live Activity command, which the app applies before
        /// its listener sees the message. [PushNotification#liveUpdate],
        /// [PushNotification#liveEnd] and [PushNotification#widget] build one.
        ///
        /// @param command the command, or null to remove it
        /// @return this builder
        /// @throws IllegalArgumentException if the command's `operation` isn't
        /// `widget`, `live-update` or `live-end`
        public Builder surface(Map<String, ?> command) {
            if (command == null) {
                values.remove("surface");
                return this;
            }
            Object operation = command.get("operation");
            if (!OPERATION_WIDGET.equals(operation) && !OPERATION_LIVE_UPDATE.equals(operation)
                    && !OPERATION_LIVE_END.equals(operation)) {
                throw new IllegalArgumentException("A surface command's operation is widget, "
                        + "live-update or live-end");
            }
            values.put("surface", copyMap(command));
            return this;
        }

        /// @return an immutable snapshot of the message built so far
        public PushNotification build() {
            Map<String, Object> snapshot = new LinkedHashMap<String, Object>(values);
            if (!data.isEmpty()) {
                snapshot.put("data", data);
            }
            if (!platform.isEmpty()) {
                snapshot.put("platform", platform);
            }
            return new PushNotification(snapshot);
        }

        private Builder put(String key, String value) {
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
            return this;
        }

        private static void requireKey(String value, String label) {
            if (value == null || value.length() == 0 || !value.equals(value.trim())) {
                throw new IllegalArgumentException(label
                        + " must be non-empty and must not have surrounding whitespace");
            }
        }
    }
}
