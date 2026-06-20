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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;

import java.util.LinkedHashMap;
import java.util.Map;

/// A provider for Google Analytics 4 using the Measurement Protocol (v2). This
/// replaces the retired Universal Analytics / Measurement Protocol v1 endpoint
/// that the legacy {@code AnalyticsService} targeted.
///
/// Create one with a GA4 measurement id (`G-XXXXXXXX`) and a Measurement
/// Protocol API secret generated in the GA4 admin console:
///
/// ```java
/// Analytics.addProvider(new GoogleAnalyticsProvider("G-XXXXXXXX", "MY_API_SECRET"));
/// ```
///
/// Screen views are sent as the GA4 `screen_view` event, custom events use the
/// (sanitised) event name, and crashes are reported as `app_exception`. IP
/// addresses are anonymised by GA4 by default. Consent is enforced upstream by
/// {@link Analytics}, so this provider sends whatever it is handed.
public class GoogleAnalyticsProvider extends AbstractAnalyticsProvider {
    private static final String DEFAULT_ENDPOINT = "https://www.google-analytics.com/mp/collect";

    private final String measurementId;
    private final String apiSecret;
    private String endpoint = DEFAULT_ENDPOINT;
    private String userId;
    private final Map<String, String> userProperties = new LinkedHashMap<String, String>();

    /// Creates a GA4 provider.
    ///
    /// #### Parameters
    ///
    /// - `measurementId`: the GA4 measurement id, e.g. `G-XXXXXXXX`
    ///
    /// - `apiSecret`: a Measurement Protocol API secret
    public GoogleAnalyticsProvider(String measurementId, String apiSecret) {
        this.measurementId = measurementId;
        this.apiSecret = apiSecret;
    }

    /// Overrides the collection endpoint. Useful for routing through a
    /// first-party proxy (or a test server).
    ///
    /// #### Parameters
    ///
    /// - `url`: the collection endpoint URL
    public void setEndpoint(String url) {
        this.endpoint = url;
    }

    @Override
    public String getName() {
        return "google-analytics";
    }

    @Override
    public void setUserId(String id) {
        this.userId = id;
    }

    @Override
    public void setUserProperty(String key, String value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            userProperties.remove(key);
        } else {
            userProperties.put(sanitizeName(key), value);
        }
    }

    @Override
    public void trackScreen(String name, String referrer) {
        StringBuilder params = new StringBuilder();
        params.append('{');
        AnalyticsJson.appendString(params, "screen_name", name, true);
        if (referrer != null && referrer.length() > 0) {
            AnalyticsJson.appendString(params, "referrer", referrer, false);
        }
        AnalyticsJson.appendLong(params, "engagement_time_msec", 1, false);
        params.append('}');
        send("screen_view", params.toString());
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        StringBuilder params = new StringBuilder();
        params.append('{');
        boolean first = true;
        if (event.getCategory() != null) {
            AnalyticsJson.appendString(params, "category", event.getCategory(), true);
            first = false;
        }
        for (Map.Entry<String, Object> e : event.getParameters().entrySet()) {
            if (!first) {
                params.append(',');
            }
            params.append('"');
            AnalyticsJson.escape(params, sanitizeName(e.getKey()));
            params.append("\":");
            AnalyticsJson.appendValue(params, e.getValue());
            first = false;
        }
        params.append('}');
        send(sanitizeName(event.getName()), params.toString());
    }

    @Override
    public void reportCrash(AnalyticsCrashReport report) {
        String description = report.getMessage();
        if ((description == null || description.length() == 0) && report.getThrowable() != null) {
            description = report.getThrowable().getClass().getName();
        }
        if (description == null) {
            description = "";
        }
        if (description.length() > 100) {
            description = description.substring(0, 100);
        }
        StringBuilder params = new StringBuilder();
        params.append('{');
        AnalyticsJson.appendString(params, "description", description, true);
        AnalyticsJson.appendBoolean(params, "fatal", report.isFatal(), false);
        params.append('}');
        send("app_exception", params.toString());
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return capability == AnalyticsCapability.SCREEN_VIEWS
                || capability == AnalyticsCapability.EVENTS
                || capability == AnalyticsCapability.USER_PROPERTIES
                || capability == AnalyticsCapability.CRASH_REPORTING
                || capability == AnalyticsCapability.REAL_TIME;
    }

    private void send(String eventName, String paramsJson) {
        StringBuilder b = new StringBuilder();
        b.append('{');
        AnalyticsJson.appendString(b, "client_id", clientId(), true);
        if (userId != null) {
            AnalyticsJson.appendString(b, "user_id", userId, false);
        }
        if (!userProperties.isEmpty()) {
            b.append(",\"user_properties\":{");
            boolean firstProp = true;
            for (Map.Entry<String, String> e : userProperties.entrySet()) {
                if (!firstProp) {
                    b.append(',');
                }
                b.append('"');
                AnalyticsJson.escape(b, e.getKey());
                b.append("\":{");
                AnalyticsJson.appendString(b, "value", e.getValue(), true);
                b.append('}');
                firstProp = false;
            }
            b.append('}');
        }
        b.append(",\"events\":[{");
        AnalyticsJson.appendString(b, "name", eventName, true);
        b.append(",\"params\":").append(paramsJson);
        b.append("}]}");
        post(b.toString());
    }

    private void post(String json) {
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl(endpoint + "?measurement_id=" + Util.encodeUrl(measurementId)
                + "&api_secret=" + Util.encodeUrl(apiSecret));
        req.setPost(true);
        req.setHttpMethod("POST");
        req.setContentType("application/json");
        req.setRequestBody(json);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    private String clientId() {
        AnalyticsContext ctx = getContext();
        if (ctx != null && ctx.getClientId() != null) {
            return ctx.getClientId();
        }
        return "";
    }

    /// GA4 event and parameter names must be alphanumeric or underscore and
    /// begin with a letter. This replaces any other character with an
    /// underscore and prefixes a leading digit.
    private static String sanitizeName(String name) {
        if (name == null || name.length() == 0) {
            return "event";
        }
        StringBuilder b = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
            boolean digit = c >= '0' && c <= '9';
            if (letter || digit || c == '_') {
                b.append(c);
            } else {
                b.append('_');
            }
        }
        char first = b.charAt(0);
        if (!((first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z') || first == '_')) {
            b.insert(0, '_');
        }
        String result = b.toString();
        if (result.length() > 40) {
            return result.substring(0, 40);
        }
        return result;
    }
}
