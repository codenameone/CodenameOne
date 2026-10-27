/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.net;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Preferences;
import com.codename1.io.RequestAuthorizer;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;

/// This phone as the server knows it, said in headers on every API request.
///
/// `X-Reel-Device` is an id made once and kept: streams, downloads and the
/// account's device list count by it, and signing a device out ends the
/// sign-in it last used. `X-Reel-Profile-Key` is the key the server gave when
/// the watching profile was unlocked; a PIN-protected profile answers nothing
/// without it.
///
/// The headers ride on the request authorizer, which the network layer asks
/// about every request to the API and which hands it the request.
public final class Device implements RequestAuthorizer.Proactive {
    private static final String ID_KEY = "reel.device.id";
    private static String profileKey = "";
    private final RequestAuthorizer.Proactive tokens;

    Device(RequestAuthorizer.Proactive tokens) {
        this.tokens = tokens;
    }

    /// This phone's id, made on first use.
    public static String id() {
        String id = Preferences.get(ID_KEY, "");
        if (id.length() == 0) {
            StringBuilder b = new StringBuilder("d");
            String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
            java.util.Random r = new java.util.Random();
            for (int iter = 0; iter < 24; iter++) {
                b.append(alphabet.charAt(r.nextInt(alphabet.length())));
            }
            id = b.toString();
            Preferences.set(ID_KEY, id);
        }
        return id;
    }

    /// A name a person recognizes in the device list.
    public static String name() {
        String platform = CN.getPlatformName();
        return platform == null || platform.length() == 0 ? "Phone" : "Reel on " + platform;
    }

    /// The key of the profile watching, or empty. Kept in memory only: the
    /// next launch asks for the PIN again.
    public static void setProfileKey(String key) {
        profileKey = key == null ? "" : key;
    }

    public static String profileKey() {
        return profileKey;
    }

    private static void stamp(ConnectionRequest request) {
        request.addRequestHeader("X-Reel-Device", id());
        request.addRequestHeader("X-Reel-Device-Name", name());
        request.addRequestHeader("X-Reel-Platform", CN.getPlatformName());
        if (profileKey.length() > 0) {
            request.addRequestHeader("X-Reel-Profile-Key", profileKey);
        }
    }

    @Override
    public String getAuthorization(ConnectionRequest request) {
        stamp(request);
        return tokens.getAuthorization(request);
    }

    @Override
    public AsyncResource<Boolean> refreshAuthorization(ConnectionRequest request, String rejectedAuthorization) {
        return tokens.refreshAuthorization(request, rejectedAuthorization);
    }

    @Override
    public AsyncResource<Boolean> prepareAuthorization(ConnectionRequest request) {
        return tokens.prepareAuthorization(request);
    }
}
