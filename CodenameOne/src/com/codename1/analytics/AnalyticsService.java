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
import com.codename1.io.Log;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/// The analytics service allows an application to report its usage, it is seamlessly
/// invoked by GUI builder applications if analytics is enabled for your application but can
/// work just as well for handcoded apps!
///
/// To enable analytics just use the `java.lang.String)`
/// method of the analytics service. For most typical usage you should also invoke the
/// `#setAppsMode(boolean)` method with `true`. If you are
/// not using the GUI builder invoke the visit method whenever you would like to log a
/// page view event.
///
/// @author Shai Almog
public class AnalyticsService {
    private static final Object INSTANCE_LOCK = new Object();
    private static AnalyticsService instance;
    private static boolean appsMode = true;
    private static boolean failSilently = true;
    private static int timeout;
    private static int readTimeout;
    private String agent;
    private String domain;
    private ConnectionRequest lastRequest;

    /// Indicates whether analytics server failures should brodcast an error event
    ///
    /// #### Returns
    ///
    /// the failSilently
    public static boolean isFailSilently() {
        return failSilently;
    }

    /// Indicates whether analytics server failures should brodcast an error event
    ///
    /// #### Parameters
    ///
    /// - `aFailSilently`: the failSilently to set
    public static void setFailSilently(boolean aFailSilently) {
        failSilently = aFailSilently;
    }

    /// Apps mode allows improved analytics using the newer google analytics API designed for apps
    ///
    /// #### Returns
    ///
    /// the appsMode
    public static boolean isAppsMode() {
        return appsMode;
    }

    /// Apps mode allows improved analytics using the newer google analytics API designed for apps.
    /// Most developers should invoke this method with `true`.
    ///
    /// #### Parameters
    ///
    /// - `aAppsMode`: the appsMode to set
    public static void setAppsMode(boolean aAppsMode) {
        appsMode = aAppsMode;
    }

    /// Sets timeout for HTTP requests to Google Analytics service.
    ///
    /// #### Parameters
    ///
    /// - `ms`: Milliseconds timeout.
    ///
    /// #### Since
    ///
    /// 7.0
    public static void setTimeout(int ms) {
        timeout = ms;
    }

    /// Sets read timeout for  HTTP requests to Google Analytics services.
    ///
    /// #### Parameters
    ///
    /// - `ms`: Milliseconds read timeout.
    ///
    /// #### Since
    ///
    /// 7.0
    public static void setReadTimeout(int ms) {
        readTimeout = ms;
    }

    /// Indicates whether analytics is enabled for this application
    ///
    /// #### Returns
    ///
    /// true if analytics is enabled
    public static boolean isEnabled() {
        return instance != null && instance.isAnalyticsEnabled();
    }

    /// Initializes google analytics for this application
    ///
    /// #### Parameters
    ///
    /// - `agent`: the google analytics tracking agent
    ///
    /// - `domain`: @param domain a domain to represent your application, commonly you should use your package name as a URL (e.g.
    ///               com.mycompany.myapp should become: myapp.mycompany.com)
    public static void init(String agent, String domain) {
        synchronized (INSTANCE_LOCK) {
            if (instance == null) {
                instance = new AnalyticsService();
            }
            instance.agent = agent;
            instance.domain = domain;
        }
    }

    /// Allows installing an analytics service other than the default
    ///
    /// #### Parameters
    ///
    /// - `i`: the analytics service implementation.
    public static void init(AnalyticsService i) {
        synchronized (INSTANCE_LOCK) {
            instance = i;
        }
    }

    /// Sends an asynchronous notice to the server regarding a page in the application being viewed, notice that
    /// you don't need to append the URL prefix to the page string.
    ///
    /// #### Parameters
    ///
    /// - `page`: the page viewed
    ///
    /// - `referer`: the source page
    public static void visit(String page, String referer) {
        instance.visitPage(page, referer);
    }

    /// In apps mode we can send information about an exception to the analytics server
    ///
    /// #### Parameters
    ///
    /// - `t`: the exception
    ///
    /// - `message`: up to 150 character message,
    ///
    /// - `fatal`: is the exception fatal
    public static void sendCrashReport(Throwable t, String message, boolean fatal) {
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#exception
        ConnectionRequest req = getGaRequest();
        req.addArgument("t", "exception");
        System.out.println(message);
        req.addArgument("exd", message.substring(0, Math.min(message.length(), 150) - 1));
        if (fatal) {
            req.addArgument("exf", "1");
        } else {
            req.addArgument("exf", "0");
        }

        NetworkManager.getInstance().addToQueue(req);
    }

