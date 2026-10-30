/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.installreferrer;

/** android_play_install_referrer's {@code ReferrerDetails}. */
public class ReferrerDetails {
    private final String installReferrer;
    private final long referrerClickTimestampSeconds;
    private final long installBeginTimestampSeconds;
    private final long referrerClickTimestampServerSeconds;
    private final long installBeginTimestampServerSeconds;
    private final String installVersion;
    private final boolean googlePlayInstantParam;

    public ReferrerDetails(String installReferrer, long referrerClickTimestampSeconds,
            long installBeginTimestampSeconds, long referrerClickTimestampServerSeconds,
            long installBeginTimestampServerSeconds, String installVersion, boolean googlePlayInstantParam) {
        this.installReferrer = installReferrer;
        this.referrerClickTimestampSeconds = referrerClickTimestampSeconds;
        this.installBeginTimestampSeconds = installBeginTimestampSeconds;
        this.referrerClickTimestampServerSeconds = referrerClickTimestampServerSeconds;
        this.installBeginTimestampServerSeconds = installBeginTimestampServerSeconds;
        this.installVersion = installVersion;
        this.googlePlayInstantParam = googlePlayInstantParam;
    }

    public String installReferrer() {
        return installReferrer;
    }

    public long referrerClickTimestampSeconds() {
        return referrerClickTimestampSeconds;
    }

    public long installBeginTimestampSeconds() {
        return installBeginTimestampSeconds;
    }

    public long referrerClickTimestampServerSeconds() {
        return referrerClickTimestampServerSeconds;
    }

    public long installBeginTimestampServerSeconds() {
        return installBeginTimestampServerSeconds;
    }

    public String installVersion() {
        return installVersion;
    }

    public boolean googlePlayInstantParam() {
        return googlePlayInstantParam;
    }
}
