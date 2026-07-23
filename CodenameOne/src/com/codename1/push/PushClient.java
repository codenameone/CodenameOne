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
package com.codename1.push;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.Log;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;
import com.codename1.ui.Display;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.LiveActivity;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// Explicit v3 client binding. The application main class does not implement `PushCallback`.
public final class PushClient {
    private static PushClient active;

    private final String appId;
    private final PushListener listener;
    private final PushRegistrationSink registrationSink;
    private final PushTransport transport;
    private PushSubscription subscription;
    private final PushCallback compatibilityCallback = new CompatibilityCallback();

    private PushClient(Builder builder) {
        appId = builder.appId;
        listener = builder.listener;
        registrationSink = builder.registrationSink;
        transport = builder.transport;
    }

    public static Builder builder(String appId) { return new Builder(appId); }

    public void register() {
        active = this;
        CodenameOneImplementation.setPushCallback(compatibilityCallback);
        if (transport == null) {
            Display.getInstance().registerPush();
        } else if (!transport.isSupported()) {
            fireError(new PushError("unsupported_transport", transport.getId() + " is unavailable", false));
        } else {
            transport.register(new TransportCallback());
        }
    }

    public void unregister() {
        if (transport == null) {
            Display.getInstance().deregisterPush();
            notifyUnregistered();
        } else {
            transport.unregister(new TransportCallback());
        }
    }

    public String getAppId() { return appId; }
    public PushSubscription getSubscription() { return subscription; }

    /// Used by generated native bootstraps to find the explicit callback binding.
    public static PushCallback getActiveCallback() {
        return active == null ? null : active.compatibilityCallback;
    }

    /// Native/custom transport entry point for an encoded v3 envelope.
    public static void dispatch(String envelopeJson) {
        if (active != null) active.receive(envelopeJson);
    }