    private static ConnectionRequest getGaRequest() {
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl("https://www.google-analytics.com/collect");
        req.setPost(true);
        req.setFailSilently(true);
        req.addArgument("v", "1");
        req.addArgument("tid", instance.agent);
        if (timeout > 0) {
            req.setTimeout(timeout);
        }
        if (readTimeout > 0) {
            req.setReadTimeout(readTimeout);
        }
        long uniqueId = Log.getUniqueDeviceId();
        req.addArgument("cid", String.valueOf(uniqueId));
        return req;
    }

    /// Indicates if the analytics is enabled, subclasses must override this method to process their information
    ///
    /// #### Returns
    ///
    /// true if analytics is enabled
    protected boolean isAnalyticsEnabled() {
        return agent != null;
    }

    /// Decorates the ConnectionRequest to be sent to the server before the request is sent.
    /// This can be overridden to add additional request parameters to the service, and hence provide
    /// additional analytics data.
    ///
    /// If using Google Analytics, the current you can see the available POST parameters that
    /// the server accepts [here](https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters).
    ///
    /// #### Parameters
    ///
    /// - `page`: The page visited
    ///
    /// - `referer`: The page from which the user came.
    ///
    /// - `request`: The ConnectionRequest
    ///
    /// #### Since
    ///
    /// 7.0
    protected void decorateVisitPageRequest(String page, String referer, ConnectionRequest request) {

    }

    /// Subclasses should override this method to track page visits
    ///
    /// #### Parameters
    ///
    /// - `page`: the page visited
    ///
    /// - `referer`: the page from which the user came
    protected void visitPage(String page, String referer) {
        if (lastRequest != null) {
            final String fPage = page;
            final String fReferer = referer;
            ActionListener onComplete = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    visitPage(fPage, fReferer);
                }
            };
            lastRequest.addResponseListener(onComplete);
            lastRequest.addResponseCodeListener(onComplete);
            lastRequest.addExceptionListener(onComplete);
            return;
        }
        if (appsMode) {
            // https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#apptracking
            final ConnectionRequest req = getGaRequest();
            req.addArgument("t", "appview");
            req.addArgument("an", Display.getInstance().getProperty("AppName", "Codename One App"));
            String version = Display.getInstance().getProperty("AppVersion", "1.0");
            req.addArgument("av", version);
            req.addArgument("cd", page);
            ActionListener onComplete = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (req == lastRequest) { //NOPMD CompareObjectsWithEquals
                        lastRequest = null;
                    }
                }
            };
            req.addResponseListener(onComplete);
            req.addResponseCodeListener(onComplete);
            req.addExceptionListener(onComplete);
            lastRequest = req;
            decorateVisitPageRequest(page, referer, req);
            NetworkManager.getInstance().addToQueue(req);
        } else {
            String url = Display.getInstance().getProperty("cloudServerURL", "https://codename-one.appspot.com/") + "anal";
            final ConnectionRequest r = new ConnectionRequest();
            r.setUrl(url);
            r.setPost(false);
            r.setFailSilently(failSilently);
            r.addArgument("guid", "ON");
            r.addArgument("utmac", instance.agent);
            r.addArgument("utmn", Integer.toString((int) (System.currentTimeMillis() % 0x7fffffff)));
            if (page == null || page.length() == 0) {
                page = "-";
            }
            r.addArgument("utmp", page);
            if (referer == null || referer.length() == 0) {
                referer = "-";
            }
            r.addArgument("utmr", referer);
            r.addArgument("d", instance.domain);
            r.setPriority(ConnectionRequest.PRIORITY_LOW);
            if (timeout > 0) {
                r.setTimeout(timeout);
            }
            if (readTimeout > 0) {
                r.setReadTimeout(readTimeout);
            }
            ActionListener onComplete = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (r == lastRequest) { //NOPMD CompareObjectsWithEquals
                        lastRequest = null;
                    }
                }
            };
            r.addResponseListener(onComplete);
            r.addResponseCodeListener(onComplete);
            r.addExceptionListener(onComplete);
            lastRequest = r;
            decorateVisitPageRequest(page, referer, r);
            NetworkManager.getInstance().addToQueue(r);
        }
    }
}
