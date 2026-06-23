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

import java.util.Map;

/// A privacy-first provider targeting Matomo (formerly Piwik)
/// through its HTTP Tracking API. Matomo can be self-hosted and offers IP
/// anonymisation and consent-aware tracking, which makes it a natural fit for
/// GDPR-sensitive deployments. The same SPI shape applies to comparable
/// privacy-focused backends (PostHog, Plausible), so swapping is trivial.
///
/// ```java
/// Analytics.addProvider(new MatomoAnalyticsProvider("https://matomo.example.com", 1));
/// ```
///
/// Screen views map to Matomo actions; events use Matomo's `e_c`/`e_a`/`e_n`
/// event parameters; crashes are reported as an event in the `crash` category.
/// The pseudonymous client id is truncated to Matomo's 16-character visitor id.
public class MatomoAnalyticsProvider extends AbstractAnalyticsProvider {
    private final String trackerUrl;
    private final String idSite;
    private String userId;

    /// Creates a Matomo provider.
    ///
    /// #### Parameters
    ///
    /// - `matomoBaseUrl`: the Matomo base URL or full `matomo.php` tracker URL
    ///
    /// - `idSite`: the Matomo site id
    public MatomoAnalyticsProvider(String matomoBaseUrl, int idSite) {
        this.trackerUrl = normalize(matomoBaseUrl);
        this.idSite = Integer.toString(idSite);
    }

    @Override
    public String getName() {
        return "matomo";
    }

    @Override
    public void setUserId(String id) {
        this.userId = id;
    }

    @Override
    public void trackScreen(String name, String referrer) {
        ConnectionRequest r = base();
        String safeName = name == null ? "" : name;
        r.addArgument("action_name", safeName);
        r.addArgument("url", pseudoUrl(safeName));
        if (referrer != null && referrer.length() > 0) {
            r.addArgument("urlref", pseudoUrl(referrer));
        }
        NetworkManager.getInstance().addToQueue(r);
    }

    @Override
    public void trackEvent(AnalyticsEvent event) {
        ConnectionRequest r = base();
        r.addArgument("url", pseudoUrl(event.getName()));
        r.addArgument("e_c", event.getCategory() == null ? "event" : event.getCategory());
        r.addArgument("e_a", event.getName());
        Map<String, Object> params = event.getParameters();
        Object label = params.get("label");
        if (label != null) {
            r.addArgument("e_n", label.toString());
        }
        Object value = params.get("value");
        if (value instanceof Number) {
            r.addArgument("e_v", value.toString());
        }
        NetworkManager.getInstance().addToQueue(r);
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
        ConnectionRequest r = base();
        r.addArgument("url", pseudoUrl("crash"));
        r.addArgument("e_c", "crash");
        r.addArgument("e_a", report.isFatal() ? "fatal" : "handled");
        r.addArgument("e_n", description);
        NetworkManager.getInstance().addToQueue(r);
    }

    @Override
    public boolean supports(AnalyticsCapability capability) {
        return capability == AnalyticsCapability.SCREEN_VIEWS
                || capability == AnalyticsCapability.EVENTS
                || capability == AnalyticsCapability.CRASH_REPORTING
                || capability == AnalyticsCapability.REAL_TIME;
    }

    private ConnectionRequest base() {
        ConnectionRequest r = new ConnectionRequest();
        r.setUrl(trackerUrl);
        r.setPost(false);
        r.setFailSilently(true);
        r.addArgument("idsite", idSite);
        r.addArgument("rec", "1");
        r.addArgument("apiv", "1");
        r.addArgument("send_image", "0");
        r.addArgument("_id", visitorId());
        r.addArgument("rand", Long.toString(System.currentTimeMillis()));
        if (userId != null) {
            r.addArgument("uid", userId);
        }
        return r;
    }

    private String visitorId() {
        AnalyticsContext ctx = getContext();
        String c = ctx == null || ctx.getClientId() == null ? "" : ctx.getClientId();
        if (c.length() >= 16) {
            return c.substring(0, 16);
        }
        StringBuilder b = new StringBuilder(c);
        while (b.length() < 16) {
            b.append('0');
        }
        return b.toString();
    }

    private String pseudoUrl(String path) {
        String host = "app";
        AnalyticsContext ctx = getContext();
        if (ctx != null && ctx.getAppName() != null && ctx.getAppName().length() > 0) {
            host = ctx.getAppName();
        }
        return "https://" + host + "/" + (path == null ? "" : path);
    }

    private static String normalize(String base) {
        if (base == null) {
            return "";
        }
        if (base.endsWith("/matomo.php") || base.endsWith("/piwik.php")) {
            return base;
        }
        if (base.endsWith("/")) {
            return base + "matomo.php";
        }
        return base + "/matomo.php";
    }
}
