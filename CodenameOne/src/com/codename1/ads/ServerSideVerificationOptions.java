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
package com.codename1.ads;

/// Options for server side verification (SSV) of rewarded ads. When set on a
/// [RewardedAd] or [RewardedInterstitialAd] before it is shown, the ad network
/// posts a signed callback to your server so the reward can be granted securely
/// rather than trusting the client.
///
/// The `customData` and `userId` are forwarded verbatim to your verification
/// endpoint, letting you correlate the callback with the user and context.
///
/// @author Shai Almog
public class ServerSideVerificationOptions {
    private final String userId;
    private final String customData;

    /// Creates a new set of server side verification options.
    ///
    /// #### Parameters
    ///
    /// - `userId`: the user identifier forwarded to the verification callback, may be null
    /// - `customData`: an opaque string forwarded to the verification callback, may be null
    public ServerSideVerificationOptions(String userId, String customData) {
        this.userId = userId;
        this.customData = customData;
    }

    /// The user identifier forwarded to the verification callback, may be null.
    public String getUserId() {
        return userId;
    }

    /// An opaque string forwarded to the verification callback, may be null.
    public String getCustomData() {
        return customData;
    }
}
