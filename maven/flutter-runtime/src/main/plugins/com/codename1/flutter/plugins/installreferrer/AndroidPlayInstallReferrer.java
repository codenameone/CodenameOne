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

import dart.async.Future;

/**
 * android_play_install_referrer's {@code AndroidPlayInstallReferrer}. The referrer comes
 * from the Play Install Referrer service, which Codename One's Android port does not bind,
 * so every platform answers what the plugin answers where the service is unavailable: a
 * failed future. An app that awaits it inside a try keeps working, with no attribution.
 */
public final class AndroidPlayInstallReferrer {

    private AndroidPlayInstallReferrer() {
    }

    /** {@code AndroidPlayInstallReferrer.installReferrer}, read as a static field. */
    public static final Future<ReferrerDetails> installReferrer = Future.error(new dart.core.UnsupportedError(
            "android_play_install_referrer: the Play Install Referrer service is not available"));
}
