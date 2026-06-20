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
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// The Codename One first-party analytics provider. It batches events and
/// posts them as JSON to the Codename One cloud
/// (`/api/v2/analytics/events`), where they are stored and aggregated for the
/// reports shown in the developer console. The capabilities you actually get
/// (screen views, custom events, retention window, raw export) are gated
/// server-side by your subscription tier.
///
/// App identity is read from the build-injected `Display` properties
/// (`build_key`, `package_name`, `AppName`, `AppVersion`, `OSVer`) -- the same
/// mechanism the on-device crash client uses -- so no API key needs to be
/// embedded in the app. Events are buffered in memory and flushed when the
/// batch fills up or when {@link #flush()} is called.
///
/// ```java
/// Analytics.addProvider(new CodenameOneAnalyticsProvider());
/// Analytics.setConsent(AnalyticsConsent.granted());
/// ```
public class CodenameOneAnalyticsProvider extends AbstractAnalyticsProvider {
    private static final String DEFAULT_BASE_URL = "https://cloud.codenameone.com";
    private static final String PATH = "/api/v2/analytics/events";
    private static final String SCREEN_VIEW = "screen_view";
    private static final int DEFAULT_BATCH_SIZE = 20;

    private final Object lock = new Object();
    private final List<PendingEvent> buffer = new ArrayList<PendingEvent>();
    private int batchSize = DEFAULT_BATCH_SIZE;
    private String endpoint;

    /// Overrides the ingest endpoint URL. By default the provider posts to the
    /// Codename One cloud (honouring the `cloudServerURL` display property).
    ///
    /// #### Parameters
    ///
    /// - `url`: the full ingest URL
    public void setEndpoint(String url) {
        this.endpoint = url;
    }

    /// Sets the number of events buffered before an automatic flush.
    ///
    /// #### Parameters
    ///
    /// - `size`: the batch size, values below 1 are clamped to 1
    public void setBatchSize(int size) {
        this.batchSize = size < 1 ? 1 : size;
    }

    @Override
    public String getName() {
        return "codenameone";
    }

    @Override
    public void trackScreen(String name, String referrer) {
        PendingEvent e = new PendingEvent();
        e.name = SCREEN_VIEW;
        e.screen = name;
        if (referrer != null && referrer.length() > 0) {
            e.params = new java.util.LinkedHashMap<String, Object>();
            e.params.put("referrer", referrer);
        }
        enqueue(e);
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        PendingEvent e = new PendingEvent();
        e.name = event.getName();
        e.category = event.getCategory();
        e.clientTs = event.getTimestamp();
        e.params = event.getParameters();
        enqueue(e);
    }

    @Override
    public void setUserProperty(String key, String value) {
        PendingEvent e = new PendingEvent();
        e.name = "user_property";
        e.params = new java.util.LinkedHashMap<String, Object>();
        e.params.put("key", key);
        e.params.put("value", value);
        enqueue(e);
    }

    @Override
    public void reportCrash(AnalyticsCrashReport report) {
        String description = report.getMessage();
        if ((description == null || description.length() == 0) && report.getThrowable() != null) {
            description = report.getThrowable().getClass().getName();
        }
        PendingEvent e = new PendingEvent();
        e.name = "app_exception";
        e.params = new java.util.LinkedHashMap<String, Object>();
        e.params.put("description", description == null ? "" : description);
        e.params.put("fatal", Boolean.valueOf(report.isFatal()));
        enqueue(e);
    }

    @Override
    public void flush() {
        List<PendingEvent> pending;
        synchronized (lock) {
            if (buffer.isEmpty()) {
                return;
            }
            pending = new ArrayList<PendingEvent>(buffer);
            buffer.clear();
        }
        post(buildBatch(pending));
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return capability == AnalyticsCapability.SCREEN_VIEWS
                || capability == AnalyticsCapability.EVENTS
                || capability == AnalyticsCapability.USER_PROPERTIES
                || capability == AnalyticsCapability.CRASH_REPORTING
                || capability == AnalyticsCapability.REAL_TIME
                || capability == AnalyticsCapability.FUNNELS
                || capability == AnalyticsCapability.RAW_EXPORT;
    }

