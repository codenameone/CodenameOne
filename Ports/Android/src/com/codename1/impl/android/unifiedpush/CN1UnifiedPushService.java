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
package com.codename1.impl.android.unifiedpush;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.android.AndroidImplementation;
import com.codename1.impl.android.StubUtil;
import com.codename1.io.Preferences;
import com.codename1.push.PushCallback;
import com.codename1.ui.Display;
import org.json.JSONException;
import org.json.JSONObject;
import org.unifiedpush.android.connector.FailedReason;
import org.unifiedpush.android.connector.PushService;
import org.unifiedpush.android.connector.data.PublicKeySet;
import org.unifiedpush.android.connector.data.PushEndpoint;
import org.unifiedpush.android.connector.data.PushMessage;

/// Receives the UnifiedPush endpoint and messages from the distributor.
///
/// Declared in the manifest by the builder with the connector's `PUSH_EVENT`
/// action. See [UnifiedPushSupport] for why the endpoint is registered as a Web
/// Push subscription.
public class CN1UnifiedPushService extends PushService {
    /// The prefix of a Web Push device key; `PushClient` reads the provider from it.
    static final String WEB_KEY_PREFIX = "cn1-web-";

    @Override
    public void onNewEndpoint(PushEndpoint endpoint, String instance) {
        PublicKeySet keys = endpoint.getPubKeySet();
        if (keys == null) {
            // Only a distributor older than UnifiedPush 2 answers without keys, and
            // the push service encrypts every message: it could deliver nothing.
            UnifiedPushSupport.fail("The UnifiedPush distributor does not support Web Push "
                    + "encryption; update it");
            return;
        }
        final String deviceKey;
        try {
            deviceKey = deviceKey(endpoint.getUrl(), keys.getPubKey(), keys.getAuth());
        } catch (JSONException err) {
            UnifiedPushSupport.fail("UnifiedPush registration failed: " + err);
            return;
        }
        if (!Display.isInitialized()) {
            // A distributor can rotate the endpoint while the application is not
            // running. Nothing can be stored or reported without the Codename One
            // context; the application registers again when it next starts, and the
            // distributor answers that with the current endpoint.
            return;
        }
        Preferences.set("push_key", deviceKey);
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                PushCallback callback = CodenameOneImplementation.getPushCallback();
                if (callback != null) {
                    callback.registeredForPush(deviceKey);
                }
            }
        });
    }

    /// The Web Push device key for a subscription: `cn1-web-` and the base64url
    /// form of the subscription JSON a browser's `PushSubscription.toJSON()` gives.
    static String deviceKey(String url, String p256dh, String auth) throws JSONException {
        JSONObject keys = new JSONObject();
        keys.put("p256dh", p256dh);
        keys.put("auth", auth);
        JSONObject subscription = new JSONObject();
        subscription.put("endpoint", url);
        subscription.put("keys", keys);
        byte[] json;
        try {
            json = subscription.toString().getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new JSONException(impossible.toString());
        }
        return WEB_KEY_PREFIX + android.util.Base64.encodeToString(json,
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP
                        | android.util.Base64.NO_PADDING);
    }

    @Override
    public void onMessage(PushMessage message, String instance) {
        if (!message.getDecrypted()) {
            // The push service encrypts every message. One that arrives in clear
            // was not sent by it, so it is dropped rather than shown.
            com.codename1.io.Log.p("Dropped an unencrypted UnifiedPush message");
            return;
        }
        String envelope;
        try {
            envelope = new String(message.getContent(), "UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            return;
        }
        if (envelope.length() > 0) {
            AndroidImplementation.handleV3Push(envelope, this, StubUtil.appIsRunning(),
                    StubUtil.getAppStubClass());
        }
    }

    @Override
    public void onRegistrationFailed(FailedReason reason, String instance) {
        UnifiedPushSupport.fail("UnifiedPush registration failed: " + reason);
    }

    @Override
    public void onUnregistered(String instance) {
        if (Display.isInitialized()) {
            String key = Preferences.get("push_key", null);
            if (key != null && key.startsWith(WEB_KEY_PREFIX)) {
                Preferences.delete("push_key");
            }
        }
    }
}