    private void registered(final PushSubscription value) {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() { registered(value); }
            });
            return;
        }
        subscription = value;
        if (registrationSink != null) {
            registrationSink.registered(value);
        } else if (transport == null) {
            registerManaged(value);
        }
        if (listener != null) listener.onRegistration(value);
    }

    private void receive(final String json) {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() { receive(json); }
            });
            return;
        }
        try {
            PushMessage message = PushMessage.parse(json);
            applySurface(message);
            if (listener != null) listener.onMessage(message);
        } catch (IOException ex) {
            Log.e(ex);
            fireError(new PushError("invalid_envelope", ex.getMessage(), false));
        }
    }

    private static void applySurface(PushMessage message) {
        java.util.Map<String, Object> surface = message.getSurface();
        if (surface.isEmpty()) return;
        String operation = value(surface.get("operation"));
        if ("widget".equals(operation)) {
            Surfaces.publishRemote(value(surface.get("kind")), value(surface.get("timeline")));
        } else if ("live-update".equals(operation)) {
            LiveActivity.updateRemote(value(surface.get("id")), value(surface.get("state")));
        } else if ("live-end".equals(operation)) {
            LiveActivity.endRemote(value(surface.get("id")), value(surface.get("state")),
                    Boolean.TRUE.equals(surface.get("dismissImmediately")));
        }
    }

    private static String value(Object value) { return value == null ? null : String.valueOf(value); }

    private void notifyUnregistered() {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() { notifyUnregistered(); }
            });
            return;
        }
        if (subscription != null && registrationSink != null) {
            registrationSink.unregistered(subscription);
        } else if (subscription != null && transport == null) {
            unregisterManaged();
        }
        subscription = null;
        if (active == this) {
            active = null;
            CodenameOneImplementation.setPushCallback(null);
        }
    }

    private void registerManaged(PushSubscription value) {
        final Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("provider", value.getTransportId());
        body.put("token", value.getToken());
        body.put("installationId", installationId());
        ConnectionRequest request = new ConnectionRequest() {
            protected void postResponse() {
                try {
                    Map<String, Object> response = JSONParser.parseJSON(getResponseData());
                    Object id = response.get("id");
                    if (id != null) Preferences.set("push_v3_subscription", String.valueOf(id));
                } catch (IOException ex) {
                    fireError(new PushError("registration_response", ex.getMessage(), false));
                }
            }
            protected void handleErrorResponseCode(int code, String message) {
                fireError(new PushError(code == 402 ? "upgrade_required" : "managed_registration",
                        message == null ? "BuildCloud registration failed" : message, code >= 500));
            }
        };
        request.setUrl(endpoint("/subscriptions"));
        request.setPost(true);
        request.setContentType("application/json");
        request.addRequestHeader("X-CN1-Push-App", appId);
        request.setRequestBody(JSONParser.mapToJson(body));
        NetworkManager.getInstance().addToQueue(request);
    }

    private void unregisterManaged() {
        String id = Preferences.get("push_v3_subscription", null);
        if (id == null) return;
        ConnectionRequest request = new ConnectionRequest();
        request.setUrl(endpoint("/subscriptions/") + id);
        request.setHttpMethod("DELETE");
        request.addRequestHeader("X-CN1-Push-App", appId);
        request.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(request);
        Preferences.delete("push_v3_subscription");
    }

    private static String endpoint(String suffix) {
        return Display.getInstance().getProperty("push.v3.serverUrl",
                "https://cloud.codenameone.com/api/v3/push/client") + suffix;
    }

    private static String installationId() {
        String value = Preferences.get("push_v3_installation", null);
        if (value == null) {
            value = Long.toHexString(System.currentTimeMillis()) + Long.toHexString(Double.doubleToLongBits(Math.random()));
            Preferences.set("push_v3_installation", value);
        }
        return value;
    }

    private void fireError(final PushError error) {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                public void run() { fireError(error); }
            });
            return;
        }
        if (listener != null) listener.onError(error);
    }

    private final class CompatibilityCallback implements PushCallback {
        public void push(String value) { receive(value); }
        public void registeredForPush(String deviceId) {
            String transportId = transportId(deviceId);
            registered(new PushSubscription(transportId, nativeToken(deviceId),
                    Display.getInstance().getPlatformName(),
                    installationId(), 0, Collections.<String>emptyList()));
        }
        public void pushRegistrationError(String error, int errorCode) {
            fireError(new PushError("registration_" + errorCode, error, errorCode == 1));
        }
    }

    private static String transportId(String deviceId) {
        if (deviceId != null && deviceId.startsWith("cn1-")) {
            int end = deviceId.indexOf('-', 4);
            String value = deviceId.substring(4, end < 0 ? deviceId.length() : end);
            return "hms".equals(value) ? "huawei" : value;
        }
        String platform = Display.getInstance().getPlatformName();
        if ("ios".equals(platform)) return "apns";
        if ("win".equals(platform)) return "wns";
        if ("and".equals(platform)) return "fcm";
        if ("js".equals(platform)) return "web";
        return "native";
    }

    private static String nativeToken(String deviceId) {
        if (deviceId != null && deviceId.startsWith("cn1-web-")) {
            try {
                return new String(com.codename1.util.Base64.decodeUrlSafe(deviceId.substring(9)),
                        "UTF-8");
            } catch (Exception error) {
                return deviceId;
            }
        }
        if (deviceId != null && deviceId.startsWith("cn1-")) {
            int separator = deviceId.indexOf('-', 4);
            if (separator >= 0 && separator + 1 < deviceId.length()) {
                return deviceId.substring(separator + 1);
            }
        }
        return deviceId;
    }

    private final class TransportCallback implements PushTransport.Callback {
        public void registered(PushSubscription value) { PushClient.this.registered(value); }
        public void unregistered() { notifyUnregistered(); }
        public void message(String envelopeJson) { receive(envelopeJson); }
        public void failed(PushError error) { fireError(error); }
    }

    public static final class Builder {
        private final String appId;
        private PushListener listener;
        private PushRegistrationSink registrationSink;
        private PushTransport transport;

        private Builder(String appId) {
            if (appId == null || appId.length() == 0) throw new IllegalArgumentException("appId is required");
            this.appId = appId;
        }
        public Builder listener(PushListener value) { listener = value; return this; }
        public Builder registrationSink(PushRegistrationSink value) { registrationSink = value; return this; }
        public Builder transport(PushTransport value) { transport = value; return this; }
        public PushClient build() {
            if (listener == null) throw new IllegalStateException("listener is required");
            if (transport != null && registrationSink == null) {
                throw new IllegalStateException("custom transports require a registrationSink");
            }
            return new PushClient(this);
        }
    }
}
