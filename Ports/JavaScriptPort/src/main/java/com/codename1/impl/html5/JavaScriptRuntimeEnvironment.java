/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
