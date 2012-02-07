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
package com.codename1.impl.blackberry;

import net.rim.device.api.system.EventLogger;

/**
 *
 * @author Chen
 */
public class EventLog {

    public static final long GUID = 0x2051fd67b72d11L;
    public static final String APP_NAME = "CN1";
    private static EventLog instance = new EventLog();

    private EventLog() {
        EventLogger.register(GUID, APP_NAME, EventLogger.VIEWER_STRING);
    }

    public static EventLog getInstance() {
        return instance;
    }

    private void logEvent(String msg, int level) {
        EventLogger.logEvent(GUID, msg.getBytes(), level);
    }

    public void logDebugEvent(String msg) {
        logEvent(msg, EventLogger.DEBUG_INFO);
    }

    public void logInformationEvent(String msg) {
        logEvent(msg, EventLogger.INFORMATION);
    }

    public void logWarningEvent(String msg) {
        logEvent(msg, EventLogger.WARNING);
    }

    public void logErrorEvent(String msg) {
        logEvent(msg, EventLogger.ERROR);
    }

    public void logSevereErrorEvent(String msg) {
        logEvent(msg, EventLogger.SEVERE_ERROR);
    }

    public void logAlwaysEvent(String msg) {
        logEvent(msg, EventLogger.ALWAYS_LOG);
    }
}