    private void enqueue(PendingEvent e) {
        if (e.clientTs <= 0) {
            e.clientTs = System.currentTimeMillis();
        }
        boolean full;
        synchronized (lock) {
            buffer.add(e);
            full = buffer.size() >= batchSize;
        }
        if (full) {
            flush();
        }
    }

    private String buildBatch(List<PendingEvent> events) {
        AnalyticsContext ctx = getContext();
        Display d = Display.getInstance();
        String clientId = ctx == null || ctx.getClientId() == null ? "" : ctx.getClientId();
        String appName = ctx == null ? "" : ctx.getAppName();
        String appVersion = ctx == null ? "" : ctx.getAppVersion();
        String platform = ctx == null ? "" : ctx.getPlatform();
        String locale = ctx == null ? "" : ctx.getLocale();
        String buildKey = d == null ? "" : d.getProperty("build_key", "");
        String packageName = d == null ? "" : d.getProperty("package_name", "");
        String osVersion = d == null ? "" : d.getProperty("OSVer", "");
        if (locale == null || locale.length() == 0) {
            Locale loc = Locale.getDefault();
            locale = loc == null ? "" : loc.toString();
        }

        StringBuilder b = new StringBuilder(256);
        b.append('{');
        AnalyticsJson.appendString(b, "clientId", clientId, true);
        AnalyticsJson.appendString(b, "buildKey", buildKey, false);
        AnalyticsJson.appendString(b, "packageName", packageName, false);
        AnalyticsJson.appendString(b, "appName", appName, false);
        AnalyticsJson.appendString(b, "appVersion", appVersion, false);
        AnalyticsJson.appendString(b, "platform", platform, false);
        AnalyticsJson.appendString(b, "osVersion", osVersion, false);
        AnalyticsJson.appendString(b, "locale", locale, false);
        // Reaching here means the Analytics facade already satisfied the
        // analytics consent gate; the flag tells the server consent was
        // granted (consent travels with the data).
        AnalyticsJson.appendBoolean(b, "consentAnalytics", true, false);
        b.append(",\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            appendEvent(b, events.get(i));
        }
        b.append("]}");
        return b.toString();
    }

    private static void appendEvent(StringBuilder b, PendingEvent e) {
        b.append('{');
        AnalyticsJson.appendString(b, "name", e.name, true);
        AnalyticsJson.appendString(b, "category", e.category, false);
        AnalyticsJson.appendString(b, "screen", e.screen, false);
        AnalyticsJson.appendLong(b, "clientTs", e.clientTs, false);
        b.append(",\"params\":");
        if (e.params == null || e.params.isEmpty()) {
            b.append("{}");
        } else {
            b.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> p : e.params.entrySet()) {
                if (!first) {
                    b.append(',');
                }
                b.append('"');
                AnalyticsJson.escape(b, p.getKey());
                b.append("\":");
                AnalyticsJson.appendValue(b, p.getValue());
                first = false;
            }
            b.append('}');
        }
        b.append('}');
    }

    private void post(String json) {
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl(resolveEndpoint());
        req.setPost(true);
        req.setHttpMethod("POST");
        req.setContentType("application/json");
        req.setRequestBody(json);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    private String resolveEndpoint() {
        if (endpoint != null && endpoint.length() > 0) {
            return endpoint;
        }
        Display d = Display.getInstance();
        String base = d == null ? DEFAULT_BASE_URL : d.getProperty("cloudServerURL", DEFAULT_BASE_URL);
        if (base == null || base.length() == 0) {
            base = DEFAULT_BASE_URL;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + PATH;
    }

    private static final class PendingEvent {
        private String name;
        private String category;
        private String screen;
        private long clientTs;
        private Map<String, Object> params;
    }
}
