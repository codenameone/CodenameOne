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
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import java.io.IOException;
import java.io.InputStream;

/**
 * The analytics service allows an application to report its usage, it is seamlessly
 * invoked by GUI builder applications if analytics is enabled for your application!
 * To enable analytics just use the init() method of the analytics service. If you are 
 * not using the GUI builder invoke the visit method whenever you would like to log a 
 * page view event.
 * 
 *
 * @author Shai Almog
 */
public class AnalyticsService {
    private static AnalyticsService instance;

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
    private String agent;
    private String domain;
    private static boolean failSilently = true;
    
    /**
     * Indicates whether analytics is enabled for this application
     * 
     * @return true if analytics is enabled
     */
    public static boolean isEnabled() {
        return instance != null && instance.agent != null;
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
     * Sends an asynchronous notice to the server regarding a page in the application being viewed, notice that
     * you don't need to append the URL prefix to the page string.
     * 
     * @param page the page viewed
     * @param referer the source page
     */
    public static void visit(String page, String referer) {
        String url = "https://codename-one.appspot.com/anal";
        ConnectionRequest r = new ConnectionRequest();
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
        NetworkManager.getInstance().addToQueue(r);
    }
}
