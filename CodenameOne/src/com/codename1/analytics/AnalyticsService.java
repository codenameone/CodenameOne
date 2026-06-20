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
package com.codename1.analytics;

import com.codename1.io.ConnectionRequest;

/// The legacy analytics entry point, retained for backward compatibility. New
/// code should use the {@link Analytics} facade together with one or more
/// {@link AnalyticsProvider} implementations, which adds a generic provider SPI,
/// GDPR/CCPA consent handling and modern backends (GA4, Matomo, Firebase and
/// the first-party Codename One service).
///
/// This class now delegates to {@link Analytics}: {@link #init(String, String)}
/// registers a {@link GoogleAnalyticsProvider} (Google Analytics 4, replacing
/// the retired Measurement Protocol v1 that this class used to target) and
/// {@link #visit(String, String)} / {@link #sendCrashReport(Throwable, String,
/// boolean)} route through the facade.
///
/// To preserve the historical "always on" behaviour of this deprecated API --
/// which predates the consent model -- {@link #init(String, String)} and
/// {@link #init(AnalyticsService)} switch the facade to
/// {@link ConsentMode#OPT_OUT}. Applications that need opt-in / GDPR behaviour
/// should migrate to the {@link Analytics} API and call
/// {@link Analytics#setConsent(AnalyticsConsent)}.
///
/// @author Shai Almog
/// @deprecated use {@link Analytics} and an {@link AnalyticsProvider}
@Deprecated
public class AnalyticsService {
    private static final Object INSTANCE_LOCK = new Object();
    private static AnalyticsService instance;
    private static boolean appsMode = true;
    private static boolean failSilently = true;
    private static int timeout;
    private static int readTimeout;
    private String agent;
    private String domain;

    /// Indicates whether analytics server failures should brodcast an error event
    ///
    /// #### Returns
    ///
    /// the failSilently
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static boolean isFailSilently() {
        return failSilently;
    }

    /// Indicates whether analytics server failures should brodcast an error event
    ///
    /// #### Parameters
    ///
    /// - `aFailSilently`: the failSilently to set
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static void setFailSilently(boolean aFailSilently) {
        failSilently = aFailSilently;
    }

    /// Apps mode allows improved analytics using the newer google analytics API designed for apps
    ///
    /// #### Returns
    ///
    /// the appsMode
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static boolean isAppsMode() {
        return appsMode;
    }

    /// Apps mode allows improved analytics using the newer google analytics API designed for apps.
    /// This setting is retained for source compatibility but no longer affects behaviour; the
    /// modern GA4 protocol is always used.
    ///
    /// #### Parameters
    ///
    /// - `aAppsMode`: the appsMode to set
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static void setAppsMode(boolean aAppsMode) {
        appsMode = aAppsMode;
    }

    /// Retained for source compatibility; no longer affects behaviour.
    ///
    /// #### Parameters
    ///
    /// - `ms`: Milliseconds timeout.
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static void setTimeout(int ms) {
        timeout = ms;
    }

    /// Retained for source compatibility; no longer affects behaviour.
    ///
    /// #### Parameters
    ///
    /// - `ms`: Milliseconds read timeout.
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static void setReadTimeout(int ms) {
        readTimeout = ms;
    }

    /// Indicates whether analytics is enabled for this application
    ///
    /// #### Returns
    ///
    /// true if analytics is enabled
    ///
    /// @deprecated use {@link Analytics}
    @Deprecated
    public static boolean isEnabled() {
        return instance != null && instance.isAnalyticsEnabled();
    }

    /// Initializes analytics for this application using the modern Google
    /// Analytics 4 protocol. The `agent` is used as the GA4 measurement id.
    ///
    /// #### Parameters
    ///
    /// - `agent`: the google analytics tracking agent / measurement id
    ///
    /// - `domain`: a domain to represent your application, commonly your package name as a URL
    /// (e.g. com.mycompany.myapp should become: myapp.mycompany.com)
    ///
    /// @deprecated use {@link Analytics} with a {@link GoogleAnalyticsProvider}
    @Deprecated
    public static void init(String agent, String domain) {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new AnalyticsService();
            }
            instance.agent = agent;
            instance.domain = domain;
            Analytics.setConsentMode(ConsentMode.OPT_OUT);
            Analytics.clearProviders();
            Analytics.addProvider(new GoogleAnalyticsProvider(agent, ""));
        }
    }

    /// Allows installing an analytics service other than the default. The custom
    /// implementation's {@code visitPage} hook is invoked for page views.
    ///
    /// #### Parameters
    ///
    /// - `i`: the analytics service implementation.
    ///
    /// @deprecated use {@link Analytics} and a custom {@link AnalyticsProvider}
    @Deprecated
    public static void init(AnalyticsService i) {
        synchronized (INSTANCE_LOCK) {
            instance = i;
            Analytics.setConsentMode(ConsentMode.OPT_OUT);
        }
    }

    /// Sends an asynchronous notice to the server regarding a page in the application being viewed,
    /// notice that you don't need to append the URL prefix to the page string.
    ///
    /// #### Parameters
    ///
    /// - `page`: the page viewed
    ///
    /// - `referer`: the source page
    ///
    /// @deprecated use {@link Analytics#screen(String, String)}
    @Deprecated
    public static void visit(String page, String referer) {
        if (instance != null) {
            instance.visitPage(page, referer);
        }
    }

    /// Reports information about an exception to the analytics server.
    ///
    /// #### Parameters
    ///
    /// - `t`: the exception
    ///
    /// - `message`: up to 150 character message,
    ///
    /// - `fatal`: is the exception fatal
    ///
    /// @deprecated use {@link Analytics#crash(Throwable, String, boolean)}
    @Deprecated
    public static void sendCrashReport(Throwable t, String message, boolean fatal) {
        Analytics.crash(t, message, fatal);
    }

    /// Indicates if the analytics is enabled, subclasses must override this method to process their information
    ///
    /// #### Returns
    ///
    /// true if analytics is enabled
    protected boolean isAnalyticsEnabled() {
        return agent != null || !Analytics.getProviders().isEmpty();
    }

    /// Retained for source compatibility with subclasses written against the
    /// previous Google Analytics v1 implementation. It is no longer invoked by
    /// the default page-view path, which now delegates to {@link Analytics}.
    ///
    /// #### Parameters
    ///
    /// - `page`: The page visited
    ///
    /// - `referer`: The page from which the user came.
    ///
    /// - `request`: The ConnectionRequest
    ///
    /// @deprecated decorate the request inside a custom {@link AnalyticsProvider}
    @Deprecated
    protected void decorateVisitPageRequest(String page, String referer, ConnectionRequest request) {
    }

    /// Subclasses may override this method to track page visits. The default
    /// implementation routes the visit to the {@link Analytics} facade.
    ///
    /// #### Parameters
    ///
    /// - `page`: the page visited
    ///
    /// - `referer`: the page from which the user came
    protected void visitPage(String page, String referer) {
        Analytics.screen(page, referer);
    }
}
