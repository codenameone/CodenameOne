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
import com.codename1.io.Util;
import com.codename1.ui.Display;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.LiveActivity;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

///
/// Owns push registration and delivers typed push events to an application.
///
/// <p>Create exactly one client from the application's {@code init()} method,
/// keep it in a field on the main application class, and call {@link #register()}
/// from {@code start()}. Registration is idempotent, so it is safe for
/// {@code start()} to call it again when Codename One resumes the application.
/// Do not call {@link #unregister()} from {@code stop()}; unregistering removes
/// the device subscription and is intended for an explicit user opt-out.</p>
///
/// <p>The listener is mandatory and is installed before native registration
/// starts. Messages that reach the runtime before {@code register()} are kept in
/// a bounded process-local queue and replayed when the client becomes active.
/// Native cold-start implementations also persist messages until the Codename
/// One runtime starts. All listener and registration-sink callbacks run on the
/// Codename One EDT.</p>
///
/// <p>Codename One does not discover a listener with reflection or
/// {@code Class.forName()}. Calling {@code build()} without a listener fails
/// immediately, but the application is responsible for retaining the client and
/// calling {@code register()}. Only one client can be active in a process.</p>
///
/// @see PushListener
/// @see PushTransport
/// @see PushRegistrationSink
public final class PushClient {
    private static final int MAX_PENDING_MESSAGES = 100;
    private static final List<String> pendingMessages = new ArrayList<String>();
    private static final AtomicReference<PushClient> active =
            new AtomicReference<PushClient>();

    private final String appId;
    private final PushListener listener;
    private final PushRegistrationSink registrationSink;
    private final PushTransport transport;
    private PushSubscription subscription;
    private boolean registrationRequested;
    private final PushCallback compatibilityCallback = new CompatibilityCallback();

    private PushClient(Builder builder) {
        appId = builder.appId;
        listener = builder.listener;
        registrationSink = builder.registrationSink;
        transport = builder.transport;
    }

    ///
    /// Starts a client builder for a managed BuildCloud push application.
    ///
    /// <p>The application key is displayed in the Push section of the Codename
    /// One Console. It identifies the application during client registration; it
    /// is not a server API key. Install a {@link PushTransport} on the returned
    /// builder to bypass BuildCloud and use an application-owned push server.</p>
    ///
    /// @param appId the non-empty Push application key, or an application-owned
    ///              identifier when using a custom transport
    /// @return a new builder
    /// @throws IllegalArgumentException if {@code appId} is null or empty
    public static Builder builder(String appId) {
        return new Builder(appId);
    }

    ///
    /// Activates this client and requests native push registration.
    ///
    /// <p>This method is idempotent. Calling it from each invocation of the
    /// application's {@code start()} method requests registration only once.
    /// A native token persisted before this client becomes active is replayed
    /// immediately, then the platform is asked to refresh it. Registration
    /// otherwise completes asynchronously through
    /// {@link PushListener#onRegistration(PushSubscription)} or
    /// {@link PushListener#onError(PushError)}. Messages queued before activation
    /// are replayed before this method requests a new native token.</p>
    ///
    /// <p>If another {@code PushClient} is already active, this client reports an
    /// {@code active_client} error and remains inactive.</p>
    public void register() {
        synchronized (this) {
            if (registrationRequested) {
                return;
            }
            registrationRequested = true;
        }
        if (transport != null && !transport.isSupported()) {
            registrationFailed();
            fireError(new PushError("unsupported_transport",
                    transport.getId() + " is unavailable", false));
            return;
        }
        List<String> replay;
        synchronized (pendingMessages) {
            PushClient current = active.get();
            if (current != null && !current.equals(this)) {
                registrationFailed();
                fireError(new PushError("active_client",
                        "Another PushClient is already active", false));
                return;
            }
            if (current == null && !active.compareAndSet(null, this)) {
                registrationFailed();
                fireError(new PushError("active_client",
                        "Another PushClient became active during registration", false));
                return;
            }
            CodenameOneImplementation.setPushCallback(compatibilityCallback);
            replay = new ArrayList<String>(pendingMessages);
            pendingMessages.clear();
        }
        for (String message : replay) {
            receive(message);
        }
        if (transport == null) {
            String persistedDeviceId = Preferences.get("push_key", null);
            if (persistedDeviceId != null && persistedDeviceId.trim().length() > 0) {
                compatibilityCallback.registeredForPush(persistedDeviceId);
            }
            Display.getInstance().registerPush();
        } else {
            transport.register(new TransportCallback());
        }
    }

    ///
    /// Removes this device's subscription.
    ///
    /// <p>Use this for an explicit notification opt-out or account-removal
    /// workflow, not as part of the normal {@code stop()} lifecycle. A later call
    /// to {@link #register()} may subscribe again after unregistration completes.
    /// Custom transports report completion through
    /// {@link PushTransport.Callback#unregistered()}.</p>
    public void unregister() {
        if (transport == null) {
            Display.getInstance().deregisterPush();
            notifyUnregistered();
        } else {
            transport.unregister(new TransportCallback());
        }
    }

