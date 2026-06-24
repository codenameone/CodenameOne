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
import com.codename1.ui.CN;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

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
    // A pseudonymous session id, generated once per app run (per provider
    // instance). It is not persisted, so it naturally resets when the app is
    // relaunched -- the server uses it to group events into sessions and to
    // reconstruct screen-to-screen flows without needing any hardware id.
    private String sessionId;

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

        StringBuilder b = new StringBuilder(384);
        b.append('{');
        AnalyticsJson.appendString(b, "clientId", clientId, true);
        AnalyticsJson.appendString(b, "sessionId", sessionId(), false);
        AnalyticsJson.appendString(b, "buildKey", buildKey, false);
        AnalyticsJson.appendString(b, "packageName", packageName, false);
        AnalyticsJson.appendString(b, "appName", appName, false);
        AnalyticsJson.appendString(b, "appVersion", appVersion, false);
        AnalyticsJson.appendString(b, "platform", platform, false);
        AnalyticsJson.appendString(b, "osVersion", osVersion, false);
        AnalyticsJson.appendString(b, "locale", locale, false);
        // Device segmentation metadata (first-party only). Shared by every
        // event in the batch, so the server denormalises it onto each stored
        // row. None of it is a hardware identifier: the model is the public
        // marketing/hardware string, never the user-assigned device name.
        appendDeviceMetadata(b, d, locale);
        // The desktop OS name (JavaSE exposes os.name; harmless empty on mobile
        // ports). The platform field above already carries the coarse platform.
        String osName = d == null ? "" : d.getProperty("os.name", "");
        if (osName != null) {
            osName = osName.trim();
        }
        AnalyticsJson.appendString(b, "osName", osName == null ? "" : osName, false);
        // A coarse browser label on the JavaScript port (empty elsewhere).
        AnalyticsJson.appendString(b, "browser", browserName(d), false);
        // The app-scoped custom dimensions the console segments by. Always
        // emitted, as an empty object when none are set.
        b.append(",\"dimensions\":");
        appendDimensions(b);
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

    /// Appends the shared device-segmentation fields to the batch JSON. Every
    /// value is read defensively so a single unavailable property never aborts
    /// a flush. The device model is the public hardware/marketing string
    /// (`iPhone15,2`, `Pixel 8`) -- never the user-assigned device name, which
    /// would be personally identifying.
    private void appendDeviceMetadata(StringBuilder b, Display d, String locale) {
        String deviceModel = "";
        String manufacturer = "";
        String formFactor = "phone";
        String density = "";
        int screenWidth = 0;
        int screenHeight = 0;
        String network = "";
        String language = "";
        if (d != null) {
            // DeviceHardwareModel is the privacy-safe hardware identifier added
            // to the iOS/Android ports; DeviceName is the fallback for ports
            // that don't implement it (on Android it is already Build.MODEL).
            deviceModel = d.getProperty("DeviceHardwareModel", d.getProperty("DeviceName", ""));
            manufacturer = d.getProperty("DeviceManufacturer", "");
            formFactor = formFactor(d);
            density = densityName(d.getDeviceDensity());
            screenWidth = d.getDisplayWidth();
            screenHeight = d.getDisplayHeight();
        }
        try {
            network = networkName(NetworkManager.getInstance().getCurrentNetworkType());
        } catch (Throwable t) {
            network = "";
        }
        Locale loc = Locale.getDefault();
        if (loc != null) {
            language = loc.getLanguage();
        }
        if ((language == null || language.length() == 0) && locale != null) {
            int us = locale.indexOf('_');
            language = us > 0 ? locale.substring(0, us) : locale;
        }
        AnalyticsJson.appendString(b, "deviceModel", deviceModel, false);
        AnalyticsJson.appendString(b, "deviceManufacturer", manufacturer, false);
        AnalyticsJson.appendString(b, "formFactor", formFactor, false);
        AnalyticsJson.appendString(b, "density", density, false);
        AnalyticsJson.appendLong(b, "screenWidth", screenWidth, false);
        AnalyticsJson.appendLong(b, "screenHeight", screenHeight, false);
        AnalyticsJson.appendString(b, "network", network, false);
        AnalyticsJson.appendString(b, "language", language == null ? "" : language, false);
    }

    private static String formFactor(Display d) {
        if (CN.isWatch()) {
            return "watch";
        }
        if (d.isDesktop()) {
            return "desktop";
        }
        if (d.isTablet()) {
            return "tablet";
        }
        return "phone";
    }

    private static String densityName(int density) {
        switch (density) {
            case Display.DENSITY_VERY_LOW: return "very_low";
            case Display.DENSITY_LOW: return "low";
            case Display.DENSITY_MEDIUM: return "medium";
            case Display.DENSITY_HIGH: return "high";
            case Display.DENSITY_VERY_HIGH: return "very_high";
            case Display.DENSITY_HD: return "hd";
            case Display.DENSITY_560: return "xhd";
            case Display.DENSITY_2HD: return "2hd";
            case Display.DENSITY_4K: return "4k";
            default: return "";
        }
    }

    private static String networkName(int type) {
        switch (type) {
            case NetworkManager.NETWORK_TYPE_WIFI: return "wifi";
            case NetworkManager.NETWORK_TYPE_CELLULAR: return "cellular";
            case NetworkManager.NETWORK_TYPE_ETHERNET: return "ethernet";
            case NetworkManager.NETWORK_TYPE_BLUETOOTH: return "bluetooth";
            case NetworkManager.NETWORK_TYPE_NONE: return "none";
            default: return "";
        }
    }

    /// Derives a coarse browser label from the user agent on the JavaScript
    /// port. Returns the empty string on every non-web platform (where the
    /// user agent property is absent). The order of the checks matters: Edge
    /// and Opera advertise a Chrome token, so they are tested first.
    private static String browserName(Display d) {
        if (d == null) {
            return "";
        }
        String ua = d.getProperty("User-Agent", "");
        if (ua == null || ua.length() == 0) {
            return "";
        }
        ua = ua.toLowerCase();
        if (ua.indexOf("edg") >= 0) {
            return "Edge";
        }
        if (ua.indexOf("opr") >= 0 || ua.indexOf("opera") >= 0) {
            return "Opera";
        }
        if (ua.indexOf("firefox") >= 0) {
            return "Firefox";
        }
        if (ua.indexOf("chrome") >= 0 || ua.indexOf("crios") >= 0) {
            return "Chrome";
        }
        if (ua.indexOf("safari") >= 0) {
            return "Safari";
        }
        return "other";
    }

    /// Appends the custom dimensions as a nested JSON object built from
    /// {@link Analytics#getDimensions()}. Emits {@code {}} when empty.
    private static void appendDimensions(StringBuilder b) {
        Map<String, String> dimensions = Analytics.getDimensions();
        if (dimensions.isEmpty()) {
            b.append("{}");
            return;
        }
        b.append('{');
        boolean first = true;
        for (Map.Entry<String, String> e : dimensions.entrySet()) {
            if (!first) {
                b.append(',');
            }
            b.append('"');
            AnalyticsJson.escape(b, e.getKey());
            b.append("\":");
            AnalyticsJson.appendValue(b, e.getValue());
            first = false;
        }
        b.append('}');
    }

    private String sessionId() {
        synchronized (lock) {
            if (sessionId == null) {
                Random r = new Random();
                char[] hex = "0123456789abcdef".toCharArray();
                StringBuilder s = new StringBuilder(16);
                for (int i = 0; i < 16; i++) {
                    s.append(hex[r.nextInt(16)]);
                }
                sessionId = s.toString();
            }
            return sessionId;
        }
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
