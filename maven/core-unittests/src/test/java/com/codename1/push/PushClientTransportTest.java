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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.Preferences;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.spi.SurfaceBridge;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PushClientTransportTest extends UITestBase {
    private PushClient activeClient;

    @BeforeEach
    void clearManagedRegistrationState() {
        Preferences.delete("push_v3_subscription");
        implementation.clearQueuedRequests();
    }

    @AfterEach
    void restoreManagedRegistrationState() {
        if (activeClient != null && PushClient.getActiveCallback() != null) {
            activeClient.unregister();
        }
        activeClient = null;
        CodenameOneImplementation.setPushCallback(null);
        Preferences.delete("push_v3_subscription");
        implementation.clearQueuedRequests();
    }

    @FormTest
    void customTransportExercisesTheNativeBoundaryWithoutBuildCloud() {
        FakeTransport transport = new FakeTransport();
        RecordingListener listener = new RecordingListener();
        RecordingSink sink = new RecordingSink();
        PushClient client = track(PushClient.builder("custom-app")
                .listener(listener).registrationSink(sink).transport(transport).build());

        client.register();
        assertNotNull(client.getSubscription());
        assertSame(client.getSubscription(), sink.registered);
        assertSame(client.getSubscription(), listener.registration);
        assertNotNull(PushClient.getActiveCallback());

        PushClient.dispatch("{\"schema\":3,\"id\":\"background-1\",\"silent\":true,"
                + "\"data\":{\"state\":\"background\"}}");
        assertNotNull(listener.message);
        assertEquals("background-1", listener.message.getId());
        assertEquals("background", listener.message.getData().get("state"));

        client.unregister();
        activeClient = null;
        assertSame(sink.registered, sink.unregistered);
        assertNull(client.getSubscription());
        assertNull(PushClient.getActiveCallback());
    }

    @Test
    void customTransportRequiresAnApplicationOwnedRegistrationSink() {
        assertThrows(IllegalStateException.class, () -> PushClient.builder("custom-app")
                .listener(new RecordingListener()).transport(new FakeTransport()).build());
    }

    @FormTest
    void managedUnregisterRetainsPersistedSubscriptionUntilServerConfirmation() {
        Preferences.set("push_v3_subscription", "persisted-subscription");
        PushClient client = PushClient.builder("managed-app")
                .listener(new RecordingListener()).build();

        client.unregister();

        List<ConnectionRequest> requests = implementation.getQueuedRequests();
        assertEquals(1, requests.size());
        assertEquals("DELETE", requests.get(0).getHttpMethod());
        assertTrue(requests.get(0).getUrl().endsWith(
                "/subscriptions/persisted-subscription"));
        assertEquals("persisted-subscription",
                Preferences.get("push_v3_subscription", null));
    }

    @FormTest
    void coldStartReplayAppliesSurfaceCommandsAtTheNativeSeam() {
        RecordingBridge bridge = new RecordingBridge();
        Surfaces.setBridge(bridge);
        try {
            FakeTransport transport = new FakeTransport();
            transport.pending = "{\"schema\":3,\"id\":\"cold-1\",\"silent\":true,"
                    + "\"surface\":{\"operation\":\"widget\",\"kind\":\"orders\","
                    + "\"timeline\":\"{\\\"revision\\\":7}\"}}";
            RecordingListener listener = new RecordingListener();
            PushClient client = track(PushClient.builder("custom-app").listener(listener)
                    .registrationSink(new RecordingSink()).transport(transport).build());
            client.register();
            assertEquals("cold-1", listener.message.getId());
            assertEquals("orders", bridge.widgetKind);
            assertEquals("{\"revision\":7}", bridge.widgetTimeline);
        } finally {
            Surfaces.setBridge(null);
        }
    }

    @FormTest
    void webDeviceIdRoundTripsWithoutDroppingTheFirstBase64Character() throws Exception {
        RecordingListener listener = new RecordingListener();
        PushClient client = track(PushClient.builder("custom-app").listener(listener)
                .registrationSink(new RecordingSink()).transport(new FakeTransport()).build());
        client.register();
        String nativeToken = "{\"endpoint\":\"https://push.example/subscription\","
                + "\"keys\":{\"p256dh\":\"abc\",\"auth\":\"def\"}}";

        PushClient.getActiveCallback().registeredForPush("cn1-web-"
                + com.codename1.util.Base64.encodeUrlSafe(nativeToken.getBytes("UTF-8")));

        assertEquals(nativeToken, listener.registration.getToken());
        assertEquals("web", listener.registration.getTransportId());
    }

    @FormTest
    void pushReceivedBeforeRegistrationIsReplayedWhenClientActivates() {
        PushClient.dispatch("{\"schema\":3,\"id\":\"startup-race\",\"silent\":true}");
        RecordingListener listener = new RecordingListener();
        PushClient client = track(PushClient.builder("custom-app").listener(listener)
                .registrationSink(new RecordingSink()).transport(new FakeTransport()).build());

        client.register();

        assertNotNull(listener.message);
        assertEquals("startup-race", listener.message.getId());
    }

    @FormTest
    void unsupportedTransportDoesNotReplaceTheGlobalPushBinding() {
        RecordingListener listener = new RecordingListener();
        PushClient client = PushClient.builder("custom-app").listener(listener)
                .registrationSink(new RecordingSink()).transport(new UnsupportedTransport()).build();

        client.register();

        assertNull(PushClient.getActiveCallback());
        assertNotNull(listener.error);
        assertEquals("unsupported_transport", listener.error.getCode());
    }

    private PushClient track(PushClient client) {
        activeClient = client;
        return client;
    }

    private static final class FakeTransport implements PushTransport {
        String pending;
        public String getId() { return "fake-native"; }
        public boolean isSupported() { return true; }
        public void register(Callback value) {
            value.registered(new PushSubscription(getId(), "opaque-token", "test",
                    "installation-1", 0, Collections.singletonList("silent")));
            if (pending != null) PushClient.dispatch(pending);
        }
        public void unregister(Callback value) { value.unregistered(); }
    }

    private static final class UnsupportedTransport implements PushTransport {
        public String getId() { return "unsupported"; }
        public boolean isSupported() { return false; }
        public void register(Callback value) { fail("unsupported transport must not register"); }
        public void unregister(Callback value) {}
    }

    private static final class RecordingBridge implements SurfaceBridge {
        String widgetKind;
        String widgetTimeline;
        public boolean areWidgetsSupported() { return true; }
        public boolean isLiveActivitySupported() { return true; }
        public void registerWidgetKind(String value) {}
        public void publishWidgetTimeline(String kind, String timeline, Map<String, byte[]> images) {
            widgetKind = kind;
            widgetTimeline = timeline;
        }
        public void reloadWidgets(String kind) {}
        public int getInstalledWidgetCount(String kind) { return 1; }
        public String startLiveActivity(String descriptor, Map<String, byte[]> images) { return "live-1"; }
        public void updateLiveActivity(String id, String state) {}
        public void endLiveActivity(String id, String state, boolean immediately) {}
    }

    private static final class RecordingListener implements PushListener {
        PushSubscription registration;
        PushMessage message;
        PushError error;
        public void onRegistration(PushSubscription value) { registration = value; }
        public void onMessage(PushMessage value) { message = value; }
        public void onError(PushError value) { error = value; }
    }

    private static final class RecordingSink implements PushRegistrationSink {
        PushSubscription registered;
        PushSubscription unregistered;
        public void registered(PushSubscription value) { registered = value; }
        public void unregistered(PushSubscription value) { unregistered = value; }
    }
}