    ///
    /// Returns the application key supplied to {@link #builder(String)}.
    ///
    /// @return the application key
    public String getAppId() {
        return appId;
    }

    ///
    /// Returns the latest native subscription reported by the transport.
    ///
    /// @return the current subscription, or {@code null} before registration or
    ///         after unregistration
    public PushSubscription getSubscription() {
        return subscription;
    }

    ///
    /// Returns the compatibility callback used by generated native bootstraps.
    ///
    /// <p>Application code should use {@link PushListener}; this method exists
    /// for generated platform code and native transport integrations.</p>
    ///
    /// @return the active native callback, or {@code null} before a client is
    ///         registered
    public static PushCallback getActiveCallback() {
        PushClient client = active.get();
        return client == null ? null : client.compatibilityCallback;
    }

    ///
    /// Indicates whether an active client can receive a native message now.
    ///
    /// @return {@code true} after {@link #register()} activates a client and
    ///         before unregistration completes
    public static boolean hasActiveClient() {
        return active.get() != null;
    }

    ///
    /// Delivers an encoded schema-3 envelope from generated native code.
    ///
    /// <p>If no client is active yet, the message is queued and replayed on the
    /// next successful {@link #register()}. Application code normally does not
    /// call this method; a custom {@link PushTransport} should use its callback's
    /// {@link PushTransport.Callback#message(String)} method.</p>
    ///
    /// @param envelopeJson the complete schema-3 JSON envelope
    public static void dispatch(String envelopeJson) {
        PushClient client = active.get();
        if (client == null) {
            synchronized (pendingMessages) {
                client = active.get();
                if (client == null) {
                    if (pendingMessages.size() == MAX_PENDING_MESSAGES) {
                        pendingMessages.remove(0);
                    }
                    pendingMessages.add(envelopeJson);
                    return;
                }
            }
        }
        client.receive(envelopeJson);
    }

