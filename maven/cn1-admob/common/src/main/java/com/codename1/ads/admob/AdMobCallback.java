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
package com.codename1.ads.admob;

/// Static entry point the native layer calls to report ad events back to Java.
/// Keeping a single static fan-in method keeps the per-platform native binding
/// minimal: Android calls these methods directly, iOS invokes the generated C
/// functions. Events are routed to the right session by [AdMobProvider] using
/// the integer handle.
public class AdMobCallback {
    /// Event: the ad finished loading.
    public static final int LOADED = 1;
    /// Event: the ad failed to load. `code`/`message` describe the failure.
    public static final int FAILED = 2;
    /// Event: the ad was presented.
    public static final int SHOWN = 3;
    /// Event: the ad failed to present.
    public static final int SHOW_FAILED = 4;
    /// Event: the ad was dismissed.
    public static final int DISMISSED = 5;
    /// Event: an impression was recorded.
    public static final int IMPRESSION = 6;
    /// Event: the ad was clicked.
    public static final int CLICKED = 7;
    /// Event: the user earned a reward. `rewardType`/`rewardAmount` carry the reward.
    public static final int REWARD = 8;
    /// Event: a consent request completed. `code` carries the resulting status.
    public static final int CONSENT_COMPLETE = 9;

    private AdMobCallback() {
    }

    /// Reports an ad event from the native layer. May be invoked on any thread.
    ///
    /// #### Parameters
    ///
    /// - `handle`: the ad handle, or 0 for consent events
    /// - `event`: one of the event constants above
    /// - `code`: provider error code or consent status, depending on `event`
    /// - `message`: human readable failure description, may be null
    /// - `rewardType`: reward type for [#REWARD], otherwise null
    /// - `rewardAmount`: reward amount for [#REWARD], otherwise 0
    public static void fire(int handle, int event, int code, String message,
                            String rewardType, int rewardAmount) {
        AdMobProvider.dispatch(handle, event, code, message, rewardType, rewardAmount);
    }
}
