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

/**
 * <p>The analytics service allows an application to report its usage, it is seamlessly
 * invoked by GUI builder applications if analytics is enabled for your application but can
 * work just as well for handcoded apps!</p>
 * <p>To enable analytics just use the {@link #init(java.lang.String, java.lang.String)} 
 * method of the analytics service. For most typical usage you should also invoke the
 * {@link #setAppsMode(boolean)} method with {@code true}. If you are 
 * not using the GUI builder invoke the visit method whenever you would like to log a 
 * page view event.</p>
 * 
 *
 * @author Shai Almog
 */
public class AnalyticsService {
    private static AnalyticsService instance;

    private static boolean appsMode = true;
    
    /**
     * Indicates whether analytics server failures should brodcast an error event
     * @return the failSilently
     */
    public static boolean isFailSilently() {
        return failSilently;
    }

    /**
     * Indicates whether analytics server failures should brodcast an error event
     * @param aFailSilently the failSilently to set
     */
    public static void setFailSilently(boolean aFailSilently) {
        failSilently = aFailSilently;
    }

    /**
     * Apps mode allows improved analytics using the newer google analytics API designed for apps
     * @return the appsMode
     */
    public static boolean isAppsMode() {
        return appsMode;
    }
    
    /**
     * Sets timeout for HTTP requests to Google Analytics service.
     * @param ms Milliseconds timeout.
     * @since 7.0
     */
    public static void setTimeout(int ms) {
        timeout = ms;
    }
    
    /**
     * Sets read timeout for  HTTP requests to Google Analytics services.
     * @param ms Milliseconds read timeout.
     * @since 7.0
     */
    public static void setReadTimeout(int ms) {
        readTimeout = ms;
    }

    /**
     * Apps mode allows improved analytics using the newer google analytics API designed for apps.
     * Most developers should invoke this method with {@code true}.
     * @param aAppsMode the appsMode to set
     */
    public static void setAppsMode(boolean aAppsMode) {
        appsMode = aAppsMode;
    }
    
    private String agent;
    private String domain;
    private static boolean failSilently = true;
    private ConnectionRequest lastRequest;
    private static int timeout;
    private static int readTimeout;
    
    /**
     * Indicates whether analytics is enabled for this application
     * 
     * @return true if analytics is enabled
     */
    public static boolean isEnabled() {
        return instance != null && instance.isAnalyticsEnabled();
    }
    
    /**
     * Indicates if the analytics is enabled, subclasses must override this method to process their information
     * @return true if analytics is enabled
     */
    protected boolean isAnalyticsEnabled() {
        return agent != null;
    }
    
    /**
     * Initializes google analytics for this application
     * 
     * @param agent the google analytics tracking agent
     * @param domain a domain to represent your application, commonly you should use your package name as a URL (e.g. 
     * com.mycompany.myapp should become: myapp.mycompany.com)
     */
    public static void init(String agent, String domain) {
        if(instance == null) {
            instance = new AnalyticsService();
        }
        instance.agent = agent;
        instance.domain = domain;
    }
    
    /**
     * Allows installing an analytics service other than the default
     * @param i the analytics service implementation.
     */
    public static void init(AnalyticsService i) {
        instance = i;
    }
    
    /**
     * Sends an asynchronous notice to the server regarding a page in the application being viewed, notice that
     * you don't need to append the URL prefix to the page string.
     * 
     * @param page the page viewed
     * @param referer the source page
     */
    public static void visit(String page, String referer) {
        instance.visitPage(page, referer);
    }
    
    /**
     * Decorates the ConnectionRequest to be sent to the server before the request is sent.
     * This can be overridden to add additional request parameters to the service, and hence provide
     * additional analytics data.
     * 
     * <p>If using Google Analytics, the current you can see the available POST parameters that
     * the server accepts <a href="https://developers.google.com/analytics/devguides/collection/protocol/v1/parameters">here</a>.</p>
     * 
     * @param page The page visited
     * @param referer The page from which the user came.
     * @param request The ConnectionRequest
     * @since 7.0
     */
    protected void decorateVisitPageRequest(String page, String referer, ConnectionRequest request) {
        
    }
    
    /**
     * Subclasses should override this method to track page visits
     * @param page the page visited
     * @param referer the page from which the user came
     */
    protected void visitPage(String page, String referer) {
        if (lastRequest != null) {
            final String fPage = page;
            final String fReferer = referer;
            ActionListener onComplete = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    visitPage(fPage, fReferer);
                }
            };
            lastRequest.addResponseListener(onComplete);
            lastRequest.addResponseCodeListener(onComplete);
            lastRequest.addExceptionListener(onComplete);
            return;
        }
        if(appsMode) {
            // https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#apptracking
            final ConnectionRequest req = GetGARequest();
            req.addArgument("t", "appview");
            req.addArgument("an", Display.getInstance().getProperty("AppName", "Codename One App"));
            String version = Display.getInstance().getProperty("AppVersion", "1.0");
            req.addArgument("av", version);
            req.addArgument("cd", page);
            ActionListener onComplete = new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    if (req == lastRequest) {
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
            if(page == null || page.length() == 0) {
                page = "-";
            }
            r.addArgument("utmp", page);
            if(referer == null || referer.length() == 0) {
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
                public void actionPerformed(ActionEvent evt) {
                    if (r == lastRequest) {
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
    
    /**
     * In apps mode we can send information about an exception to the analytics server
     * @param t the exception
     * @param message up to 150 character message, 
     * @param fatal is the exception fatal
     */
    public static void sendCrashReport(Throwable t, String message, boolean fatal) {
        // https://developers.google.com/analytics/devguides/collection/protocol/v1/devguide#exception
        ConnectionRequest req = GetGARequest();
        req.addArgument("t", "exception");
        System.out.println(message);
        req.addArgument("exd", message.substring(0, Math.min(message.length(), 150) - 1));
        if(fatal) {
            req.addArgument("exf", "1");
        } else {
            req.addArgument("exf", "0");
        }
        
        NetworkManager.getInstance().addToQueue(req);
    }

    private static ConnectionRequest GetGARequest() {
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
}