    private void registered(final PushSubscription value) {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    registered(value);
                }
            });
            return;
        }
        subscription = value;
        if (registrationSink != null) {
            registrationSink.registered(value);
        }
        if (transport == null) {
            registerManaged(value);
        }
        if (listener != null) {
            listener.onRegistration(value);
        }
    }

    private void receive(final String json) {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    receive(json);
                }
            });
            return;
        }
        try {
            PushMessage message = PushMessage.parse(json);
            applySurface(message);
            if (listener != null) {
                listener.onMessage(message);
            }
        } catch (IOException ex) {
            Log.e(ex);
            fireError(new PushError("invalid_envelope", ex.getMessage(), false));
        }
    }

    private static void applySurface(PushMessage message) {
        Map<String, Object> surface = message.getSurface();
        if (surface.isEmpty()) {
            return;
        }
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

    private static String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void notifyUnregistered() {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    notifyUnregistered();
                }
            });
            return;
        }
        if (subscription != null && registrationSink != null) {
            registrationSink.unregistered(subscription);
        }
        if (transport == null) {
            unregisterManaged();
        }
        subscription = null;
        synchronized (this) {
            registrationRequested = false;
        }
        if (active.compareAndSet(this, null)) {
            CodenameOneImplementation.setPushCallback(null);
        }
    }

    private synchronized void registrationFailed() {
        registrationRequested = false;
    }

    private void registerManaged(PushSubscription value) {
        final Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("provider", value.getTransportId());
        body.put("token", value.getToken());
        String subscriptionInstallationId = value.getInstallationId();
        body.put("installationId", subscriptionInstallationId == null
                || subscriptionInstallationId.length() == 0
                ? installationId() : subscriptionInstallationId);
        ConnectionRequest request = new ConnectionRequest() {
            @Override
            protected void postResponse() {
                try {
                    Preferences.set("push_v3_subscription",
                            managedRegistrationId(getResponseData()));
                } catch (IOException ex) {
                    fireError(new PushError("registration_response", ex.getMessage(), false));
                }
            }

            @Override
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

    static String managedRegistrationId(byte[] responseData) throws IOException {
        if (responseData == null || responseData.length == 0) {
            throw new IOException("BuildCloud registration returned an empty response");
        }
        Map<String, Object> response = JSONParser.parseJSON(responseData);
        if (response == null) {
            throw new IOException("BuildCloud registration returned an empty response");
        }
        Object id = response.get("id");
        if (id == null || String.valueOf(id).trim().length() == 0) {
            throw new IOException("BuildCloud registration response is missing an id");
        }
        return String.valueOf(id);
    }

    private void unregisterManaged() {
        String id = Preferences.get("push_v3_subscription", null);
        if (id == null) {
            return;
        }
        NetworkManager.getInstance().addToQueue(new ManagedUnregisterRequest(appId, id));
    }

    private static String endpoint(String suffix) {
        return Display.getInstance().getProperty("push.v3.serverUrl",
                "https://cloud.codenameone.com/api/v3/push/client") + suffix;
    }

    private static String installationId() {
        String value = Preferences.get("push_v3_installation", null);
        if (value == null) {
            value = Util.getUUID();
            Preferences.set("push_v3_installation", value);
        }
        return value;
    }

    private void fireError(final PushError error) {
        if (!Display.getInstance().isEdt()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    fireError(error);
                }
            });
            return;
        }
        if (listener != null) {
            listener.onError(error);
        }
    }

    private static final class ManagedUnregisterRequest extends ConnectionRequest {
        ManagedUnregisterRequest(String appId, String subscriptionId) {
            setUrl(endpoint("/subscriptions/") + subscriptionId);
            setHttpMethod("DELETE");
            addRequestHeader("X-CN1-Push-App", appId);
            setFailSilently(true);
        }

        private void removePersistedId() {
            String persistedId = Preferences.get("push_v3_subscription", null);
            if (persistedId != null
                    && getUrl().endsWith("/subscriptions/" + persistedId)) {
                Preferences.delete("push_v3_subscription");
            }
        }

        @Override
        protected void postResponse() {
            removePersistedId();
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            if (code == 404) {
                removePersistedId();
            }
        }
    }

    private final class CompatibilityCallback implements PushCallback {
        @Override
        public void push(String value) {
            receive(value);
        }

        @Override
        public void registeredForPush(String deviceId) {
            String transportId = transportId(deviceId);
            registered(new PushSubscription(transportId, nativeToken(deviceId),
                    Display.getInstance().getPlatformName(),
                    installationId(), 0, Collections.<String>emptyList()));
        }

        @Override
        public void pushRegistrationError(String error, int errorCode) {
            registrationFailed();
            String code = errorCode == 1 ? "registration_server" : "registration_native";
            fireError(new PushError(code, error, errorCode == 1));
        }
    }

    private static String transportId(String deviceId) {
        if (deviceId != null && deviceId.startsWith("cn1-")) {
            int end = deviceId.indexOf('-', 4);
            String value = deviceId.substring(4, end < 0 ? deviceId.length() : end);
            return "hms".equals(value) ? "huawei" : value;
        }
        String platform = Display.getInstance().getPlatformName();
        if ("ios".equals(platform)) {
            return "apns";
        }
        if ("win".equals(platform)) {
            return "wns";
        }
        if ("and".equals(platform)) {
            return "fcm";
        }
        if ("js".equals(platform)) {
            return "web";
        }
        return "native";
    }

    private static String nativeToken(String deviceId) {
        if (deviceId != null && deviceId.startsWith("cn1-web-")) {
            try {
                return new String(com.codename1.util.Base64.decodeUrlSafe(deviceId.substring(8)),
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
        @Override
        public void registered(PushSubscription value) {
            PushClient.this.registered(value);
        }

        @Override
        public void unregistered() {
            notifyUnregistered();
        }

        @Override
        public void message(String envelopeJson) {
            receive(envelopeJson);
        }

        @Override
        public void failed(PushError error) {
            registrationFailed();
            fireError(error);
        }
    }

    ///
    /// Configures a {@link PushClient}.
    ///
    /// <p>A {@link PushListener} is required. Without a custom
    /// {@link PushTransport}, the client uses the platform transport and
    /// BuildCloud registration. Supplying a custom transport bypasses BuildCloud
    /// and also requires a {@link PushRegistrationSink} so the application can
    /// maintain its own server-side subscription.</p>
    public static final class Builder {
        private final String appId;
        private PushListener listener;
        private PushRegistrationSink registrationSink;
        private PushTransport transport;

        private Builder(String appId) {
            if (appId == null || appId.length() == 0) {
                throw new IllegalArgumentException("appId is required");
            }
            this.appId = appId;
        }
        ///
        /// Sets the application listener.
        ///
        /// @param value the non-null listener retained for the life of the client
        /// @return this builder
        public Builder listener(PushListener value) {
            listener = value;
            return this;
        }

        ///
        /// Mirrors subscription changes to application-owned code.
        ///
        /// <p>For managed push this is optional and runs in addition to
        /// BuildCloud registration. For a custom transport it is required.</p>
        ///
        /// @param value the registration sink, or {@code null} for managed push
        /// @return this builder
        public Builder registrationSink(PushRegistrationSink value) {
            registrationSink = value;
            return this;
        }

        ///
        /// Replaces the managed native transport.
        ///
        /// <p>Setting this option prevents {@code PushClient} from contacting
        /// BuildCloud. The transport must emit schema-3 envelopes and the builder
        /// must also receive a {@link #registrationSink(PushRegistrationSink)}.</p>
        ///
        /// @param value a custom native transport, or {@code null} to use managed
        ///              platform push
        /// @return this builder
        public Builder transport(PushTransport value) {
            transport = value;
            return this;
        }
        ///
        /// Creates the client.
        ///
        /// @return a client that must be retained and registered by the
        ///         application
        /// @throws IllegalStateException if no listener is configured, or if a
        ///         custom transport has no registration sink
        public PushClient build() {
            if (listener == null) {
                throw new IllegalStateException("listener is required");
            }
            if (transport != null && registrationSink == null) {
                throw new IllegalStateException("custom transports require a registrationSink");
            }
            return new PushClient(this);
        }
    }
}
