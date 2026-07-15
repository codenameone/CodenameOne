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
package com.codename1.impl.html5;

public final class JavaScriptRuntimeEnvironment {
    private final String platform;
    private final String userAgent;
    private final String language;
    private final String appName;
    private final String appCodeName;
    private final String appVersion;
    private final String deploymentType;
    private final String locationHref;

    public JavaScriptRuntimeEnvironment(String platform, String userAgent, String language, String appName,
                                        String appCodeName, String appVersion, String deploymentType, String locationHref) {
        this.platform = platform;
        this.userAgent = userAgent;
        this.language = language;
        this.appName = appName;
        this.appCodeName = appCodeName;
        this.appVersion = appVersion;
        this.deploymentType = deploymentType;
        this.locationHref = locationHref;
    }

    public String getPlatform() {
        return platform;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getLanguage() {
        return language;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppCodeName() {
        return appCodeName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDeploymentType() {
        return deploymentType;
    }

    public String getLocationHref() {
        return locationHref;
    }
}
