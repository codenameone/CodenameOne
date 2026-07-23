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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.surfaces.Surfaces;
import com.codename1.surfaces.spi.SurfaceBridge;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PushClientTransportTest extends UITestBase {
    @FormTest
    void customTransportExercisesTheNativeBoundaryWithoutBuildCloud() {
        FakeTransport transport = new FakeTransport();
        RecordingListener listener = new RecordingListener();
        RecordingSink sink = new RecordingSink();
        PushClient client = PushClient.builder("custom-app")
                .listener(listener).registrationSink(sink).transport(transport).build();

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
    void coldStartReplayAppliesSurfaceCommandsAtTheNativeSeam() {
        RecordingBridge bridge = new RecordingBridge();
        Surfaces.setBridge(bridge);
        try {
            FakeTransport transport = new FakeTransport();
            transport.pending = "{\"schema\":3,\"id\":\"cold-1\",\"silent\":true,"
                    + "\"surface\":{\"operation\":\"widget\",\"kind\":\"orders\","
                    + "\"timeline\":\"{\\\"revision\\\":7}\"}}";
            RecordingListener listener = new RecordingListener();
            PushClient.builder("custom-app").listener(listener)
                    .registrationSink(new RecordingSink()).transport(transport).build().register();
            assertEquals("cold-1", listener.message.getId());
            assertEquals("orders", bridge.widgetKind);
            assertEquals("{\"revision\":7}", bridge.widgetTimeline);
        } finally {
            Surfaces.setBridge(null);
        }
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
